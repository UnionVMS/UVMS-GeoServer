/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.gwc.GWC;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geowebcache.config.BlobStoreConfig;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.FileBlobStoreConfig;
import org.geowebcache.layer.TileLayer;
import org.junit.Test;

/**
 * 
 * Test for the BlobStorePage
 * 
 * @author Niels Charlier
 *
 */
public class BlobStorePageTest extends GeoServerWicketTestSupport {
        
    
    @Test
    public void testPage() {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        tester.assertRenderedPage(BlobStorePage.class);
        
        tester.assertComponent("selector", Form.class);
        tester.assertComponent("selector:typeOfBlobStore", DropDownChoice.class);
        tester.assertComponent("blobConfigContainer", MarkupContainer.class);
        
        tester.assertInvisible("blobConfigContainer:blobStoreForm");
        
        DropDownChoice typeOfBlobStore = (DropDownChoice) tester.getComponentFromLastRenderedPage("selector:typeOfBlobStore");
        assertEquals(1, typeOfBlobStore.getChoices().size());
        assertEquals("File BlobStore", typeOfBlobStore.getChoices().get(0).toString());
        
        executeAjaxEventBehavior("selector:typeOfBlobStore", "onchange", "0");
                        
        tester.assertVisible("blobConfigContainer:blobStoreForm");        
        tester.assertComponent("blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);        
    }   
    
    @Test
    public void testNew() throws ConfigurationException {
        BlobStorePage page = new BlobStorePage();

        tester.startPage(page);
        executeAjaxEventBehavior("selector:typeOfBlobStore", "onchange", "0");
        
        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("id", "myblobstore");       
        formTester.setValue("enabled", false);
        formTester.setValue("blobSpecificPanel:baseDirectory:border:paramValue", "/mydir");
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "onclick");
        
        List<BlobStoreConfig> blobStores = GWC.get().getBlobStores();
        BlobStoreConfig config = blobStores.get(0);
        assertTrue(config instanceof FileBlobStoreConfig);
        assertEquals("myblobstore", config.getId());
        assertEquals("/mydir", ((FileBlobStoreConfig) config).getBaseDirectory());
        assertEquals(4096, ((FileBlobStoreConfig) config).getFileSystemBlockSize());
        
        GWC.get().removeBlobStores(Collections.singleton("myblobstore"));
    }
    
    @Test
    public void testModify() throws ConfigurationException {
        FileBlobStoreConfig fconfig = new FileBlobStoreConfig("myblobstore");
        fconfig.setFileSystemBlockSize(1024);
        fconfig.setBaseDirectory("/mydir");
        GWC.get().addBlobStore(fconfig);
        TileLayer layer = GWC.get().getTileLayerByName("cite:Lakes");
        layer.setBlobStoreId("myblobstore");
        GWC.get().save(layer);
        
        BlobStorePage page = new BlobStorePage(fconfig);

        tester.startPage(page);   
        tester.assertVisible("blobConfigContainer:blobStoreForm");        
        tester.assertComponent("blobConfigContainer:blobStoreForm:blobSpecificPanel", FileBlobStorePanel.class);
        
        FormTester formTester = tester.newFormTester("blobConfigContainer:blobStoreForm");
        formTester.setValue("id", "yourblobstore");
        formTester.setValue("blobSpecificPanel:baseDirectory:border:paramValue", "/yourdir");
        formTester.submit();
        tester.executeAjaxEvent("blobConfigContainer:blobStoreForm:save", "onclick");
        
        BlobStoreConfig config = GWC.get().getBlobStores().get(0);
        assertTrue(config instanceof FileBlobStoreConfig);
        assertEquals("yourblobstore", config.getId());
        assertEquals("/yourdir", ((FileBlobStoreConfig) config).getBaseDirectory());    
                
        //test updated id!
        layer = GWC.get().getTileLayerByName("cite:Lakes");
        assertEquals("yourblobstore", layer.getBlobStoreId());
        
        GWC.get().removeBlobStores(Collections.singleton("yourblobstore"));
    }
    
}
