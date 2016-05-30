/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DimensionsRasterGetFeatureInfoTest extends WMSDimensionsTestSupport {
    
    static final String BASE_URL = "wms?service=WMS&version=1.1.0&request=GetFeatureInfo" +
        "&layers=watertemp&styles=&bbox=0.237,40.562,14.593,44.558&width=200&height=80" +
        "&srs=EPSG:4326&format=image/png" +
        "&query_layers=watertemp&feature_count=50";
    
    static final double EPS = 1e-03;
    
    private XpathEngine xpath;
    
    @Before
    public void setXpathEngine() throws Exception {            
        xpath = XMLUnit.newXpathEngine();
    };
    
    /**
     * Ensures there is at most one feature at the specified location, and returns its feature id
     * 
     * @param baseFeatureInfo The GetFeatureInfo request, minus x and y
     * @param x
     * @param y
     * @param layerName TODO
     * @return
     */
    Double getFeatureAt(String baseFeatureInfo, int x, int y, String layerName) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(baseFeatureInfo
                + "&info_format=application/vnd.ogc.gml&x=" + x + "&y=" + y);
        assertEquals("application/vnd.ogc.gml", response.getContentType());
        Document doc = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
        String sCount = xpath.evaluate("count(//" + layerName + ")", doc);
        int count = Integer.valueOf(sCount);

        if (count == 0) {
            return null;
        } else if (count == 1) {
            return Double.valueOf(xpath.evaluate("//" + layerName + "/sf:GRAY_INDEX", doc));
        } else {
            fail("Found more than one feature: " + count);
            return null; // just to make the compiler happy, fail throws an unchecked exception
        }
    }
    
    @Test 
    public void testDefaultValues() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        // this one should be medium
        assertEquals(14.51, getFeatureAt(BASE_URL, 36, 31, "sf:watertemp"), EPS);
        // this one hot
        assertEquals(19.15, getFeatureAt(BASE_URL, 68, 72, "sf:watertemp"), EPS);
    }
    
    @Test 
    public void testElevation() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        // this one should be the no-data
        String url = BASE_URL + "&elevation=100";
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.492, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }
    
    @Test
    public void testTime() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z";

        // should be similar to the default, but with different shades of color
        assertEquals(14.592, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        assertEquals(19.371, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }
    
    @Test
    public void testTimeElevation() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(WATTEMP, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);

        String url = BASE_URL + "&time=2008-10-31T00:00:00.000Z&elevation=100";
        // this one should be the no-data
        assertEquals(-30000, getFeatureAt(url, 36, 31, "sf:watertemp"), EPS);
        // and this one should be medium
        assertEquals(14.134, getFeatureAt(url, 68, 72, "sf:watertemp"), EPS);
    }
    
    @Test
    public void testTimeRange() throws Exception {
        setupRasterDimension(TIMERANGES, ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        setupRasterDimension(TIMERANGES, "wavelength", DimensionPresentation.LIST, null, null, null);
        setupRasterDimension(TIMERANGES, "date", DimensionPresentation.LIST, null, null, null);
        
        String layer = getLayerId(TIMERANGES);
        String baseUrl = "wms?LAYERS=" + layer + "&STYLES=temperature&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetFeatureInfo&SRS=EPSG:4326" +
                "&BBOX=-0.89131513678082,40.246933882167,15.721292974683,44.873229811941&WIDTH=200&HEIGHT=80&query_layers=" + layer;

        // last range
        String url = baseUrl + "&TIME=2008-11-05T00:00:00.000Z/2008-11-06T12:00:00.000Z";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertEquals(14.782, getFeatureAt(url, 68, 72, layer), EPS);
        
        // in the middle hole, no data
        url = baseUrl + "&TIME=2008-11-04T12:00:00.000Z/2008-11-04T16:00:00.000Z";
        assertNull(getFeatureAt(url, 36, 31, layer));
        
        // first range
        url = baseUrl + "&TIME=2008-10-31T12:00:00.000Z/2008-10-31T16:00:00.000Z";
        assertEquals(-30000, getFeatureAt(url, 36, 31, layer), EPS);
        assertEquals(20.027, getFeatureAt(url, 68, 72, layer), EPS);
    }
    
    
}
