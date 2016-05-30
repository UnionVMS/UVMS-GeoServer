/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.factory.GeoTools;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class CoverageStoreFileUploadTest extends CatalogRESTTestSupport {

    @Before
    public void cleanup() {
        // wipe out everything under "mosaic"
        CoverageInfo coverage = getCatalog().getResourceByName("mosaic", CoverageInfo.class);
        if(coverage != null) {
            removeStore(coverage.getStore().getWorkspace().getName(), coverage.getStore().getName());
        }
    }
    
    @Test
    public void testWorldImageUploadZipped() throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/sf/coveragestores/usa/file.worldimage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        assertEquals( "coverageStore", d.getDocumentElement().getNodeName());
        
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("sf", "usa");
        assertNotNull(cs);
        CoverageInfo ci = getCatalog().getCoverageByName("sf", "usa");
        assertNotNull(ci);
    }
    
    @Test
    @Ignore
    // fixing https://osgeo-org.atlassian.net/browse/GEOS-6845, re-enable when a proper fix for spaces in
    // name has been made
    public void testUploadWithSpaces() throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/store%20with%20spaces/file.worldimage", bytes, "application/zip");
        assertEquals(500, response.getStatusCode());
    }
    
    @Test
    public void testUploadImageMosaic() throws Exception {
        URL zip = MockData.class.getResource( "watertemp.zip" );
        InputStream is = null;
        byte[] bytes;
        try  {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/watertemp/file.imagemosaic", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        // check the response contents
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        
        XMLAssert.assertXpathEvaluatesTo("watertemp", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);
        
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
    }
    
    @Test
    public void testHarvestImageMosaic() throws Exception {
        // Upload of the Mosaic via REST
        URL zip = MockData.class.getResource( "watertemp.zip" );
        InputStream is = null;
        byte[] bytes;
        try  {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/watertemp2/file.imagemosaic", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        // check the response contents
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        
        XMLAssert.assertXpathEvaluatesTo("watertemp2", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);
        
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp2");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp2");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
        
        // Harvesting of the Mosaic
        URL zipHarvest = MockData.class.getResource("harvesting.zip");
        // Extract a Byte array from the zip file
        is = null;
        try  {
            is = zipHarvest.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        // Create the POST request
        MockHttpServletRequest request = createRequest( "/rest/workspaces/gs/coveragestores/watertemp2/file.imagemosaic" ); 
        request.setMethod( "POST" );
        request.setContentType("application/zip" );
        request.setBodyContent(bytes);
        request.setHeader( "Content-type", "application/zip" );
        // Get The response
        response = dispatch( request );
        // Get the Mosaic Reader
        GridCoverageReader reader = storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals("2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z", 
                reader.getMetadataValue(metadataNames[0]));
    }

    @Test
    public void testHarvestImageMosaicWithDirectory() throws Exception {
        // Upload of the Mosaic via REST
        URL zip = MockData.class.getResource("watertemp.zip");
        InputStream is = null;
        byte[] bytes;
        try {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }

        MockHttpServletResponse response = putAsServletResponse(
                "/rest/workspaces/gs/coveragestores/watertemp3/file.imagemosaic", bytes,
                "application/zip");
        assertEquals(201, response.getStatusCode());

        // check the response contents
        String content = response.getOutputStreamContent();
        Document d = dom(new ByteArrayInputStream(content.getBytes()));

        XMLAssert.assertXpathEvaluatesTo("watertemp3", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);

        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp3");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp3");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());

        // Harvesting of the Mosaic
        URL zipHarvest = MockData.class.getResource("harvesting.zip");
        File newZip = new File("./target/harvesting2.zip");
        // Copy the content of the first zip to the second
        FileUtils.copyInputStreamToFile(zipHarvest.openStream(), newZip);
        File outputDirectory = new File("./target/harvesting");
        outputDirectory.mkdir();
        RESTUtils.unzipFile(newZip, outputDirectory);
        // Create the POST request
        MockHttpServletRequest request = createRequest("/rest/workspaces/gs/coveragestores/watertemp3/external.imagemosaic");
        request.setMethod("POST");
        request.setContentType("text/plain");
        request.setBodyContent("file:///" + outputDirectory.getAbsolutePath());
        request.setHeader("Content-type", "text/plain");
        // Get The response
        response = dispatch(request);
        // Get the Mosaic Reader
        GridCoverageReader reader = storeInfo.getGridCoverageReader(null,
                GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals("2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                reader.getMetadataValue(metadataNames[0]));
        // Removal of the temporary directory
        FileUtils.deleteDirectory(outputDirectory);
    }

    @Test
    public void testHarvestExternalImageMosaic() throws Exception {
        // Check if an already existing directory called "mosaic" is present
        URL resource = getClass().getResource("test-data/mosaic");
        if (resource != null) {
            File oldDir = DataUtilities.urlToFile(resource);
            if (oldDir.exists()) {
                FileUtils.deleteDirectory(oldDir);
            }
        }
        // reading of the mosaic directory
        File mosaic = readMosaic();
        // Creation of the builder for building a new CoverageStore
        CatalogBuilder builder = new CatalogBuilder(getCatalog());
        // Definition of the workspace associated to the coverage
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("gs");
        // Creation of a CoverageStore
        CoverageStoreInfo store = builder.buildCoverageStore("watertemp4");
        store.setURL(DataUtilities.fileToURL(mosaic).toExternalForm());
        store.setWorkspace(ws);
        ImageMosaicFormat imageMosaicFormat = new ImageMosaicFormat();
        store.setType((imageMosaicFormat.getName()));
        // Addition to the catalog
        getCatalog().add(store);
        builder.setStore(store);
        // Input reader used for reading the mosaic folder
        GridCoverage2DReader reader = null;
        // Reader used for checking if the mosaic has been configured correctly
        StructuredGridCoverage2DReader reader2 = null;

        try {
            // Selection of the reader to use for the mosaic
            reader = imageMosaicFormat.getReader(DataUtilities
                    .fileToURL(mosaic));

            // configure the coverage
            configureCoverageInfo(builder, store, reader);

            // check the coverage is actually there
            CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp4");
            assertNotNull(storeInfo);
            CoverageInfo ci = getCatalog().getCoverageByName("mosaic");
            assertNotNull(ci);
            assertEquals(storeInfo, ci.getStore());

            // Harvesting of the Mosaic
            URL zipHarvest = MockData.class.getResource("harvesting.zip");
            // Extract a Byte array from the zip file
            InputStream is = null;
            byte[] bytes;
            try {
                is = zipHarvest.openStream();
                bytes = IOUtils.toByteArray(is);
            } finally {
                IOUtils.closeQuietly(is);
            }
            // Create the POST request
            MockHttpServletRequest request = createRequest("/rest/workspaces/gs/coveragestores/watertemp4/file.imagemosaic");
            request.setMethod("POST");
            request.setContentType("application/zip");
            request.setBodyContent(bytes);
            request.setHeader("Content-type", "application/zip");
            // Get The response
            MockHttpServletResponse response = dispatch(request);
            // Get the Mosaic Reader
            reader2 = (StructuredGridCoverage2DReader) storeInfo.getGridCoverageReader(null,
                    GeoTools.getDefaultHints());
            // Test if all the TIME DOMAINS are present
            String[] metadataNames = reader2.getMetadataNames();
            assertNotNull(metadataNames);
            assertEquals("true", reader2.getMetadataValue("HAS_TIME_DOMAIN"));
            assertEquals(
                    "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                    reader2.getMetadataValue(metadataNames[0]));
            // Removal of all the data associated to the mosaic
            reader2.delete(true);
        } finally {
            // Reader disposal
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (reader2 != null) {
                try {
                    reader2.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }
    
    @Test
    public void testReHarvestSingleTiff() throws Exception {
        // Check if an already existing directory called "mosaic" is present
        URL resource = getClass().getResource("test-data/mosaic");
        if (resource != null) {
            File oldDir = DataUtilities.urlToFile(resource);
            if (oldDir.exists()) {
                FileUtils.deleteDirectory(oldDir);
            }
        }
        // reading of the mosaic directory
        File mosaic = readMosaic();
        // Creation of the builder for building a new CoverageStore
        CatalogBuilder builder = new CatalogBuilder(getCatalog());
        // Definition of the workspace associated to the coverage
        WorkspaceInfo ws = getCatalog().getWorkspaceByName("gs");
        // Creation of a CoverageStore
        CoverageStoreInfo store = builder.buildCoverageStore("watertemp5");
        store.setURL(DataUtilities.fileToURL(mosaic).toExternalForm());
        store.setWorkspace(ws);
        ImageMosaicFormat imageMosaicFormat = new ImageMosaicFormat();
        store.setType((imageMosaicFormat.getName()));
        // Addition to the catalog
        getCatalog().add(store);
        builder.setStore(store);
        // Input reader used for reading the mosaic folder
        GridCoverage2DReader reader = null;
        // Reader used for checking if the mosaic has been configured correctly
        StructuredGridCoverage2DReader reader2 = null;

        try {
            // Selection of the reader to use for the mosaic
            reader = imageMosaicFormat.getReader(DataUtilities
                    .fileToURL(mosaic));

            // configure the coverage
            configureCoverageInfo(builder, store, reader);

            // check the coverage is actually there
            CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp5");
            assertNotNull(storeInfo);
            CoverageInfo ci = getCatalog().getCoverageByName("mosaic");
            assertNotNull(ci);
            assertEquals(storeInfo, ci.getStore());

            // Harvesting of the Mosaic
            URL zipHarvest = MockData.class.getResource("harvesting.zip");
            // Extract the first file as payload (the tiff)
            byte[] bytes = null;
            try(ZipInputStream zis = new ZipInputStream(zipHarvest.openStream())) {
                ZipEntry entry = null;
                while((entry = zis.getNextEntry()) != null) {
                    if("NCOM_wattemp_000_20081102T0000000_12.tiff".equals(entry.getName())) {
                        bytes = IOUtils.toByteArray(zis);
                    }
                }
                if(bytes == null) {
                    fail("Could not find the expected zip entry NCOM_wattemp_000_20081102T0000000_12.tiff");
                }
            } 
            reader2 = uploadGeotiffAndCheck(storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
            // now re-upload, used to blow up
            reader2 = uploadGeotiffAndCheck(storeInfo, bytes, "NCOM_wattemp_000_20081102T0000000_12.tiff");
            // Removal of all the data associated to the mosaic
            reader2.delete(true);
        } finally {
            // Reader disposal
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
            if (reader2 != null) {
                try {
                    reader2.dispose();
                } catch (Throwable t) {
                    // Does nothing
                }
            }
        }
    }

    private StructuredGridCoverage2DReader uploadGeotiffAndCheck(CoverageStoreInfo storeInfo,
            byte[] bytes, String filename) throws Exception, IOException {
        StructuredGridCoverage2DReader reader2;
        // Create the POST request
        MockHttpServletRequest request = createRequest("/rest/workspaces/gs/coveragestores/watertemp5/file.imagemosaic?filename=" + filename);
        request.setMethod("POST");
        request.setContentType("image/tiff");
        request.setBodyContent(bytes);
        request.setHeader("Content-type", "image/tiff");
        // Get The response
        assertEquals(202, dispatch(request).getStatusCode());
        // Get the Mosaic Reader
        reader2 = (StructuredGridCoverage2DReader) storeInfo.getGridCoverageReader(null,
                GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader2.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader2.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals(
                "2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z",
                reader2.getMetadataValue(metadataNames[0]));
        return reader2;
    }

    private File readMosaic() throws NoSuchAuthorityCodeException, FactoryException, IOException {
        // Select the zip file containing the mosaic
        URL mosaicZip = getClass().getResource("test-data/watertemp2.zip");
        File zipFile = DataUtilities.urlToFile(mosaicZip);

        // Creation of another zip file which is a copy of the one before
        File newZip = new File(zipFile.getParentFile(), "watertemp2_temp.zip");
        // Copy the content of the first zip to the second
        FileUtils.copyFile(zipFile, newZip);

        File mosaic = new File(zipFile.getParentFile(), "mosaic");
        if (mosaic.exists()) {
            FileUtils.deleteDirectory(mosaic);
        }
        assertTrue(mosaic.mkdirs());

        RESTUtils.unzipFile(newZip, mosaic);
        return mosaic;
    }

    private void configureCoverageInfo(CatalogBuilder builder, CoverageStoreInfo storeInfo,
            GridCoverage2DReader reader) throws Exception, IOException {
        // coverage read params
        final Map customParameters = new HashMap();

        CoverageInfo cinfo = builder.buildCoverage(reader, customParameters);

        // get the coverage name
        String name = reader.getGridCoverageNames()[0];
        cinfo.setName(name);
        cinfo.setNativeCoverageName(name);

        // add the store
        getCatalog().add(cinfo);
    }
}
