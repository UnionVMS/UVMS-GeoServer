/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Objects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Throwables.*;
import static com.google.common.collect.Maps.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.gwc.GWC;
import org.geotools.util.logging.Logging;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * A GWC's {@link Configuration} implementation that provides {@link TileLayer}s directly from the
 * GeoServer {@link Catalog}'s {@link LayerInfo}s and {@link LayerGroupInfo}s.
 * <p>
 * The sole responsibility of the class is to provide the {@link GeoServerTileLayer}s out of the
 * geoserver catalog for {@link TileLayerDispatcher}
 * </p>
 * 
 * @see #createLayer(LayerInfo)
 * @see #createLayer(LayerGroupInfo)
 * @see #getTileLayers(boolean)
 * @see CatalogStyleChangeListener
 */
public class CatalogConfiguration implements Configuration {
    
    /**
     * The configuration lock timeout, in seconds
     */
    static final int GWC_CONFIGURATION_LOCK_TIMEOUT = Integer.getInteger("gwc.configuration.lock.timeout", 60);

    /**
     * {@link GeoServerTileLayer} cache loader
     * 
     */
    private final class TileLayerLoader extends CacheLoader<String, GeoServerTileLayer> {
        private final TileLayerCatalog tileLayerCatalog;

        private TileLayerLoader(TileLayerCatalog tileLayerCatalog) {
            this.tileLayerCatalog = tileLayerCatalog;
        }

        @Override
        public GeoServerTileLayer load(String layerId) throws Exception {
            GeoServerTileLayer tileLayer = null;
            final GridSetBroker gridSetBroker = CatalogConfiguration.this.gridSetBroker;

            lock.acquireReadLock();
            try {
                if (pendingDeletes.contains(layerId)) {
                    throw new IllegalArgumentException("Tile layer '" + layerId + "' was deleted.");
                }
                GeoServerTileLayerInfo tileLayerInfo = pendingModications.get(layerId);
                if (tileLayerInfo == null) {
                    tileLayerInfo = tileLayerCatalog.getLayerById(layerId);
                }
                if (tileLayerInfo == null) {
                    throw new IllegalArgumentException("GeoServerTileLayerInfo '" + layerId
                            + "' does not exist.");
                }

                tileLayer = new GeoServerTileLayer(geoServerCatalog, layerId, gridSetBroker,
                        tileLayerInfo);
            } finally {
                lock.releaseReadLock();
            }
            if (null == tileLayer) {
                throw new IllegalArgumentException("GeoServer layer or layer group '" + layerId
                        + "' does not exist");
            }
            return tileLayer;
        }

    }

    private static final Logger LOGGER = Logging.getLogger(CatalogConfiguration.class);

    private TileLayerCatalog tileLayerCatalog;

    private Catalog geoServerCatalog;

    private GridSetBroker gridSetBroker;

    /**
     * Maps pending modifications by {@link GeoServerTileLayerInfo#getId()}
     */
    private final Map<String, GeoServerTileLayerInfo> pendingModications = newConcurrentMap();

    private final LoadingCache<String, GeoServerTileLayer> layerCache;

    /**
     * Ids of pending deletes
     */
    private final Set<String> pendingDeletes = new CopyOnWriteArraySet<String>();

    private final TimeoutReadWriteLock lock = new TimeoutReadWriteLock(GWC_CONFIGURATION_LOCK_TIMEOUT * 1000);

    public CatalogConfiguration(final Catalog catalog, final TileLayerCatalog tileLayerCatalog,
            final GridSetBroker gridSetBroker) {
        checkNotNull(catalog);
        checkNotNull(tileLayerCatalog);
        checkNotNull(gridSetBroker);
        this.tileLayerCatalog = tileLayerCatalog;
        this.geoServerCatalog = catalog;
        this.gridSetBroker = gridSetBroker;

        this.layerCache = CacheBuilder.newBuilder()//
                .concurrencyLevel(10)//
                .expireAfterAccess(10, TimeUnit.MINUTES)//
                .initialCapacity(10)//
                .maximumSize(100)//
                .build(new TileLayerLoader(tileLayerCatalog));
    }
    
    /**
     * 
     * @see org.geowebcache.config.Configuration#getIdentifier()
     */
    @Override
    public String getIdentifier() {
        return "GeoServer Catalog Configuration";
    }

    /**
     * @see org.geowebcache.config.Configuration#getServiceInformation()
     * @return {@code null}
     */
    @Override
    public ServiceInformation getServiceInformation() {
        return null;
    }

    /**
     * @return {@code true}
     * @see org.geowebcache.config.Configuration#isRuntimeStatsEnabled()
     */
    @Override
    public boolean isRuntimeStatsEnabled() {
        return true;
    }

    /**
     * Returns the list of {@link GeoServerTileLayer} objects matching the GeoServer ones.
     * <p>
     * The list is built dynamically on each call.
     * </p>
     * 
     * @see org.geowebcache.config.Configuration#getTileLayers(boolean)
     * @see org.geowebcache.config.Configuration#getTileLayers()
     * @deprecated
     */
    @Override
    public List<GeoServerTileLayer> getTileLayers() {
        Iterable<GeoServerTileLayer> layers = getLayers();
        return Lists.newArrayList(layers);
    }

    /**
     * @see org.geowebcache.config.Configuration#getLayers()
     */
    @Override
    public Iterable<GeoServerTileLayer> getLayers() {
        lock.acquireReadLock();
        try {
            final Set<String> layerIds = tileLayerCatalog.getLayerIds();

            Function<String, GeoServerTileLayer> lazyLayerFetch = new Function<String, GeoServerTileLayer>() {
                @Override
                public GeoServerTileLayer apply(final String layerId) {
                    return CatalogConfiguration.this.getTileLayerById(layerId);
                }
            };

            return Iterables.transform(layerIds, lazyLayerFetch);
        } finally {
            lock.releaseReadLock();
        }
    }

    /**
     * Returns a dynamic list of cached layer names out of the GeoServer {@link Catalog}
     * 
     * @see org.geowebcache.config.Configuration#getTileLayerNames()
     */
    @Override
    public Set<String> getTileLayerNames() {
        lock.acquireReadLock();
        try {
            final Set<String> storedNames = tileLayerCatalog.getLayerNames();
            Set<String> names = null;
            if (!pendingDeletes.isEmpty()) {
                names = new HashSet<String>(storedNames);
                for (String id : pendingDeletes) {
                    GeoServerTileLayerInfo old = tileLayerCatalog.getLayerById(id);
                    names.remove(old.getName());
                }
            }
            if (!pendingModications.isEmpty()) {
                for (Map.Entry<String, GeoServerTileLayerInfo> e : pendingModications.entrySet()) {
                    GeoServerTileLayerInfo old = tileLayerCatalog.getLayerById(e.getKey());
                    if (old != null) {
                        // it's a modification, not an addition. Make sure the name is not outdated
                        String oldName = old.getName();
                        String newName = e.getValue().getName();
                        if (!Objects.equal(oldName, newName)) {
                            if (names == null) {
                                names = new HashSet<String>(storedNames);
                            }
                            names.remove(oldName);
                            names.add(newName);
                        }
                    }
                }
            }
            return names == null ? storedNames : Collections.unmodifiableSet(names);
        } finally {
            lock.releaseReadLock();
        }
    }

    @Override
    public boolean containsLayer(String layerId) {
        checkNotNull(layerId, "layer id is null");
        lock.acquireReadLock();
        try {
            if (pendingDeletes.contains(layerId)) {
                return false;
            }
            Set<String> layerIds = tileLayerCatalog.getLayerIds();
            boolean hasLayer = layerIds.contains(layerId);
            return hasLayer;
        } finally {
            lock.releaseReadLock();
        }
    }

    @Override
    public GeoServerTileLayer getTileLayerById(final String layerId) {
        checkNotNull(layerId, "layer id is null");

        GeoServerTileLayer layer;
        lock.acquireReadLock();
        try {
            layer = layerCache.get(layerId);
        } catch (ExecutionException e) {
            throw propagate(e.getCause());
        } catch (UncheckedExecutionException e) {
            throw propagate(e.getCause());
        } finally {
            lock.releaseReadLock();
        }

        return layer;
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayer(java.lang.String)
     */
    @Override
    public GeoServerTileLayer getTileLayer(final String layerName) {
        checkNotNull(layerName, "layer name is null");

        final String layerId;

        lock.acquireReadLock();
        try {
            layerId = getLayerId(layerName);
            if (layerId == null) {
                return null;
            }
        } finally {
            lock.releaseReadLock();
        }
        return getTileLayerById(layerId);
    }

    private String getLayerId(final String layerName) {

        String storedName = layerName;
        // check pending modifs first in case name changed
        if (!pendingModications.isEmpty()) {
            for (GeoServerTileLayerInfo info : pendingModications.values()) {
                String name = info.getName();
                if (name.equals(layerName)) {
                    storedName = info.getName();
                    break;
                }
            }
        }

        final String layerId = tileLayerCatalog.getLayerId(storedName);
        if (layerId == null || pendingDeletes.contains(layerId)) {
            return null;
        }
        // name changed?
        GeoServerTileLayerInfo modifiedState = pendingModications.get(layerId);
        if (modifiedState != null && !layerName.equals(modifiedState.getName())) {
            return null;
        }
        return layerId;
    }

    private GeoServerTileLayerInfo getTileLayerInfoByName(final String layerName) {
        GeoServerTileLayerInfo tileLayerInfo = null;

        // check pending modifs first in case name changed
        if (!pendingModications.isEmpty()) {
            for (GeoServerTileLayerInfo info : pendingModications.values()) {
                String name = info.getName();
                if (name.equals(layerName)) {
                    tileLayerInfo = info;
                    break;
                }
            }
        }

        if (null == tileLayerInfo) {
            tileLayerInfo = tileLayerCatalog.getLayerByName(layerName);
            if (null == tileLayerInfo) {
                return null;
            }
            if (pendingDeletes.contains(tileLayerInfo.getId())) {
                return null;
            }
            if (pendingModications.containsKey(tileLayerInfo.getId())) {
                // found in catalog but not in pending modifications, means name changed
                return null;
            }
        }
        return tileLayerInfo;
    }

    /**
     * @see org.geowebcache.config.Configuration#getTileLayerCount()
     */
    @Override
    public int getTileLayerCount() {
        int count = 0;
        lock.acquireReadLock();
        try {
            Set<String> layerIds = tileLayerCatalog.getLayerIds();
            if (pendingDeletes.isEmpty()) {
                count = layerIds.size();
            } else {
                for (String layerId : layerIds) {
                    if (pendingDeletes.contains(layerId)) {
                        continue;
                    }
                    ++count;
                }
            }
        } finally {
            lock.releaseReadLock();
        }
        return count;
    }

    /**
     * @see org.geowebcache.config.Configuration#initialize(org.geowebcache.grid.GridSetBroker)
     */
    @Override
    public int initialize(GridSetBroker gridSetBroker) {
        lock.acquireWriteLock();
        try {
            LOGGER.info("Initializing GWC configuration based on GeoServer's Catalog");
            this.gridSetBroker = gridSetBroker;
            this.layerCache.invalidateAll();
            this.tileLayerCatalog.initialize();

            // startup sanity check
            for (String layerId : tileLayerCatalog.getLayerIds()) {
                final String layerName = tileLayerCatalog.getLayerName(layerId);
                try {
                    getTileLayerById(layerId);
                } catch (Exception e) {
                    String msg = "GeoServer TileLayer named '" + layerName + "' with id '"
                            + layerId + "' can't be loaded. "
                            + "It will be removed from the configuration but you'll need"
                            + " to delete its cache manually (if any). Original error message: "
                            + e.getMessage();
                    LOGGER.log(Level.SEVERE, msg, e);
                    tileLayerCatalog.delete(layerId);
                }
            }
            LOGGER.info("GWC configuration based on GeoServer's Catalog loaded successfuly");
        } finally {
            lock.releaseWriteLock();
        }
        return getTileLayerCount();
    }

    /**
     * @return {@code true} only if {@code tl instanceof} {@link GeoServerTileLayer} .
     * @see org.geowebcache.config.Configuration#canSave(org.geowebcache.layer.TileLayer)
     */
    @Override
    public boolean canSave(TileLayer tl) {
        return tl instanceof GeoServerTileLayer && (!tl.isTransientLayer());
    }
    
    public static boolean isLayerExposable(LayerInfo layer) {
        assert layer!=null;
        // TODO: this was copied from WMS 1.1 GetCapabilitesTransformer.handleLayerTree and is
        // replicated again in the WMS 1.3 implementation.  Should be refactored to eliminate
        // duplication.
        
        // no sense in exposing a geometryless layer through wms...
        boolean wmsExposable = false;
        if (layer.getType() == PublishedType.RASTER || layer.getType() == PublishedType.WMS) {
            wmsExposable = true;
        } else {
            try {
                wmsExposable = layer.getType() == PublishedType.VECTOR
                        && ((FeatureTypeInfo) layer.getResource()).getFeatureType()
                                .getGeometryDescriptor() != null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "An error occurred trying to determine if"
                        + " the layer is geometryless", e);
            }
        }
        
        return wmsExposable;
    }
 
    @Override
    public synchronized void addLayer(final TileLayer tl) {
        checkNotNull(tl);
        checkArgument(canSave(tl), "Can't save TileLayer of type ", tl.getClass());
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;
        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "id is null");
        checkNotNull(tileLayer.getInfo().getName(), "name is null");

        GeoServerTileLayerInfo info = tileLayer.getInfo();
        
        LayerInfo layerInfo = tileLayer.getLayerInfo();
        if(layerInfo!=null && !isLayerExposable(layerInfo)) {
            LOGGER.warning("Requested layer " + layerInfo.getName() + " has no geometry. Won't create TileLayer");
            return;
        }

        lock.acquireWriteLock();
        try {
            boolean pending = pendingModications.containsKey(info.getId());
            boolean exists = null != tileLayerCatalog.getLayerById(info.getId());
            boolean notExists = !pending && !exists;

            checkArgument(notExists, "A GeoServerTileLayer named '" + info.getName()
                    + "' already exists");
            if (pendingDeletes.remove(info.getId())) {
                LOGGER.finer("Adding a new layer " + info.getName()
                        + " before saving the deleted one with the same name");
            }
            pendingModications.put(info.getId(), info);
        } finally {
            lock.releaseWriteLock();
        }
    }

    /**
     * @see org.geowebcache.config.Configuration#modifyLayer(org.geowebcache.layer.TileLayer)
     */
    @Override
    public synchronized void modifyLayer(TileLayer tl) throws NoSuchElementException {
        checkNotNull(tl, "TileLayer is null");
        checkArgument(canSave(tl), "Can't save TileLayer of type ", tl.getClass());

        GeoServerTileLayer tileLayer = (GeoServerTileLayer) tl;

        checkNotNull(tileLayer.getInfo(), "GeoServerTileLayerInfo is null");
        checkNotNull(tileLayer.getInfo().getId(), "id is null");
        checkNotNull(tileLayer.getInfo().getName(), "name is null");

        final GeoServerTileLayerInfo info = tileLayer.getInfo();
        lock.acquireWriteLock();
        try {
            final String layerId = info.getId();
            // check pendingModifications too to catch unsaved adds
            boolean exists = pendingModications.containsKey(layerId)
                    || tileLayerCatalog.exists(layerId);
            checkArgument(exists, "No GeoServerTileLayer named '" + info.getName() + "' exists");
            pendingModications.put(layerId, info);
            layerCache.invalidate(layerId);
        } finally {
            lock.releaseWriteLock();
        }
    }

    /**
     * {@link TileLayerDispatcher} is requesting to remove the layer named after {@code layerName}
     * 
     * @see org.geowebcache.config.Configuration#removeLayer(java.lang.String)
     * @return {@code true} if the layer was removed, false if it didn't exist
     */
    @Override
    public boolean removeLayer(final String layerName) {
        checkNotNull(layerName);
        lock.acquireWriteLock();
        try {
            GeoServerTileLayerInfo tileLayerInfo = getTileLayerInfoByName(layerName);
            if (tileLayerInfo != null) {
                final String layerId = tileLayerInfo.getId();
                pendingModications.remove(layerId);
                pendingDeletes.add(layerId);
                layerCache.invalidate(layerId);
                return true;
            } else {
                return false;
            }
        } finally {
            lock.releaseWriteLock();
        }
    }

    /**
     * @see GWC#layerAdded(String)
     * @see GWC#layerRemoved(String)
     * @see GWC#layerRenamed(String, String)
     * @see GWC#truncateByLayerAndStyle(String, String)
     * @see GWC#truncate(String, String, String, BoundingBox, String)
     * @see org.geowebcache.config.Configuration#save()
     */
    @Override
    public synchronized void save() {

        final GWC mediator = GWC.get();

        final Set<String/* name */> deletedNames = Sets.newHashSet();
        final List<GeoServerTileLayerInfo[/* old, new */]> modifications = Lists.newLinkedList();

        lock.acquireWriteLock();
        // perform the transaction while holding the write lock, then downgrade to the read lock and
        // issue the modification events (otherwise another thread asking for any changed layer
        // would lock)
        try {
            for (String deletedId : pendingDeletes) {
                try {
                    GeoServerTileLayerInfo info = tileLayerCatalog.delete(deletedId);
                    if (info != null) {
                        // remove it from stack copy to avoid notifying its deletion
                        deletedNames.add(info.getName());
                    }
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE, "Error deleting tile layer '" + deletedId + "'", e);
                }
            }

            for (GeoServerTileLayerInfo modified : pendingModications.values()) {
                final GeoServerTileLayerInfo old;
                try {
                    old = tileLayerCatalog.save(modified);
                    modifications.add(new GeoServerTileLayerInfo[] { old, modified });
                } catch (RuntimeException e) {
                    LOGGER.log(Level.SEVERE,
                            "Error saving tile layer '" + modified.getName() + "'", e);
                }
            }
            this.pendingModications.clear();
            this.pendingDeletes.clear();
        } finally {
            // Downgrade to read
            lock.downgradeToReadLock();
            try {
                // issue notifications
                for (String deletedLayerName : deletedNames) {
                    try {
                        // let the mediator deal with gwc to get rid of all the caches
                        mediator.layerRemoved(deletedLayerName);
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.SEVERE, "Error deleting tile layer '" + deletedLayerName
                                + "'", e);
                    }
                }

                for (GeoServerTileLayerInfo[] oldNew : modifications) {
                    final GeoServerTileLayerInfo old = oldNew[0];
                    final GeoServerTileLayerInfo modified = oldNew[1];
                    try {
                        if (old == null) {
                            // it's an addition
                            String layerName = modified.getName();
                            mediator.layerAdded(layerName);
                        } else {
                            // it's a modification
                            issueTileLayerInfoChangeNotifications(old, modified);
                        }
                    } catch (RuntimeException e) {
                        LOGGER.log(Level.SEVERE, "Error issuing change events for tile layer "
                                + modified +".  This may result in leaked tiles that will not be truncated.", e);
                    }
                }
            } finally {
                lock.releaseReadLock();
            }
        }
    }

    private void issueTileLayerInfoChangeNotifications(final GeoServerTileLayerInfo oldInfo,
            final GeoServerTileLayerInfo newInfo) {

        checkNotNull(oldInfo);
        checkNotNull(newInfo);
        checkNotNull(oldInfo.getName());
        checkNotNull(newInfo.getName());
        checkNotNull(oldInfo.getId());
        checkNotNull(newInfo.getId());
        checkArgument(equal(oldInfo.getId(), newInfo.getId()));

        final GWC mediator = GWC.get();

        final String oldLayerName = oldInfo.getName();
        final String layerName = newInfo.getName();

        final boolean isRename = !equal(oldLayerName, layerName);
        if (isRename) {
            mediator.layerRenamed(oldLayerName, layerName);
        }
        // FIXME: There should be a way to ask GWC to "truncate redundant caches" rather than doing
        //         all this detective work.

        // First, remove the entire layer cache for any removed gridset
        Set<XMLGridSubset> oldGridSubsets = oldInfo.getGridSubsets();
        Set<XMLGridSubset> newGridSubsets = newInfo.getGridSubsets();

        Set<String> oldGridSubsetNames = gridsetNames(oldGridSubsets);
        Set<String> newGridSubsetNames = gridsetNames(newGridSubsets);

        Set<String> removedGridSets = new HashSet<String>(oldGridSubsetNames);
        removedGridSets.removeAll(newGridSubsetNames);
        for (String removedGridset : removedGridSets) {
            mediator.deleteCacheByGridSetId(layerName, removedGridset);
        }

        // then proceed with any removed cache format/style on the remaining gridsets
        Set<String> oldFormats = new HashSet<String>(oldInfo.getMimeFormats());
        Set<String> newFormats = new HashSet<String>(newInfo.getMimeFormats());
        if (!oldFormats.equals(newFormats)) {
            oldFormats.removeAll(newFormats);
            for (String removedFormat : oldFormats) {
                String styleName = null;
                String gridSetName = null;
                BoundingBox bounds = null;
                mediator.truncate(layerName, styleName, gridSetName, bounds, removedFormat);
            }
        }

        Set<String> oldStyles = oldInfo.cachedStyles();
        Set<String> newStyles = newInfo.cachedStyles();
        
        if (!newStyles.equals(oldStyles)) {
            oldStyles = new HashSet<String>(oldStyles);
            oldStyles.removeAll(newStyles);
            for (String removedStyle : oldStyles) {
                mediator.truncateByLayerAndStyle(layerName, removedStyle);
            }
        }
    }

    private Set<String> gridsetNames(Set<XMLGridSubset> gridSubsets) {
        Set<String> names = new HashSet<String>();
        for (XMLGridSubset gridSubset : gridSubsets) {
            names.add(gridSubset.getGridSetName());
        }
        return names;
    }

    public void reset() {
        lock.acquireWriteLock();
        try {
            this.layerCache.invalidateAll();
            this.tileLayerCatalog.reset();
        } finally {
            lock.releaseWriteLock();
        }
    }
}
