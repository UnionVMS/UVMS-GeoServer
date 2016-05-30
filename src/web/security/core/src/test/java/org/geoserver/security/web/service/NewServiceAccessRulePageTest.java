/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.security.web.role.NewRolePage;
import org.junit.Test;

public class NewServiceAccessRulePageTest extends AbstractSecurityWicketTestSupport {

    NewServiceAccessRulePage page;
    
    
    @Test
    public void testFill() throws Exception {
        
        initializeForXML();
        //insertValues();        
        tester.startPage(page=new NewServiceAccessRulePage());        
        tester.assertRenderedPage(NewServiceAccessRulePage.class);
        
        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.serviceChoice.getChoices(),"wms");        
        form.select("service", index);
        tester.executeAjaxEvent("form:service", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.methodChoice.getChoices(),"GetStyles");
        form.select("method", index);
        tester.assertComponent("form:roles:palette:recorder", Recorder.class);

        // add a role on the fly
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);
        form=tester.newFormTester("form");                
        form.setValue("name", "ROLE_NEW");
        form.submit("save");
        
        // assign the new role to the method
        form=tester.newFormTester("form");
        tester.assertRenderedPage(NewServiceAccessRulePage.class);
        form.setValue("roles:palette:recorder", gaService.getRoleByName("ROLE_NEW").getAuthority());
        
        // reopen new role dialog again to ensure that the current state is not lost
        form.submit("roles:addRole");
        tester.assertRenderedPage(NewRolePage.class);

        tester.clickLink("form:cancel");
        tester.assertRenderedPage(NewServiceAccessRulePage.class);
        
        // now save
        form=tester.newFormTester("form");
        form.submit("save");
        
        tester.assertErrorMessages(new String[0]);
        tester.assertRenderedPage(ServiceAccessRulePage.class);

        ServiceAccessRule foundRule=null;
        for (ServiceAccessRule rule : ServiceAccessRuleDAO.get().getRules()) {
            if ("wms".equals(rule.getService())&& "GetStyles".equals(rule.getMethod())) {
                foundRule = rule;
                break;
            }
        }
        assertNotNull(foundRule);
        assertEquals(1,foundRule.getRoles().size());
        assertEquals("ROLE_NEW",foundRule.getRoles().iterator().next());        
    }
    
    @Test
    public void testDuplicateRule() throws Exception {
        initializeForXML();
        initializeServiceRules();
        tester.startPage(page=new NewServiceAccessRulePage());
                
        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.serviceChoice.getChoices(),"wms");        
        form.select("service", index);
        tester.executeAjaxEvent("form:service", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.methodChoice.getChoices(),"GetMap");
        form.select("method", index);
        form.setValue("roles:palette:recorder", "ROLE_WMS");
                        
        form.submit("save");                
        assertTrue(testErrorMessagesWithRegExp(".*wms\\.GetMap.*"));
        tester.assertRenderedPage(NewServiceAccessRulePage.class);
    }
    
    @Test
    public void testEmptyRoles() throws Exception {
        initializeForXML();
        initializeServiceRules();
        tester.startPage(page=new NewServiceAccessRulePage());
                
        FormTester form = tester.newFormTester("form");
        int index = indexOf(page.serviceChoice.getChoices(),"wms");        
        form.select("service", index);
        tester.executeAjaxEvent("form:service", "onchange");
        form = tester.newFormTester("form");
        index = indexOf(page.methodChoice.getChoices(),"GetStyles");
        form.select("method", index);
                        
        form.submit("save");
        assertTrue(testErrorMessagesWithRegExp(".*has no role.*"));
        tester.assertRenderedPage(NewServiceAccessRulePage.class);
    }


    
    @Test
    public void testReadOnlyRoleService() throws Exception{
        initializeForXML();
        activateRORoleService();
        tester.startPage(page=new NewServiceAccessRulePage());
        tester.assertInvisible("form:roles:addRole");
    }

    protected int indexOf(List<? extends String> strings, String searchValue) {
        int index =0;
        for (String s : strings) {
            if (s.equals(searchValue))
                return index;
            index++;
        }
        assertTrue(index!=-1);
        return -1;
    }

}
