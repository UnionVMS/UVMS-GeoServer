/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.rest.CatalogRESTTestSupport;
import org.geoserver.wcs.WCSInfo;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WCSSettingsTest extends CatalogRESTTestSupport {
    
    @After 
    public void revertChanges() {
        revertService(WCSInfo.class, null);
    }

    public void testGetASJSON() throws Exception {
        JSON json = getAsJSON("/rest/services/wcs/settings.json");
        JSONObject jsonObject = (JSONObject) json;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("wcs", wcsinfo.get("id"));
        assertEquals("true", wcsinfo.get("enabled").toString().trim());
        assertEquals("My GeoServer WCS", wcsinfo.get("name"));
        assertEquals("false", wcsinfo.get("verbose").toString().trim());
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM("/rest/services/wcs/settings.xml");
        assertEquals("wcs", dom.getDocumentElement().getLocalName());
        assertEquals(1, dom.getElementsByTagName("name").getLength());
        assertXpathEvaluatesTo("true", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("WCS", "/wcs/name", dom);
        assertXpathEvaluatesTo("false", "/wcs/verbose", dom);
    }

    @Test
    public void testPutAsJSON() throws Exception {
        String json = "{'wcs': {'id':'wcs','enabled':'false','name':'WCS'}}";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/settings/",
                json, "text/json");
        assertEquals(200, response.getStatusCode());
        JSON jsonMod = getAsJSON("/rest/services/wcs/settings.json");
        JSONObject jsonObject = (JSONObject) jsonMod;
        assertNotNull(jsonObject);
        JSONObject wcsinfo = (JSONObject) jsonObject.get("wcs");
        assertEquals("false", wcsinfo.get("enabled").toString().trim());
        assertEquals("WCS", wcsinfo.get("name"));
    }

    @Test
    public void testPutASXML() throws Exception {
        String xml = "<wcs>"
                + "<id>wcs</id>"
                + "<enabled>false</enabled>"
                + "<name>WCS</name><title>GeoServer Web Coverage Service</title>"
                + "<maintainer>http://geoserver.org/comm</maintainer>"
                + "</wcs>";
        MockHttpServletResponse response = putAsServletResponse("/rest/services/wcs/settings", xml,
                "text/xml");
        assertEquals(200, response.getStatusCode());
        Document dom = getAsDOM("/rest/services/wcs/settings.xml");
        assertXpathEvaluatesTo("false", "/wcs/enabled", dom);
        assertXpathEvaluatesTo("WCS", "/wcs/name", dom);
    }

    @Test
    public void testDelete() throws Exception {
        assertEquals(405, deleteAsServletResponse("/rest/services/wcs/settings").getStatusCode());
    }
}
