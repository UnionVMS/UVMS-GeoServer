/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletContext;

public class HTMLFeatureInfoOutputFormatTest extends WMSTestSupport {
    private HTMLFeatureInfoOutputFormat outputFormat;
    
    private FeatureCollectionType fcType;
    
    Map<String, Object> parameters;
    
    GetFeatureInfoRequest getFeatureInfoRequest;
    
    private static final String templateFolder = "/org/geoserver/wms/featureinfo/";
    
    private String currentTemplate;
    
    @Before
    public void setUp() throws URISyntaxException, IOException {
        outputFormat = new HTMLFeatureInfoOutputFormat(getWMS());
        
        currentTemplate = "test_content.ftl";
        // configure template loader
        GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(
                this.getClass(), getDataDirectory()) {
    
            @Override
            public Object findTemplateSource(String path) throws IOException {
                String templatePath;
                if (path.toLowerCase().contains("content")) {
                    templatePath = currentTemplate;
    
                } else {
                    templatePath = "empty.ftl";
                }
                try {
                    return new File(this.getClass()
                            .getResource(templateFolder + templatePath).toURI());
                } catch (URISyntaxException e) {
                    return null;
    
                }
    
            }
    
        };
        outputFormat.templateLoader = templateLoader;
        
        // test request with some parameters to use in templates
        Request request = new Request();
        parameters = new HashMap<String, Object>();
        parameters.put("LAYER", "testLayer");        
        Map<String, String> env = new HashMap<String, String>();
        env.put("TEST1", "VALUE1");
        env.put("TEST2", "VALUE2");        
        parameters.put("ENV", env);
        request.setKvp(parameters);
        
        Dispatcher.REQUEST.set(request);
        
        final FeatureTypeInfo featureType = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        
        fcType = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fcType.getFeature().add(featureType.getFeatureSource(null, null).getFeatures());
        
        // fake layer list
        List<MapLayerInfo> queryLayers = new ArrayList<MapLayerInfo>();               
        LayerInfo layerInfo = new LayerInfoImpl();
        layerInfo.setType(PublishedType.VECTOR);
        ResourceInfo resourceInfo = new FeatureTypeInfoImpl(null);
        NamespaceInfo nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix("topp");
        nameSpace.setURI("http://www.topp.org");
        resourceInfo.setNamespace(nameSpace);
        layerInfo.setResource(resourceInfo);
        MapLayerInfo mapLayerInfo = new MapLayerInfo(layerInfo);
        queryLayers.add(mapLayerInfo);
        getFeatureInfoRequest = new GetFeatureInfoRequest();
        getFeatureInfoRequest.setQueryLayers(queryLayers);
                
    }

    /**
     * Test request values are inserted in processed template
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testRequestParametersAreEvaluatedInTemplate() throws IOException {        
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        String result = new String(outStream.toByteArray());
         
        assertEquals("VALUE1,VALUE2,testLayer" , result);    
    }
    
    
    @Test
    public void testEnvironmentVariablesAreEvaluatedInTemplate() throws IOException {
        currentTemplate = "test_env_content.ftl";
        System.setProperty("TEST_PROPERTY", "MYVALUE");
        MockServletContext servletContext = (MockServletContext)applicationContext.getServletContext();
        servletContext.setInitParameter("TEST_INIT_PARAM", "MYPARAM");
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
            String result = new String(outStream.toByteArray());
             
            assertEquals("MYVALUE,MYPARAM" , result);
        } finally {
            System.clearProperty("TEST_PROPERTY");
        }
    }
    
    /**
     * Test that if template asks a request parameter that is not present in request
     * an exception is thrown.
     * 
     */
    @Test
    public void testErrorWhenRequestParametersAreNotDefined() {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        boolean error = false;
        
        // remove one parameter required in template
        parameters.remove("LAYER");
        try {
            outputFormat.write(fcType, getFeatureInfoRequest, outStream);
        } catch (IOException e) {
            error = true;
        }
        assertTrue(error); 
    }
    
    @Test
    public void testHTMLGetFeatureInfoCharset() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&width=20&height=20&x=10&y=10" + "&info_format=text/html";

        MockHttpServletResponse response = getAsServletResponse(request,"");

        // MimeType
        assertEquals("text/html", response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConcurrentRequests() throws Exception {
        FeatureTypeInfo featureType1 = getFeatureTypeInfo(MockData.PRIMITIVEGEOFEATURE);
        List<MapLayerInfo> layers1 = Collections.singletonList(new MapLayerInfo(
            getCatalog().getLayerByName(featureType1.prefixedName())));
        FeatureCollectionType type1 = WfsFactory.eINSTANCE.createFeatureCollectionType();
        type1.getFeature().add(featureType1.getFeatureSource(null, null).getFeatures());
        final FeatureTypeInfo featureType2 = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        List<MapLayerInfo> layers2 = Collections.singletonList(new MapLayerInfo(
            getCatalog().getLayerByName(featureType2.prefixedName())));
        FeatureCollectionType type2 = WfsFactory.eINSTANCE.createFeatureCollectionType();
        type2.getFeature().add(featureType2.getFeatureSource(null, null).getFeatures());
        final HTMLFeatureInfoOutputFormat format = new HTMLFeatureInfoOutputFormat(getWMS());
        format.templateLoader = new GeoServerTemplateLoader(getClass(), getDataDirectory()) {
            @Override
            public Object findTemplateSource(String path) throws IOException {
                String templatePath = "empty.ftl";
                if (path.toLowerCase().contains("content") && (this.resource != null) && 
                        this.resource.prefixedName().equals(featureType2.prefixedName())) {
                    templatePath = "test_content.ftl";
                }
                try {
                    return new File(this.getClass()
                        .getResource(templateFolder + templatePath).toURI());
                } catch (URISyntaxException e) {
                    return null;
                }
            }
        };
        int numRequests = 50;
        List<Callable<String>> tasks = new ArrayList<>(numRequests);
        for (int i = 0; i < numRequests; i++) {
            final GetFeatureInfoRequest request = new GetFeatureInfoRequest();
            request.setQueryLayers(((i % 2) == 0) ? layers1 : layers2);
            final FeatureCollectionType type = (((i % 2) == 0) ? type1 : type2);
            tasks.add(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    format.write(type, request, output);
                    return new String(output.toByteArray());
                }
            });
        }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = executor.invokeAll(tasks);
            for (int i = 0; i < numRequests; i++) {
                String info = futures.get(i).get();
                if ((i % 2) == 0) {
                    assertEquals("", info);
                } else {
                    assertNotEquals("", info);
                }
            }
        } finally {
            executor.shutdown();
        }
    }
}
