/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLUnit.newXpathEngine;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.OpenLayersMapOutputFormat;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * WMS 1.3 GetCapabilities integration tests
 * 
 * <p>
 * These tests are initialy ported from the 1.1.1 capabilities integration tests
 * </p>
 * 
 * @author Gabriel Roldan
 * 
 */
public class CapabilitiesIntegrationTest extends WMSTestSupport {

    public CapabilitiesIntegrationTest() {
        super();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();        
    }
    
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
       
        Catalog catalog = getCatalog();
        DataStoreInfo info =catalog.getDataStoreByName(MockData.SF_PREFIX);
        info.setEnabled(false);
        catalog.save(info);
        
        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);
        
        // add a workspace qualified style
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        testData.addStyle(ws, "Lakes", "Lakes.sld", SystemTestData.class, catalog);
        StyleInfo lakesStyle = catalog.getStyleByName(ws, "Lakes");
        LayerInfo lakesLayer = catalog.getLayerByName(MockData.LAKES.getLocalPart());
        lakesLayer.setDefaultStyle(lakesStyle);
        catalog.save(lakesLayer);
    }
    
    
//    @Before
//    public void setProxyURL() throws Exception {
//
//        GeoServerInfo global = getGeoServer().getGlobal();
//        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
//        getGeoServer().save(global);
//    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }


  
    
    @org.junit.Test 
    public void testCapabilities() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
    }

    @org.junit.Test 
    public void testGetCapsContainsNoDisabledTypes() throws Exception {

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertEquals("WMS_Capabilities", doc.getDocumentElement().getNodeName());
        // see that disabled elements are disabled for good
        assertXpathEvaluatesTo("0", "count(//Name[text()='sf:PrimitiveGeoFeature'])", doc);

    }

    @org.junit.Test 
    public void testFilteredCapabilitiesCite() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0&namespace=cite"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wms:Layer/wms:Name[starts-with(., cite)]", dom)
                .getLength() > 0);
        assertEquals(0,
                xpath.getMatchingNodes("//wms:Layer/wms:Name[not(starts-with(., cite))]", dom)
                        .getLength());
    }

    @org.junit.Test 
    public void testLayerCount() throws Exception {
        List<LayerInfo> layers = new ArrayList<LayerInfo>(getCatalog().getLayers());
        for (ListIterator<LayerInfo> it = layers.listIterator(); it.hasNext();) {
            LayerInfo next = it.next();
            if (!next.enabled() || next.getName().equals(MockData.GEOMETRYLESS.getLocalPart())) {
                it.remove();
            }
        }
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();

        Document dom = dom(get("wms?request=GetCapabilities&version=1.3.0"), true);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        NodeList nodeLayers = xpath.getMatchingNodes(
                "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Layer", dom);

        assertEquals(layers.size() + groups.size(), nodeLayers.getLength());
    }

    @org.junit.Test 
    public void testWorkspaceQualified() throws Exception {
        Document dom = dom(get("cite/wms?request=getCapabilities&version=1.3.0"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath.getMatchingNodes("//wms:Layer/wms:Name[starts-with(., cite)]", dom)
                .getLength() > 0);
        assertEquals(0,
                xpath.getMatchingNodes("//wms:Layer/wms:Name[not(starts-with(., cite))]", dom)
                        .getLength());

        NodeList nodes = xpath.getMatchingNodes("//wms:Layer//wms:OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/ows"));
        }

    }

    @org.junit.Test 
    public void testLayerQualified() throws Exception {
        // Qualify the request with a layer.  Other layers should not be included.
        Document dom = dom(get("cite/Forests/wms?request=getCapabilities&version=1.3.0"), true);
        Element e = dom.getDocumentElement();
        assertEquals("WMS_Capabilities", e.getLocalName());
        XpathEngine xpath = XMLUnit.newXpathEngine();
        assertTrue(xpath
                .getMatchingNodes("//wms:Layer/wms:Name[starts-with(., cite:Forests)]", dom)
                .getLength() == 1);
        assertEquals(1, xpath.getMatchingNodes("//wms:Layer/wms:Layer", dom).getLength());

        NodeList nodes = xpath.getMatchingNodes("//wms:Layer//wms:OnlineResource", dom);
        assertTrue(nodes.getLength() > 0);
        for (int i = 0; i < nodes.getLength(); i++) {
            e = (Element) nodes.item(i);
            String attribute = e.getAttribute("xlink:href");
            assertTrue(attribute.contains("geoserver/cite/Forests/ows"));
        }

    }

    @org.junit.Test
    public void testAttribution() throws Exception {
        // Uncomment the following lines if you want to use DTD validation for these tests
        // (by passing false as the second param to getAsDOM())
        // BUG: Currently, this doesn't seem to actually validate the document, although
        // 'validation' fails if the DTD is missing

        // GeoServerInfo global = getGeoServer().getGlobal();
        // global.setProxyBaseUrl("src/test/resources/geoserver");
        // getGeoServer().save(global);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("0", "count(//wms:Attribution)", doc);

        // Add attribution to one of the layers
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        AttributionInfo attr = points.getAttribution();

        attr.setTitle("Point Provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);

        // Add href to same layer
        attr = points.getAttribution();
        attr.setHref("http://example.com/points/provider");
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:OnlineResource)", doc);

        // Add logo to same layer
        attr = points.getAttribution();
        attr.setLogoURL("http://example.com/points/logo");
        attr.setLogoType("image/logo");
        attr.setLogoHeight(50);
        attr.setLogoWidth(50);
        getCatalog().save(points);

        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:Title)", doc);
        assertXpathEvaluatesTo("1", "count(//wms:Attribution/wms:LogoURL)", doc);
    }

    @org.junit.Test 
    public void testAlternateStyles() throws Exception {
        // add an alternate style to Fifteen
        StyleInfo pointStyle = getCatalog().getStyleByName("point");
        LayerInfo layer = getCatalog().getLayerByName("Fifteen");
        layer.getStyles().add(pointStyle);
        getCatalog().save(layer);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);

        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='cdf:Fifteen'])", doc);
        assertXpathEvaluatesTo("2", "count(//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style)", doc);

        XpathEngine xpath = newXpathEngine();
        String href = xpath
                .evaluate(
                        "//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style[wms:Name='Default']/wms:LegendURL/wms:OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertFalse(href.contains("style"));
        href = xpath
                .evaluate(
                        "//wms:Layer[wms:Name='cdf:Fifteen']/wms:Style[wms:Name='point']/wms:LegendURL/wms:OnlineResource/@xlink:href",
                        doc);
        assertTrue(href.contains("GetLegendGraphic"));
        assertTrue(href.contains("layer=cdf%3AFifteen"));
        assertTrue(href.contains("style=point"));
    }

    @org.junit.Test
    public void testServiceMetadata() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setTitle("test title");
        service.setAbstract("test abstract");
        service.setAccessConstraints("test accessConstraints");
        service.setFees("test fees");
        service.getKeywords().clear();
        service.getKeywords().add(new Keyword("test keyword 1"));
        service.getKeywords().add(new Keyword("test keyword 2"));
        service.setMaintainer("test maintainer");
        service.setOnlineResource("http://example.com/geoserver");
        GeoServerInfo global = getGeoServer().getGlobal();
        ContactInfo contact = global.getContact();
        contact.setAddress("__address");
        contact.setAddressCity("__city");
        contact.setAddressCountry("__country");
        contact.setAddressPostalCode("__ZIP");
        contact.setAddressState("__state");
        contact.setAddressType("__type");
        contact.setContactEmail("e@mail");
        contact.setContactOrganization("__org");
        contact.setContactFacsimile("__fax");
        contact.setContactPerson("__me");
        contact.setContactPosition("__position");
        contact.setContactVoice("__phone");
        
        getGeoServer().save(global);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        //print(doc);

        String base = "wms:WMS_Capabilities/wms:Service/";
        assertXpathEvaluatesTo("WMS", base + "wms:Name", doc);
        assertXpathEvaluatesTo("test title", base + "wms:Title", doc);
        assertXpathEvaluatesTo("test abstract", base + "wms:Abstract", doc);
        assertXpathEvaluatesTo("test keyword 1", base + "wms:KeywordList/wms:Keyword[1]", doc);
        assertXpathEvaluatesTo("test keyword 2", base + "wms:KeywordList/wms:Keyword[2]", doc);
        assertXpathEvaluatesTo("http://example.com/geoserver", base + "wms:OnlineResource/@xlink:href", doc);
        
        String cinfo = base + "wms:ContactInformation/";
        assertXpathEvaluatesTo("__me", cinfo + "wms:ContactPersonPrimary/wms:ContactPerson", doc);
        assertXpathEvaluatesTo("__org", cinfo + "wms:ContactPersonPrimary/wms:ContactOrganization", doc);
        assertXpathEvaluatesTo("__position", cinfo + "wms:ContactPosition", doc);
        assertXpathEvaluatesTo("__type", cinfo + "wms:ContactAddress/wms:AddressType", doc);
        assertXpathEvaluatesTo("__address", cinfo + "wms:ContactAddress/wms:Address", doc);
        assertXpathEvaluatesTo("__city", cinfo + "wms:ContactAddress/wms:City", doc);
        assertXpathEvaluatesTo("__state", cinfo + "wms:ContactAddress/wms:StateOrProvince", doc);
        assertXpathEvaluatesTo("__ZIP", cinfo + "wms:ContactAddress/wms:PostCode", doc);
        assertXpathEvaluatesTo("__country", cinfo + "wms:ContactAddress/wms:Country", doc);
        assertXpathEvaluatesTo("__phone", cinfo + "wms:ContactVoiceTelephone", doc);
        assertXpathEvaluatesTo("__fax", cinfo + "wms:ContactFacsimileTelephone", doc);
        assertXpathEvaluatesTo("e@mail", cinfo + "wms:ContactElectronicMailAddress", doc);
    }
    
    @Test 
    public void testNoFeesOrContraints() throws Exception {
        final WMSInfo service = getGeoServer().getService(WMSInfo.class);
        service.setAccessConstraints(null);
        service.setFees(null);
        getGeoServer().save(service);

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        // print(doc);

        String base = "wms:WMS_Capabilities/wms:Service/";
        assertXpathEvaluatesTo("WMS", base + "wms:Name", doc);
        assertXpathEvaluatesTo("none", base + "wms:Fees", doc);
        assertXpathEvaluatesTo("none", base + "wms:AccessConstraints", doc);
    }
    
    @org.junit.Test
    public void testQueryable() throws Exception {
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setQueryable(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setQueryable(false);
        getCatalog().save(points);

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathEvaluatesTo("1", "//wms:Layer[wms:Name='" + linesName + "']/@queryable", doc);
        assertXpathEvaluatesTo("0", "//wms:Layer[wms:Name='" + pointsName + "']/@queryable", doc);
    }
    
    @org.junit.Test
    public void testOpaque() throws Exception {
        LayerInfo lines = getCatalog().getLayerByName(MockData.LINES.getLocalPart());
        lines.setOpaque(true);
        getCatalog().save(lines);
        LayerInfo points = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        points.setOpaque(false);
        getCatalog().save(points);

        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        String pointsName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();

        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathEvaluatesTo("1", "//wms:Layer[wms:Name='" + linesName + "']/@opaque", doc);
        assertXpathEvaluatesTo("0", "//wms:Layer[wms:Name='" + pointsName + "']/@opaque", doc);
    }

    @org.junit.Test
    public void testKeywordVocab() throws Exception {
        FeatureTypeInfo lines = getFeatureTypeInfo(MockData.LINES);

        Keyword kw = new Keyword("foo");
        kw.setVocabulary("bar");
        lines.getKeywords().add(kw);
        
        getCatalog().save(lines);

        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        
        kw = new Keyword("baz");
        kw.setVocabulary("bar");
        wms.getKeywords().add(kw);
        getGeoServer().save(wms);
        
        String linesName = MockData.LINES.getPrefix() + ":" + MockData.LINES.getLocalPart();
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        String xpath = "//wms:Layer[wms:Name='" + linesName + "']/wms:KeywordList/wms:Keyword[@vocabulary='bar']";
        assertXpathExists(xpath, doc);
        assertXpathEvaluatesTo("foo", xpath, doc);

        xpath = "//wms:Service/wms:KeywordList/wms:Keyword[@vocabulary='bar']";
        assertXpathExists(xpath, doc);
        assertXpathEvaluatesTo("baz", xpath, doc);

    }
    
    @org.junit.Test
    public void testBoundingBoxCRS84() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        assertXpathExists("/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:BoundingBox[@CRS = 'CRS:84']", doc);
        assertXpathExists("/wms:WMS_Capabilities/wms:Capability/wms:Layer//wms:Layer/wms:BoundingBox[@CRS = 'CRS:84']", doc);
    }
    
    @org.junit.Test 
    public void testMetadataLinks() throws Exception {
        String layerName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        LayerInfo layer = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        MetadataLinkInfo mdlink = getCatalog().getFactory().createMetadataLink();
        mdlink.setMetadataType("FGDC");
        mdlink.setContent("http://geoserver.org");
        mdlink.setType("text/xml");
        ResourceInfo resource = layer.getResource();
        resource.getMetadataLinks().add(mdlink);
        getCatalog().save(resource);
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        String xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:Format";
        assertXpathEvaluatesTo("text/xml", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/@type";
        assertXpathEvaluatesTo("FGDC", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:OnlineResource/@xlink:type";
        assertXpathEvaluatesTo("simple", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:MetadataURL/wms:OnlineResource/@xlink:href";
        assertXpathEvaluatesTo("http://geoserver.org", xpath, doc);
        
        // Test transforming localhost to proxyBaseUrl
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = global.getSettings().getProxyBaseUrl();
        mdlink.setContent("/metadata");
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata", xpath, doc);
        
        // Test KVP in URL
        String query = "key=value";
        mdlink.setContent("/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata?" + query, xpath, doc);

        mdlink.setContent("http://localhost/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("http://localhost/metadata?" + query, xpath, doc);
        
    }
    
    @org.junit.Test 
    public void testDataLinks() throws Exception {
        String layerName = MockData.POINTS.getPrefix() + ":" + MockData.POINTS.getLocalPart();
        
        LayerInfo layer = getCatalog().getLayerByName(MockData.POINTS.getLocalPart());
        DataLinkInfo mdlink = getCatalog().getFactory().createDataLink();
        mdlink.setContent("http://geoserver.org");
        mdlink.setType("text/xml");
        ResourceInfo resource = layer.getResource();
        resource.getDataLinks().add(mdlink);
        getCatalog().save(resource);
        
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        String xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:Format";
        assertXpathEvaluatesTo("text/xml", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:OnlineResource/@xlink:type";
        assertXpathEvaluatesTo("simple", xpath, doc);
        
        xpath = "//wms:Layer[wms:Name='" + layerName + "']/wms:DataURL/wms:OnlineResource/@xlink:href";
        assertXpathEvaluatesTo("http://geoserver.org", xpath, doc);
        
        // Test transforming localhost to proxyBaseUrl
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = global.getSettings().getProxyBaseUrl();
        mdlink.setContent("/metadata");
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata", xpath, doc);
        
        // Test KVP in URL
        String query = "key=value";
        mdlink.setContent("/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo(proxyBaseUrl + "/metadata?" + query, xpath, doc);

        mdlink.setContent("http://localhost/metadata?" + query);
        getCatalog().save(resource);
        
        doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("http://localhost/metadata?" + query, xpath, doc);
        
    }
    
    // [GEOS-6312] OpenLayers output format is not listed in WMS 1.3 capabilities document
    @Test
    public void testOpenlayersFormat() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);
        assertXpathEvaluatesTo("1", "count(//wms:GetMap[wms:Format = '" + OpenLayersMapOutputFormat.MIME_TYPE + "'])", doc);
    }

    @Test
    public void testStyleWorkspaceQualified() throws Exception {
        Document doc = getAsDOM("wms?service=WMS&request=getCapabilities&version=1.3.0", true);

        // check the style name got prefixed too
        assertXpathEvaluatesTo("cite:Lakes",
                "//wms:Layer[wms:Name='cite:Lakes']/wms:Style[1]/wms:Name", doc);
    }

}
