/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.spring.test.ApplicationContextMock;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

public class VectorTileMapOutputFormatTest {

    private static CoordinateReferenceSystem WEB_MERCATOR;

    private static CoordinateReferenceSystem WGS84;

    private static Style defaultPointStyle, defaultLineStyle, defaultPolygonStyle;

    private WMS wmsMock;

    private VectorTileMapOutputFormat outptFormat;

    private VectorTileBuilder tileBuilderMock;

    private FeatureLayer pointLayer, lineLayer, polygonLayer;

    @BeforeClass
    public static void beforeClass() throws Exception {
        defaultPointStyle = parseStyle("default_point.sld");
        defaultLineStyle = parseStyle("default_line.sld");
        defaultPolygonStyle = parseStyle("default_polygon.sld");

        // avoid lots of application context unset warnings in the console
        GeoServerExtensionsHelper.init(new ApplicationContextMock());

        WEB_MERCATOR = CRS.decode("EPSG:3857");
        WGS84 = CRS.decode("EPSG:4326", true);
    }

    @Before
    public void before() throws Exception {
        wmsMock = mock(WMS.class);

        tileBuilderMock = mock(VectorTileBuilder.class);

        VectorTileBuilderFactory tileBuilderFactory = mock(VectorTileBuilderFactory.class);
        when(tileBuilderFactory.getMimeType()).thenReturn("testMime");
        when(tileBuilderFactory.getOutputFormats()).thenReturn(
                ImmutableSet.of("testMime", "testFormat"));

        when(tileBuilderFactory.newBuilder(any(Rectangle.class), any(ReferencedEnvelope.class)))
                .thenReturn(tileBuilderMock);

        outptFormat = new VectorTileMapOutputFormat(wmsMock, tileBuilderFactory);

        MemoryDataStore ds = new MemoryDataStore();

        final String pointsTypeSpec = "sp:String,ip:Integer,geom:Point:srid=4326";
        final String linesTypeSpec = "sp:String,ip:Integer,geom:LineString:srid=4326";
        final String polyTypeSpec = "sp:String,ip:Integer,geom:Polygon:srid=4326";

        SimpleFeatureType pointType = DataUtilities.createType("points", pointsTypeSpec);
        SimpleFeatureType lineType = DataUtilities.createType("lines", linesTypeSpec);
        SimpleFeatureType polyType = DataUtilities.createType("polygons", polyTypeSpec);

        ds.addFeature(feature(pointType, "point1", "StringProp1_1", 1000, "POINT(1 1)"));
        ds.addFeature(feature(pointType, "point2", "StringProp1_2", 2000, "POINT(2 2)"));
        ds.addFeature(feature(pointType, "point3", "StringProp1_3", 3000, "POINT(3 3)"));

        ds.addFeature(feature(lineType, "line1", "StringProp2_1", 1000, "LINESTRING (1 1, 2 2)"));
        ds.addFeature(feature(lineType, "line1", "StringProp2_2", 2000, "LINESTRING (3 3, 4 4)"));
        ds.addFeature(feature(lineType, "line1", "StringProp2_3", 3000, "LINESTRING (5 5, 6 6)"));

        ds.addFeature(feature(polyType, "polygon1", "StringProp3_1", 1000,
                "POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))"));
        ds.addFeature(feature(polyType, "polygon2", "StringProp3_2", 2000,
                "POLYGON ((6 6, 7 7, 8 8, 9 9, 6 6))"));
        ds.addFeature(feature(polyType, "polygon3", "StringProp3_3", 3000,
                "POLYGON ((11 11, 12 12, 13 13, 14 14, 11 11))"));

        pointLayer = new FeatureLayer(ds.getFeatureSource("points"), defaultPointStyle);
        lineLayer = new FeatureLayer(ds.getFeatureSource("lines"), defaultLineStyle);
        polygonLayer = new FeatureLayer(ds.getFeatureSource("polygons"), defaultPolygonStyle);
    }

    @Test
    public void testSimple() throws Exception {

        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 180, -90, 90, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, pointLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outptFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1)).addFeature(eq("points"), eq("point1"), eq("geom"),
                any(Geometry.class), any(Map.class));
    }

    private WMSMapContent createMapContent(ReferencedEnvelope mapBounds, Rectangle renderingArea,
            Layer... layers) throws Exception {

        GetMapRequest mapRequest = createGetMapRequest(mapBounds, renderingArea);

        WMSMapContent map = new WMSMapContent(mapRequest);
        map.getViewport().setBounds(mapBounds);
        if (layers != null) {
            for (Layer l : layers) {
                map.addLayer(l);
            }
        }
        map.setMapWidth(renderingArea.width);
        map.setMapHeight(renderingArea.height);

        return map;
    }

    protected GetMapRequest createGetMapRequest(ReferencedEnvelope requestEnvelope,
            Rectangle renderingArea) {
        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");

        List<MapLayerInfo> layers = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        // for (int i = 0; i < layerNames.length; i++) {
        // LayerInfo layerInfo = getCatalog().getLayerByName(layerNames[i].getLocalPart());
        // try {
        // styles.add(layerInfo.getDefaultStyle().getStyle());
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // layers.add(new MapLayerInfo(layerInfo));
        // }

        request.setLayers(layers);
        request.setStyles(styles);
        request.setBbox((Envelope) requestEnvelope);
        request.setCrs(requestEnvelope.getCoordinateReferenceSystem());
        if (requestEnvelope.getCoordinateReferenceSystem() == WGS84) {
            request.setSRS("EPSG:4326");
        } else if (requestEnvelope.getCoordinateReferenceSystem() == WEB_MERCATOR) {
            request.setSRS("EPSG:3857");
        } else {
            throw new IllegalArgumentException("Please use one of the test CRS's");
        }
        request.setWidth(renderingArea.width);
        request.setHeight(renderingArea.height);
        request.setRawKvp(new HashMap<String, String>());
        return request;
    }

    // @Test
    // public void testMapBoxTileBuilder() throws Exception {
    //
    // Envelope mapBounds = new Envelope(-92.8, -93.2, 4.5, 4.6);
    // mapBounds = JTS.transform(mapBounds,
    // CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:900913"), true));
    //
    // WMSMapContent mapContent = createMapContent("EPSG:900913", mapBounds, POINTS, POLYGONS);
    // mapContent.setMapHeight(256);
    // mapContent.setMapWidth(256);
    //
    // MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
    //
    // VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(),
    // builderFact);
    // outputFormat.setTransformToScreenCoordinates(true);
    //
    // RawMap map = (RawMap) outputFormat.produceMap(mapContent);
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // map.writeTo(bos);
    // VectorTileDecoder decoder = new VectorTileDecoder();
    // decoder.setAutoScale(false);
    //
    // FeatureIterable feats = decoder.decode(bos.toByteArray());
    //
    // for (Feature feat : feats) {
    // System.out.println(feat.getLayerName() + ": ");
    // System.out.print(feat.getAttributes());
    // System.out.println(feat.getGeometry());
    // }
    //
    // bos.close();
    //
    // List<Feature> featList = feats.asList();
    // assertEquals(2, featList.size());
    // assertEquals("Points", featList.get(0).getLayerName());
    // assertTrue(featList.get(0).getGeometry() instanceof Point);
    // assertEquals(new Coordinate(641, 973),
    // ((Point) featList.get(0).getGeometry()).getCoordinate());
    // assertEquals("Polygons", featList.get(1).getLayerName());
    // assertTrue(featList.get(1).getGeometry() instanceof Polygon);
    // assertEquals(new Coordinate(646, 976),
    // (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    // }
    //
    // @Test
    // public void testMapBoxTileBuilderOtherCrs() throws Exception {
    // WMSMapContent mapContent = createMapContent("EPSG:4326", new Envelope(-92.8, -93.2, 4.5,
    // 4.6), POINTS, POLYGONS);
    //
    // mapContent.setMapHeight(256);
    // mapContent.setMapWidth(256);
    //
    // MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
    //
    // VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(),
    // builderFact);
    // outputFormat.setTransformToScreenCoordinates(true);
    //
    // RawMap map = (RawMap) outputFormat.produceMap(mapContent);
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // map.writeTo(bos);
    // VectorTileDecoder decoder = new VectorTileDecoder();
    // decoder.setAutoScale(false);
    //
    // FeatureIterable feats = decoder.decode(bos.toByteArray());
    //
    // for (Feature feat : feats) {
    // System.out.println(feat.getLayerName() + ": ");
    // System.out.print(feat.getAttributes());
    // System.out.println(feat.getGeometry());
    // }
    //
    // bos.close();
    //
    // List<Feature> featList = feats.asList();
    // assertEquals(2, featList.size());
    // assertEquals("Points", featList.get(0).getLayerName());
    // assertTrue(featList.get(0).getGeometry() instanceof Point);
    // assertEquals(new Coordinate(641, 973),
    // ((Point) featList.get(0).getGeometry()).getCoordinate());
    // assertEquals("Polygons", featList.get(1).getLayerName());
    // assertTrue(featList.get(1).getGeometry() instanceof Polygon);
    // assertEquals(new Coordinate(646, 976),
    // (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    // }

    protected static SimpleFeature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    private static Style parseStyle(String styleResource) throws IOException {
        try (InputStream in = GeoServerLoader.class.getResourceAsStream(styleResource)) {
            StyledLayerDescriptor sld = new SLDHandler().parse(in, null, null, null);
            return ((NamedLayer) sld.getStyledLayers()[0]).getStyles()[0];
        }
    }

}
