/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.data.test.CiteTestData.BUILDINGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;

public class LayerPageTest extends GeoServerWicketTestSupport {

    public static QName GS_BUILDINGS = new QName(MockData.DEFAULT_URI, "Buildings",
            MockData.DEFAULT_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        //we don't want any of the defaults
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(BUILDINGS, getCatalog());

        Map<LayerProperty,Object> props = new HashMap();
        props.put(LayerProperty.STYLE, BUILDINGS.getLocalPart());
        testData.addVectorLayer(GS_BUILDINGS, props, getCatalog());
    }

    @Test
    public void testBasicActions() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();
        
        // check it has two layers
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));
        
        // sort on workspace once (top to bottom)
        String wsSortPath = "table:listContainer:sortableLinks:1:header:link";
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("cite", workspaces.get(0));
        assertEquals("gs", workspaces.get(1));
        
        // sort on workspace twice (bottom to top)
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("gs", workspaces.get(0));
        assertEquals("cite", workspaces.get(1));
        
        // select second layer
        String checkBoxPath = "table:listContainer:items:6:selectItemContainer:selectItem";
        CheckBox selector = (CheckBox) tester.getComponentFromLastRenderedPage(checkBoxPath);
        // dirty trick, how to set a form component value without a form
        tester.getServletRequest().setParameter(selector.getInputName(), "true");
        tester.executeAjaxEvent(selector, "onclick");
        assertEquals(1, table.getSelection().size());
        LayerInfo li = (LayerInfo) table.getSelection().get(0);
        assertEquals("cite", li.getResource().getStore().getWorkspace().getName());
    }

    private List<String> getWorkspaces(GeoServerTablePanel table) {
        Iterator it  = table.getDataProvider().iterator(0, 2);
        List<String> workspaces = new ArrayList<String>();
        while(it.hasNext()) {
            LayerInfo li = (LayerInfo) it.next();
            String wsName = li.getResource().getStore().getWorkspace().getName();
            workspaces.add(wsName);
        }
        return workspaces;
    }

}
