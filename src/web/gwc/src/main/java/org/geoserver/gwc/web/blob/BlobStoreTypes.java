/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.BlobStoreConfig;

/**
 *
 * Access point for BlobStore Types stored in Spring
 *
 * @author Niels Charlier
 */
public final class BlobStoreTypes {
    
    private BlobStoreTypes() {}
    
    /**
     * Lazy loaded map of blob store types
     */
    private static Map<Class<? extends BlobStoreConfig>, BlobStoreType> TYPES;
            
    
    private static Map<Class<? extends BlobStoreConfig>, BlobStoreType> getTypes() {
        if (TYPES == null) {
            //the treemap with comparator makes sure that the types are always displayed in the
            //same order, alphabetically sorted on name
            TYPES = new TreeMap<Class<? extends BlobStoreConfig>, BlobStoreType>(
                    new Comparator<Class>() {
                        @Override
                        public int compare(Class o1, Class o2) {
                            return o1.toString().compareTo(o2.toString());
                        }
                    });
            for (BlobStoreType type : GeoWebCacheExtensions.extensions(BlobStoreType.class)) {
                TYPES.put(type.getConfigClass(), type);
            }
        }
        return TYPES;
    }

    /**
     * 
     * Get BlobStoreType from BlobStoreConfig class
     * 
     * @param clazz 
     * @return
     */
    public static BlobStoreType getFromClass(Class<? extends BlobStoreConfig> clazz) {
        return getTypes().get(clazz);
    }

    /**
     * Get all BlobStoreTypes
     * 
     * @return
     */
    public static List<BlobStoreType> getAll() {
        return new ArrayList<BlobStoreType>(getTypes().values());
    }

}