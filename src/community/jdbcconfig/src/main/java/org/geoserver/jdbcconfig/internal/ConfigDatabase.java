/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geoserver.jdbcconfig.internal.DbUtils.logStatement;
import static org.geoserver.jdbcconfig.internal.DbUtils.params;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitorAdapter;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.util.CacheProvider;
import org.geoserver.util.DefaultCacheProvider;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * 
 */
public class ConfigDatabase {

    public static final Logger LOGGER = Logging.getLogger(ConfigDatabase.class);

    private Dialect dialect;

    private DataSource dataSource;

    private DbMappings dbMappings;

    private CatalogImpl catalog;

    private GeoServer geoServer;

    private NamedParameterJdbcOperations template;

    private XStreamInfoSerialBinding binding;

    private Cache<String, Info> cache;

    private InfoRowMapper<CatalogInfo> catalogRowMapper;

    private InfoRowMapper<Info> configRowMapper;
    
    private CatalogClearingListener catalogListener;
    private ConfigClearingListener configListener;
    

    /**
     * Protected default constructor needed by spring-jdbc instrumentation
     */
    protected ConfigDatabase() {
        //
    }

    public ConfigDatabase(final DataSource dataSource, final XStreamInfoSerialBinding binding) {
        this(dataSource, binding, null);
    }

    public ConfigDatabase(final DataSource dataSource, final XStreamInfoSerialBinding binding,
            CacheProvider cacheProvider) {

        this.binding = binding;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        // cannot use dataSource at this point due to spring context config hack
        // in place to support tx during testing
        this.dataSource = dataSource;

        this.catalogRowMapper = new InfoRowMapper<CatalogInfo>(CatalogInfo.class, binding);
        this.configRowMapper = new InfoRowMapper<Info>(Info.class, binding);

        if (cacheProvider == null) {
            cacheProvider = DefaultCacheProvider.findProvider();
        }
        cache = cacheProvider.getCache("catalog");
    }

    private Dialect dialect() {
        if (dialect == null) {
            this.dialect = Dialect.detect(dataSource);
        }
        return dialect;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void initDb(@Nullable URL initScript) throws IOException {
        this.dbMappings = new DbMappings(dialect());
        if (null != initScript) {
            runInitScript(initScript);
        }
        dbMappings.initDb(template);
    }

    private void runInitScript(URL initScript) throws IOException {

        LOGGER.info("------------- Running catalog database init script " + initScript
                + " ------------");

        Util.runScript(initScript, template.getJdbcOperations(), LOGGER);
        
        LOGGER.info("Initialization SQL script run sucessfully");
    }

    public DbMappings getDbMappings() {
        return dbMappings;
    }

    public void setCatalog(CatalogImpl catalog) {
        this.catalog = catalog;
        this.binding.setCatalog(catalog);
        
        catalog.removeListeners(CatalogClearingListener.class);
        catalog.addListener(new CatalogClearingListener());
    }

    public CatalogImpl getCatalog() {
        return this.catalog;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;
        
        if(configListener!=null) geoServer.removeListener(configListener);
        configListener = new ConfigClearingListener();
        geoServer.addListener(configListener);
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }
    
    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {

        QueryBuilder<T> sqlBuilder = QueryBuilder.forCount(dialect, of, dbMappings).filter(filter);

        final StringBuilder sql = sqlBuilder.build();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        final int count;
        if (fullySupported) {
            final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
            logStatement(sql, namedParameters);

            count = template.queryForInt(sql.toString(), namedParameters);
        } else {
            LOGGER.fine("Filter is not fully supported, doing scan of supported part to return the number of matches");
            // going the expensive route, filtering as much as possible
            CloseableIterator<T> iterator = query(of, filter, null, null, (SortBy)null);
            try {
                return Iterators.size(iterator);
            } finally {
                iterator.close();
            }
        }
        return count;
    }
    
    public <T extends Info> CloseableIterator<T> query(final Class<T> of, final Filter filter,
            @Nullable Integer offset, @Nullable Integer limit, @Nullable SortBy sortOrder) {
        if(sortOrder == null) {
            return query(of, filter, offset, limit, new SortBy[]{});
        } else {
            return query(of, filter, offset, limit, new SortBy[]{sortOrder});
        }
    }
    
    public <T extends Info> CloseableIterator<T> query(final Class<T> of, final Filter filter,
            @Nullable Integer offset, @Nullable Integer limit, @Nullable SortBy... sortOrder) {

        checkNotNull(of);
        checkNotNull(filter);
        checkArgument(offset == null || offset.intValue() >= 0);
        checkArgument(limit == null || limit.intValue() >= 0);

        QueryBuilder<T> sqlBuilder = QueryBuilder.forIds(dialect, of, dbMappings).filter(filter)
                .offset(offset).limit(limit).sortOrder(sortOrder);

        final StringBuilder sql = sqlBuilder.build();
        final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        logStatement(sql, namedParameters);

        Stopwatch sw = Stopwatch.createStarted();
        // the oracle offset/limit implementation returns a two column result set
        // with rownum in the 2nd - queryForList will throw an exception
        List<String> ids = template.query(sql.toString(), namedParameters, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        });
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(Joiner.on("").join("query returned ", ids.size(), " records in ",
                    sw.toString()));
        }

        List<T> lazyTransformed = Lists.transform(ids, new Function<String, T>() {
            @Nullable
            @Override
            public T apply(String id) {
                return getById(id, of);
            }
        });


        CloseableIterator<T> result;
        Iterator<T> iterator = Iterators.filter(lazyTransformed.iterator(),
                com.google.common.base.Predicates.notNull());

        if (fullySupported) {
            result = new CloseableIteratorAdapter<T>(iterator);
        } else {
            // Apply the filter
            result = CloseableIteratorAdapter.filter(iterator, filter);
            // The offset and limit should not have been applied as part of the query
            assert(!sqlBuilder.isOffsetLimitApplied());
            // Apply offset and limits after filtering
            result = applyOffsetLimit(result, offset, limit);
        }

        return result;
    }

    private <T extends Info> CloseableIterator<T> applyOffsetLimit(CloseableIterator<T> iterator, Integer offset, Integer limit){
        if (offset != null) {
            Iterators.advance(iterator, offset.intValue());
        }
        if (limit != null) {
            iterator = CloseableIteratorAdapter.limit(iterator, limit.intValue());
        }
        return iterator;
    }
    
    public <T extends Info> List<T> queryAsList(final Class<T> of, final Filter filter,
            Integer offset, Integer count, SortBy sortOrder) {

        CloseableIterator<T> iterator = query(of, filter, offset, count, sortOrder);
        List<T> list;
        try {
            list = ImmutableList.copyOf(iterator);
        } finally {
            iterator.close();
        }
        return list;
    }

    public <T extends CatalogInfo> T getDefault(final String key, Class<T> type) {
        String sql = "SELECT ID FROM DEFAULT_OBJECT WHERE DEF_KEY = :key";

        String defaultObjectId;
        try {
            ImmutableMap<String, String> params = ImmutableMap.of("key", key);
            logStatement(sql, params);
            defaultObjectId = template.queryForObject(sql, params, String.class);
        } catch (EmptyResultDataAccessException notFound) {
            return null;
        }
        return getById(defaultObjectId, type);
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public <T extends Info> T add(final T info) {
        checkNotNull(info);
        checkNotNull(info.getId(), "Object has no id");
        checkArgument(!(info instanceof Proxy), "Added object shall not be a dynamic proxy");

        final String id = info.getId();

        byte[] value = binding.objectToEntry(info);

        final String blob = new String(value);
        final Class<T> interf = ClassMappings.fromImpl(info.getClass()).getInterface();
        final Integer typeId = dbMappings.getTypeId(interf);

        Map<String, ?> params = params("type_id", typeId, "id", id, "blob", blob);
        final String statement = String.format("insert into object (oid, type_id, id, blob) values (%s, :type_id, :id, :blob)",
                dialect.nextVal("seq_OBJECT"));
        logStatement(statement, params);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updateCount = template.update(statement, new MapSqlParameterSource(params), keyHolder, new String[] {"oid"});
        checkState(updateCount == 1, "Insert statement failed");
        // looks like some db's return the pk different than others, so lets try both ways
        Number key = (Number) keyHolder.getKeys().get("oid");
        if (key == null) {
            key = keyHolder.getKey();
        }
        addAttributes(info, key);

        cache.put(id, info);
        return getById(id, interf);
    }

    private void addAttributes(final Info info, final Number infoPk) {
        final String id = info.getId();
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Storing properties of " + id + " with pk " + infoPk);
        }

        final Iterable<Property> properties = dbMappings.properties(info);

        for (Property prop : properties) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Adding property " + prop.getPropertyName() + "='" + prop.getValue()
                        + "'");
            }

            final List<?> values = valueList(prop);

            Object propValue;
            Integer colIndex;

            for (int index = 0; index < values.size(); index++) {
                colIndex = prop.isCollectionProperty() ? (index + 1) : 0;
                propValue = values.get(index);
                final String storedValue = marshalValue(propValue);

                addAttribute(info, infoPk, prop, colIndex, storedValue);
            }
        }
    }

    private void addAttribute(final Info info, final Number infoPk, Property prop,
            Integer colIndex, final String storedValue) {
        Map<String, ?> params = params("value", storedValue);

        final String insertPropertySQL = "insert into object_property " //
                + "(oid, property_type, related_oid, related_property_type, colindex, value, id) " //
                + "values (:object_id, :property_type, :related_oid, :related_property_type, :colindex, :value, :id)";

        final boolean isRelationShip = prop.isRelationship();

        Integer relatedObjectId = null;
        final Integer concreteTargetPropertyOid;

        if (isRelationShip) {
            Info relatedObject = lookUpRelatedObject(info, prop, colIndex);
            if (relatedObject == null) {
                concreteTargetPropertyOid = null;
            } else {
                // the related property may refer to an abstract type (e.g.
                // LayerInfo.resource.name), so we need to find out the actual property type id (for
                // example, whether it belongs to FeatureTypeInfo or CoverageInfo)
                relatedObject = ModificationProxy.unwrap(relatedObject);
                relatedObjectId = this.findObjectId(relatedObject);

                Integer targetPropertyOid = prop.getPropertyType().getTargetPropertyOid();
                PropertyType targetProperty;
                String targetPropertyName;

                Class<?> targetQueryType;
                ClassMappings classMappings = ClassMappings.fromImpl(relatedObject.getClass());
                targetQueryType = classMappings.getInterface();
                targetProperty = dbMappings.getPropertyType(targetPropertyOid);
                targetPropertyName = targetProperty.getPropertyName();

                Set<Integer> propertyTypeIds;
                propertyTypeIds = dbMappings
                        .getPropertyTypeIds(targetQueryType, targetPropertyName);
                checkState(propertyTypeIds.size() == 1);
                concreteTargetPropertyOid = propertyTypeIds.iterator().next();
            }
        } else {
            concreteTargetPropertyOid = null;
        }

        final Number propertyType = prop.getPropertyType().getOid();
        final String id = info.getId();

        params = params("object_id", infoPk,//
                "property_type", propertyType,//
                "id", id,//
                "related_oid", relatedObjectId,//
                "related_property_type", concreteTargetPropertyOid, //
                "colindex", colIndex, //
                "value", storedValue);

        logStatement(insertPropertySQL, params);
        template.update(insertPropertySQL, params);
    }

    /**
     * @param info
     * @param prop
     * @return
     */
    private Info lookUpRelatedObject(final Info info, final Property prop,
            @Nullable Integer collectionIndex) {

        checkArgument(collectionIndex == 0 || prop.isCollectionProperty());

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        final Integer targetPropertyTypeId = prop.getPropertyType().getTargetPropertyOid();
        checkArgument(targetPropertyTypeId != null);

        final PropertyType targetPropertyType = dbMappings.getPropertyType(targetPropertyTypeId);
        checkState(targetPropertyType != null);

        final Class<?> targetType = dbMappings.getType(targetPropertyType.getObjectTypeOid());
        checkState(targetType != null);

        final String localPropertyName = prop.getPropertyName();
        String[] steps = localPropertyName.split("\\.");
        // Step back through ancestor property references If starting at a.b.c.d, then look at a.b.c, then a.b, then a
        for (int i = steps.length - 1; i >= 0; i--) {
            String backPropName = Strings.join(".", Arrays.copyOfRange(steps, 0, i));
            Object backProp = ff.property(backPropName).evaluate(info);
            if (backProp != null) {
                if (prop.isCollectionProperty() && (backProp instanceof Set || backProp instanceof List)) {
                    List<?> list;
                    if (backProp instanceof Set) {
                        list = asValueList(backProp);
                        if (list.size() > 0 && list.get(0) != null
                                && targetType.isAssignableFrom(list.get(0).getClass())) {
                            String targetPropertyName = targetPropertyType.getPropertyName();
                            final PropertyName expr = ff.property(targetPropertyName);
                            Collections.sort(list, new Comparator<Object>() {
                                @Override
                                public int compare(Object o1, Object o2) {
                                    Object v1 = expr.evaluate(o1);
                                    Object v2 = expr.evaluate(o2);
                                    String m1 = marshalValue(v1);
                                    String m2 = marshalValue(v2);
                                    return m1 == null ? (m2 == null ? 0 : -1) : (m2 == null ? 1
                                            : m1.compareTo(m2));
                                }
                            });
                        }
                    } else {
                        list = (List<?>) backProp;
                    }
                    if (collectionIndex <= list.size()) {
                        backProp = list.get(collectionIndex - 1);
                    }
                }
                if (targetType.isAssignableFrom(backProp.getClass())) {
                    return (Info) backProp;
                }
            }
        }
        // throw new IllegalArgumentException("Found no related object of type "
        // + targetType.getName() + " for property " + localPropertyName + " of " + info);
        return null;
    }

    private List<?> valueList(Property prop) {
        final Object value = prop.getValue();
        return asValueList(value);
    }

    private List<?> asValueList(final Object value) {
        final List<?> values;
        if (value instanceof List) {
            values = (List<?>) value;
        } else if (value instanceof Collection) {
            values = Lists.newArrayList((Collection<?>) value);
        } else {
            values = Lists.newArrayList(value);
        }
        return values;
    }

    /**
     * @return the stored representation of a scalar property value
     */
    private String marshalValue(Object propValue) {
        // TODO pad numeric values
        String marshalled = Converters.convert(propValue, String.class);
        return marshalled;
    }

    /**
     * @param info
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void remove(Info info) {

        final Integer oid = findObjectId(info);
        if (oid == null) {
            return;
        }
        cache.invalidate(info.getId());

        String deleteObject = "delete from object where id = :id";
        String deleteRelatedProperties = "delete from object_property where related_oid = :oid";

        int updateCount = template.update(deleteObject, ImmutableMap.of("id", info.getId()));
        if (updateCount != 1) {
            LOGGER.warning("Requested to delete " + info + " (" + info.getId()
                    + ") but nothing happened on the database.");
        }
        final int relatedPropCount = template.update(deleteRelatedProperties, params("oid", oid));
        LOGGER.fine("Removed " + relatedPropCount + " related properties of " + info.getId());

        cache.invalidate(info.getId());
    }

    /**
     * @param info
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public <T extends Info> T save(T info) {
        checkNotNull(info);

        final String id = info.getId();

        checkNotNull(id, "Can't modify an object with no id");

        final ModificationProxy modificationProxy = ModificationProxy.handler(info);
        Preconditions.checkNotNull(modificationProxy, "Not a modification proxy: ", info);

        final Info oldObject = (Info) modificationProxy.getProxyObject();

        cache.invalidate(id);

        // get changed properties before h.commit()s
        final Iterable<Property> changedProperties = dbMappings.changedProperties(oldObject, info);

        // see HACK block bellow
        final boolean updateResouceLayersName = info instanceof ResourceInfo
                && modificationProxy.getPropertyNames().contains("name");
        final boolean updateResourceLayersKeywords = 
                CollectionUtils.exists(modificationProxy.getPropertyNames(), new Predicate() {
            @Override
            public boolean evaluate(Object input) {
                return ((String)input).contains("keyword");
            }
            
        });
        
        modificationProxy.commit();

        Map<String, ?> params;

        // get the object's internal id
        final Integer objectId = findObjectId(info);
        final String blob;
        try {
            byte[] value = binding.objectToEntry(info);
            blob = new String(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
        String updateStatement = "update object set blob = :blob where oid = :oid";
        params = params("blob", blob, "oid", objectId);
        logStatement(updateStatement, params);
        template.update(updateStatement, params);

        updateQueryableProperties(oldObject, objectId, changedProperties);

        cache.invalidate(id);
        Class<T> clazz = ClassMappings.fromImpl(oldObject.getClass()).getInterface();

        // / <HACK>
        // we're explicitly changing the resourceinfo's layer name property here because
        // LayerInfo.getName() is a derived property. This can be removed once LayerInfo.name become
        // a regular JavaBean property
        if (info instanceof ResourceInfo) {
            if (updateResouceLayersName) {
                updateResourceLayerName((ResourceInfo) info);
            }
            if (updateResourceLayersKeywords) {
                updateResourceLayerKeywords((ResourceInfo) info);
            }
        }
        // / </HACK>
        return getById(id, clazz);
    }

    private <T> void updateResourceLayerName(ResourceInfo info) {
        final Object newValue = info.getName();
        Filter filter = Predicates.equal("resource.id", info.getId());
        List<LayerInfo> resourceLayers;
        resourceLayers = this.queryAsList(LayerInfo.class, filter, null, null, null);
        for (LayerInfo layer : resourceLayers) {
            Set<PropertyType> propertyTypes = dbMappings.getPropertyTypes(LayerInfo.class, "name");
            PropertyType propertyType = propertyTypes.iterator().next();
            Property changedProperty = new Property(propertyType, newValue);
            Integer layerOid = findObjectId(layer);
            updateQueryableProperties(layer, layerOid, ImmutableSet.of(changedProperty));
        }
    }
    private <T> void updateResourceLayerKeywords(ResourceInfo info) {
        final Object newValue = info.getKeywords();
        Filter filter = Predicates.equal("resource.id", info.getId());
        List<LayerInfo> resourceLayers;
        resourceLayers = this.queryAsList(LayerInfo.class, filter, null, null, null);
        for (LayerInfo layer : resourceLayers) {
            Set<PropertyType> propertyTypes = dbMappings.getPropertyTypes(LayerInfo.class, "resource.keywords.value");
            PropertyType propertyType = propertyTypes.iterator().next();
            Property changedProperty = new Property(propertyType, newValue);
            Integer layerOid = findObjectId(layer);
            updateQueryableProperties(layer, layerOid, ImmutableSet.of(changedProperty));
        }
    }

    private Integer findObjectId(final Info info) {
        final String id = info.getId();
        final String oidQuery = "select oid from object where id = :id";
        Map<String, ?> params = params("id", id);
        logStatement(oidQuery, params);
        final Integer objectId = template.queryForInt(oidQuery, params);
        Preconditions.checkState(objectId != null, "Object not found: " + id);
        return objectId;
    }

    private void updateQueryableProperties(final Info info, final Integer objectId,
            Iterable<Property> changedProperties) {

        Map<String, ?> params;

        final Integer oid = objectId;
        Integer propertyType;
        Integer relatedOid;
        Integer relatedPropertyType;
        Integer colIndex;
        String storedValue;

        for (Property changedProp : changedProperties) {
            LOGGER.finer("Updating property " + changedProp);

            final boolean isRelationship = changedProp.isRelationship();
            propertyType = changedProp.getPropertyType().getOid();

            final List<?> values = valueList(changedProp);

            for (int i = 0; i < values.size(); i++) {
                final Object rawValue = values.get(i);
                storedValue = marshalValue(rawValue);
                checkArgument(
                        changedProp.isCollectionProperty() || values.size() == 1,
                        "Got a multivalued value for a non collection property "
                                + changedProp.getPropertyName() + "=" + values);

                colIndex = changedProp.isCollectionProperty() ? (i + 1) : 0;

                if (isRelationship) {
                    final Info relatedObject = lookUpRelatedObject(info, changedProp, colIndex);
                    relatedOid = relatedObject == null ? null : findObjectId(relatedObject);
                    relatedPropertyType = changedProp.getPropertyType().getTargetPropertyOid();
                } else {
                    // it's a self property, lets update the value on the property table
                    relatedOid = null;
                    relatedPropertyType = null;
                }
                String sql = "update object_property set " //
                        + "related_oid = :related_oid, "//
                        + "related_property_type = :related_property_type, "//
                        + "value = :value "//
                        + "where oid = :oid and property_type = :property_type and colindex = :colindex";
                params = params("related_oid", relatedOid, "related_property_type",
                        relatedPropertyType, "value", storedValue, "oid", oid, "property_type",
                        propertyType, "colindex", colIndex);

                logStatement(sql, params);
                final int updateCnt = template.update(sql, params);

                if (updateCnt == 0) {
                    addAttribute(info, oid, changedProp, colIndex, storedValue);
                } else {
                    // prop existed already, lets update any related property that points to its old
                    // value
                    String updateRelated = "update object_property set value = :value "
                            + "where related_oid = :oid and related_property_type = :property_type and colindex = :colindex";
                    params = params("oid", oid, "property_type", propertyType, "colindex",
                            colIndex, "value", storedValue);
                    logStatement(updateRelated, params);
                    int relatedUpdateCnt = template.update(updateRelated, params);
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Updated " + relatedUpdateCnt + " back pointer properties to "
                                + changedProp.getPropertyName() + " of "
                                + info.getClass().getSimpleName() + "[" + info.getId() + "]");
                    }
                }
            }
            if (changedProp.isCollectionProperty()) {
                // delete any remaining collection value that's no longer in the value list
                String sql = "delete from object_property where oid=:oid and property_type=:property_type "
                        + "and colindex > :maxIndex";
                Integer maxIndex = Integer.valueOf(values.size());
                template.update(sql,
                        params("oid", oid, "property_type", propertyType, "maxIndex", maxIndex));
            }
        }
    }

    @Nullable
    public <T extends Info> T getById(final String id, final Class<T> type) {
        Assert.notNull(id, "id");

        Info info = null;
        try {
            final Callable<? extends Info> valueLoader;
            if (CatalogInfo.class.isAssignableFrom(type)) {
                valueLoader = new CatalogLoader(id);
            } else {
                valueLoader = new ConfigLoader(id);
            }

            info = cache.get(id, valueLoader);

        } catch (CacheLoader.InvalidCacheLoadException notFound) {
            return null;
        } catch (ExecutionException e) {
            Throwables.propagate(e.getCause());
        }

        if (info == null) {
            return null;
        }
        if (info instanceof CatalogInfo) {
            info = resolveCatalog((CatalogInfo) info);
        }
        else if (info instanceof ServiceInfo) {
            resolveTransient((ServiceInfo)info);
        }

        if (type.isAssignableFrom(info.getClass())) {
            // use ModificationProxy only in this case as returned object is cached. saveInternal
            // follows suite checking whether the object being saved is a mod proxy, but that's not
            // mandatory in this implementation and should only be the case when the object was
            // obtained by id
            return ModificationProxy.create(type.cast(info), type);
        }

        return null;
    }

    private <T extends CatalogInfo> T resolveCatalog(final T real) {
        if (real == null) {
            return null;
        }
        CatalogImpl catalog = getCatalog();
        catalog.resolve(real);
        // may the cached value have been serialized and hence lost transient fields? (that's why I
        // don't like having transient fields foreign to the domain model in the catalog config
        // objects)
        resolveTransient(real);

        return real;
    }

    private <T extends CatalogInfo> void resolveTransient(T real) {
        if (null == real) {
            return;
        }
        real = ModificationProxy.unwrap(real);
        if (real instanceof StyleInfoImpl || real instanceof StoreInfoImpl
                || real instanceof ResourceInfoImpl) {
            OwsUtils.set(real, "catalog", catalog);
        }
        if (real instanceof ResourceInfoImpl) {
            resolveTransient(((ResourceInfoImpl) real).getStore());
        } else if (real instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) real;
            resolveTransient(layer.getDefaultStyle());
            if (!layer.getStyles().isEmpty()) {
                for (StyleInfo s : layer.getStyles()) {
                    resolveTransient(s);
                }
            }
            resolveTransient(layer.getResource());
        } else if (real instanceof LayerGroupInfo) {
            for (PublishedInfo p : ((LayerGroupInfo) real).getLayers()) {
                resolveTransient(p);
            }
            for (StyleInfo s : ((LayerGroupInfo) real).getStyles()) {
                resolveTransient(s);
            }
        }
    }

    private <T extends ServiceInfo> void resolveTransient(T real) {
        real = ModificationProxy.unwrap(real);
        OwsUtils.resolveCollections(real);
        real.setGeoServer(getGeoServer());
    }

    /**
     * @param type
     * @return immutable list of results
     */
    public <T extends Info> List<T> getAll(final Class<T> clazz) {

        Map<String, ?> params = params("types", typesParam(clazz));

        final String sql = "select id from object where type_id in ( :types ) order by id";

        List<String> ids = template.queryForList(sql, params, String.class);

        List<T> transformed = Lists.transform(ids, new Function<String, T>() {
            @Nullable
            @Override
            public T apply(String input) {
                return getById(input, clazz);
            }
        });
        Iterable<T> filtered = Iterables.filter(transformed, com.google.common.base.Predicates.notNull());
        return ImmutableList.copyOf(filtered);
    }

    private <T extends Info> List<Integer> typesParam(final Class<T> clazz) {

        final Class<?>[] actualTypes;

        actualTypes = ClassMappings.fromInterface(clazz).concreteInterfaces();

        List<Integer> inValues = new ArrayList<Integer>(actualTypes.length);
        for (Class<?> type : actualTypes) {
            inValues.add(this.dbMappings.getTypeId(type));
        }

        return inValues;
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void setDefault(final String key, @Nullable final String id) {
        String sql;
        sql = "DELETE FROM DEFAULT_OBJECT WHERE DEF_KEY = :key";
        template.update(sql, params("key", key));
        if (id != null) {
            sql = "INSERT INTO DEFAULT_OBJECT (DEF_KEY, ID) VALUES(:key, :id)";
            template.update(sql, params("key", key, "id", id));
        }
    }

    public void dispose() {
        cache.invalidateAll();
        cache.cleanUp();
    }

    private final class CatalogLoader implements Callable<CatalogInfo> {

        private final String id;

        public CatalogLoader(final String id) {
            this.id = id;
        }

        @Override
        public CatalogInfo call() throws Exception {
            CatalogInfo info;
            try {
                String sql = "select blob from object where id = :id";
                Map<String, String> params = ImmutableMap.of("id", id);
                logStatement(sql, params);
                info = template.queryForObject(sql, params, catalogRowMapper);
            } catch (EmptyResultDataAccessException noSuchObject) {
                return null;
            }
            return info;
        }
    }

    private final class ConfigLoader implements Callable<Info> {

        private final String id;

        public ConfigLoader(final String id) {
            this.id = id;
        }

        @Override
        public Info call() throws Exception {
            Info info;
            try {
                info = template.queryForObject("select blob from object where id = :id",
                        ImmutableMap.of("id", id), configRowMapper);
            } catch (EmptyResultDataAccessException noSuchObject) {
                return null;
            }
            OwsUtils.resolveCollections(info);
            if (info instanceof GeoServerInfo) {

                GeoServerInfoImpl global = (GeoServerInfoImpl) info;
                if (global.getMetadata() == null) {
                    global.setMetadata(new MetadataMap());
                }
                if (global.getClientProperties() == null) {
                    global.setClientProperties(new HashMap<Object, Object>());
                }
                if (global.getCoverageAccess() == null) {
                    global.setCoverageAccess(new CoverageAccessInfoImpl());
                }
                if (global.getJAI() == null) {
                    global.setJAI(new JAIInfoImpl());
                }
            }
            if (info instanceof ServiceInfo) {
                ((ServiceInfo)info).setGeoServer(geoServer);
            }

            return info;
        }
    }

    /**
     * @return whether there exists a property named {@code propertyName} for the given type of
     *         object, and hence native sorting can be done over it.
     */
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        Set<PropertyType> propertyTypes = dbMappings.getPropertyTypes(type, propertyName);
        return !propertyTypes.isEmpty();
    }

    void clear(Info info) {
        cache.invalidate(info.getId());
    }
    
    /**
     * Listens to catalog events clearing cache entires when resources are modified.
     */
    // Copied from org.geoserver.catalog.ResourcePool
    public class CatalogClearingListener extends CatalogVisitorAdapter implements CatalogListener {

        public void handleAddEvent(CatalogAddEvent event) {
        }

        public void handleModifyEvent(CatalogModifyEvent event) {
        }

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            event.getSource().accept( this );
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) {
            event.getSource().accept( this );
        }

        public void reloaded() {
        }
       
        @Override
        public void visit(DataStoreInfo dataStore) {
            clear(dataStore);
        }
        
        @Override
        public void visit(CoverageStoreInfo coverageStore) {
            clear(coverageStore);
        }
        
        @Override
        public void visit(FeatureTypeInfo featureType) {
            clear(featureType);
        }

        @Override
        public void visit(WMSStoreInfo wmsStore) {
            clear(wmsStore);
        }

        @Override
        public void visit(StyleInfo style) {
            clear(style);
        }

        @Override
        public void visit(WorkspaceInfo workspace) {
            clear(workspace);
        }

        @Override
        public void visit(NamespaceInfo workspace) {
            clear(workspace);
        }

        @Override
        public void visit(CoverageInfo coverage) {
            clear(coverage);
        }

        @Override
        public void visit(LayerInfo layer) {
            clear(layer);
        }

        @Override
        public void visit(LayerGroupInfo layerGroup) {
            clear(layerGroup);
        }

        @Override
        public void visit(WMSLayerInfo wmsLayerInfoImpl) {
            clear(wmsLayerInfoImpl);
        }
        
        
    }
    /**
     * Listens to configuration events clearing cache entires when resources are modified.
     */
    public class ConfigClearingListener extends ConfigurationListenerAdapter {

        @Override
        public void handlePostGlobalChange(GeoServerInfo global) {
            clear(global);
        }

        @Override
        public void handleSettingsPostModified(SettingsInfo settings) {
            clear(settings);
        }

        @Override
        public void handleSettingsRemoved(SettingsInfo settings) {
            clear(settings);
        }

        @Override
        public void handlePostLoggingChange(LoggingInfo logging) {
            clear(logging);
        }

        @Override
        public void handlePostServiceChange(ServiceInfo service) {
            clear(service);
        }

        @Override
        public void handleServiceRemove(ServiceInfo service) {
            clear(service);
        }
    }

    
}
