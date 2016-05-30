/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LayerTest extends CatalogRESTTestSupport {

    @After
    public void revertChanges() throws IOException {
        revertLayer(SystemTestData.BUILDINGS);
        revertLayer(SystemTestData.BRIDGES);
        StyleInfo si = getCatalog().getStyleByName("cite", "foo");
        if(si != null) {
            getCatalog().remove(si);
        }
    }
    
    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layers/cite:Buildings.xml");
        assertEquals( "layer", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("Buildings", "/layer/name", dom );
        // check the layer name is actually the first child (GEOS-3336 risked modifying
        // the order)
        assertXpathEvaluatesTo("Buildings", "/layer/*[1]", dom );
        assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/styles/Buildings.xml",
                "/layer/defaultStyle/atom:link/attribute::href", dom);
    }
    
    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM("/rest/layers/cite:Buildings.html" );
    }
    
    @Test
    public void testGetWrongLayer() throws Exception {
        // Parameters for the request
        String layer = "cite:Buildingssssss";
        // Request path
        String requestPath = "/rest/layers/" + layer + ".html";
        // Exception path
        String exception = "No such layer: " + layer;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getOutputStreamContent().contains(
                exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatusCode());
        assertFalse(response.getOutputStreamContent().contains(
                exception));
        // No exception thrown
        assertTrue(response.getOutputStreamContent().isEmpty());
    }
    
    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layers.xml");
        assertXpathEvaluatesTo(catalog.getLayers().size()+"", "count(//layer)", dom );
    }
    
    @Test
    public void testGetAllAsHTML() throws Exception {
        getAsDOM( "/rest/layers.html");
    }
    
    @Test
    public void testPut() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:Buildings" );
        assertEquals( "Buildings", l.getDefaultStyle().getName() );
        String xml = 
            "<layer>" +
              "<defaultStyle>Forests</defaultStyle>" + 
              "<styles>" + 
                "<style>Ponds</style>" +
              "</styles>" + 
            "</layer>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/layers/cite:Buildings", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        l = catalog.getLayerByName("cite:Buildings");
        assertEquals( "Forests", l.getDefaultStyle().getName() );
    }
    
    @Test
    public void testUpdateStyleJSON() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:Buildings" );
        assertEquals( "Buildings", l.getDefaultStyle().getName() );
        JSONObject json = (JSONObject) getAsJSON("/rest/layers/cite:Buildings.json");
        // print(json);
        JSONObject layer = (JSONObject) json.get("layer");
        JSONObject style = (JSONObject) layer.get("defaultStyle");
        style.put("name", "polygon");
        style.put("href", "http://localhost:8080/geoserver/rest/styles/polygon.json");
        String updatedJson = json.toString();
        MockHttpServletResponse response = 
                putAsServletResponse("/rest/layers/cite:Buildings", updatedJson, "application/json");
        assertEquals( 200, response.getStatusCode() );

        l = catalog.getLayerByName("cite:Buildings");
        assertEquals( "polygon", l.getDefaultStyle().getName() );
    }
    
    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getLayerByName( "cite:Buildings" ));
        
        assertEquals(200, deleteAsServletResponse("/layers/cite:Buildings").getStatusCode());
    }
    
    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getLayerByName( "cite:Buildings" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Buildings" ));
        
        assertEquals(200, deleteAsServletResponse("/rest/layers/cite:Buildings").getStatusCode());
        
        assertNull(catalog.getLayerByName( "cite:Buildings" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Buildings" ));
        
        assertNotNull(catalog.getLayerByName( "cite:Bridges" ));
        assertNotNull(catalog.getFeatureTypeByName( "cite", "Bridges" ));
        
        assertEquals(200, deleteAsServletResponse("/rest/layers/cite:Bridges?recurse=true").getStatusCode());
    
        assertNull(catalog.getLayerByName( "cite:Bridges" ));
        assertNull(catalog.getFeatureTypeByName( "cite", "Bridges" ));
    }

    @Test
    public void testPutWorkspaceStyle() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));
        assertNull(cat.getStyleByName("cite", "foo"));

        String xml = 
            "<style>" +
              "<name>foo</name>" +
              "<filename>foo.sld</filename>" + 
            "</style>";

        MockHttpServletResponse response =
            postAsServletResponse("/rest/workspaces/cite/styles", xml);
        assertEquals(201, response.getStatusCode());
        assertNotNull(cat.getStyleByName("cite", "foo"));

        xml = 
            "<layer>" + 
                "<defaultStyle>" + 
                    "<name>foo</name>" +
                    "<workspace>cite</workspace>" +
                "</defaultStyle>" +
                "<enabled>true</enabled>" + 
            "</layer>";
        response =
            putAsServletResponse("/rest/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatusCode());

        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultStyle());
        assertEquals("foo", l.getDefaultStyle().getName());
        assertNotNull(l.getDefaultStyle().getWorkspace());

        Document dom = getAsDOM("/rest/layers/cite:Buildings.xml");
        assertXpathExists("/layer/defaultStyle/name[text() = 'cite:foo']", dom);
        assertXpathExists("/layer/defaultStyle/workspace[text() = 'cite']", dom);
        assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/workspaces/cite/styles/foo.xml", 
            "//defaultStyle/atom:link/@href", dom );
    }
    
    @Test
    public void testPutWorkspaceAlternateStyle() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));
        assertNull(cat.getStyleByName("cite", "foo"));

        String xml = 
            "<style>" +
              "<name>foo</name>" +
              "<filename>foo.sld</filename>" + 
            "</style>";

        MockHttpServletResponse response =
            postAsServletResponse("/rest/workspaces/cite/styles", xml);
        assertEquals(201, response.getStatusCode());
        assertNotNull(cat.getStyleByName("cite", "foo"));

        xml = 
            "<layer>" + 
                "<styles>" +
                  "<style>" +
                    "<name>foo</name>" +
                    "<workspace>cite</workspace>" +
                  "</style>" +
                "</styles>" +
                "<enabled>true</enabled>" + 
            "</layer>";
        response =
            putAsServletResponse("/rest/layers/cite:Buildings", xml, "application/xml");
        assertEquals(200, response.getStatusCode());

        LayerInfo l = cat.getLayerByName("cite:Buildings");
        assertNotNull(l.getDefaultStyle());
        StyleInfo style = l.getStyles().iterator().next();
        assertEquals("foo", style.getName());
        assertNotNull(style.getWorkspace());

        Document dom = getAsDOM("/rest/layers/cite:Buildings.xml");
        assertXpathExists("/layer/styles/style/name[text() = 'cite:foo']", dom);
        assertXpathExists("/layer/styles/style/workspace[text() = 'cite']", dom);
        assertXpathEvaluatesTo("http://localhost:8080/geoserver/rest/workspaces/cite/styles/foo.xml", 
            "//styles/style/atom:link/@href", dom );
    }
}
