/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import static org.junit.Assert.*;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import net.sf.json.JSONObject;

import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.security.CatalogMode;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Test for {@link DataAccessControlResource},{@link ServiceAccessControlResource} and
 * {@link RESTAccessControlResource}
 * 
 * @author christian
 *
 */
@TestSetup(run=TestSetupFrequency.REPEAT)
public class AccessControlResourceTest extends SecurityRESTTestSupport {

    final static String BASE_URI="/rest/security/acl/";
    
    final static String DATA_URI=BASE_URI+"layers";
    final static String DATA_URI_XML=DATA_URI+".xml";    
    final static String DATA_URI_JSON=DATA_URI+".json";
    
    final static String SERVICE_URI=BASE_URI+"services";
    final static String SERVICE_URI_XML=SERVICE_URI+".xml";
    final static String SERVICE_URI_JSON=SERVICE_URI+".json";
    
    final static String REST_URI=BASE_URI+"rest";
    final static String REST_URI_XML=REST_URI+".xml";
    final static String REST_URI_JSON=REST_URI+".json";
    
    final static String CATALOG_URI=BASE_URI+"catalog";
    final static String CATALOG_URI_XML=CATALOG_URI+".xml";
    final static String CATALOG_URI_JSON=CATALOG_URI+".json";
    
    final private static String TEST_ROLE1="TEST_ROLE1";
    final private static String TEST_ROLE2="TEST_ROLE2";
    final private static String TEST_ROLELIST=TEST_ROLE1+","+TEST_ROLE2;
                
     
    
    String createXMLBody(String[][] rules) {
        StringBuffer buff = new StringBuffer();
        buff.append("<").append(RuleXMLFormat.ROOTELEMENT).append(">\n");
        for (String [] rule : rules) {
            buff.append("<").append(RuleXMLFormat.RULEELEMENT).append(" ");
            buff.append(RuleXMLFormat.RESOURCEATTR).append("=\"").append(rule[0]);
            buff.append("\">");
            buff.append(rule[1]);
            buff.append("</").append(RuleXMLFormat.RULEELEMENT).append(">\n");
        }        
        buff.append("</").append(RuleXMLFormat.ROOTELEMENT).append(">\n");
        return buff.toString();
    }
    
    String createJSONBody(String[][] rules) {
        JSONObject json = new JSONObject();
        for (String [] rule : rules) {
            json.put(rule[0], rule[1]);
        }
        return json.toString(1);
    }
    
    void checkXMLResponse(Document dom, String[][] rules) throws XpathException {
        assertEquals( RuleXMLFormat.ROOTELEMENT, dom.getDocumentElement().getNodeName() );
        assertEquals(rules.length,
                dom.getDocumentElement().getElementsByTagName(RuleXMLFormat.RULEELEMENT).getLength());
        String pattern = "/"+RuleXMLFormat.ROOTELEMENT+"/"+ RuleXMLFormat.RULEELEMENT+"[@"
                + RuleXMLFormat.RESOURCEATTR+"='XXX']";
        for (String [] rule : rules) {
            String exp = pattern.replace("XXX", rule[0]);
            String roles = xp.evaluate(exp, dom);
            assertTrue(checkRolesStringsForEquality(rule[1], roles));
        }
    }
    
    
    /**
     * Checks role strings for equality
     * 
     * e. g. ROLE1,ROLE2 is equal to ROLE2,ROLE1
     * 
     * @param roleString1
     * @param roleString2
     * @return
     */
    boolean checkRolesStringsForEquality(String roleString1, String roleString2) {
        String[] roleArray1=roleString1.split(",");
        String[] roleArray2=roleString2.split(",");
        
        if (roleArray1.length!= roleArray2.length)
            return false;
        
        Set<String> roleSet1= new HashSet<String>();
        for (String role : roleArray1)
            roleSet1.add(role.trim());
        
        Set<String> roleSet2= new HashSet<String>();
        for (String role : roleArray2)
            roleSet2.add(role.trim());
        
        for (String role : roleSet1) {
            if (roleSet2.contains(role)==false)
                return false;
        }
        return true;
            

            
    }
        
    void checkJSONResponse(JSONObject json, String[][] rules) {        
        for (String [] rule : rules) {
            String roles=json.getString(rule[0]);
            assertTrue(checkRolesStringsForEquality(rule[1], roles));                        
        }               
    }

    
    String[][] getDefaultLayerRules() {
        return new String[][] { {"*.*.r","*"}, {"*.*.w","*"} };
    }
    
    String[][] getDefaultServiceRules() {
        return new String[][] { {"*.*","*"} };
    }
    
    String[][] getDefaultRestRules() {
        return new String[][] { {"/**:GET","ADMIN"},{"/**:POST,DELETE,PUT","ADMIN"} };
    }
    
    String[][] getDefaultRestRulesForDelete() {
        return new String[][] { {"%2F**:GET","ADMIN"},{"%2F**:POST,DELETE,PUT","ADMIN"} };
    }



    
    
    @Test
    public void testGet() throws Exception {
        
        String[][] layerRules=getDefaultLayerRules();
        
        
        JSONObject json = (JSONObject) getAsJSON( DATA_URI_JSON);
        //System.out.println(json.toString(1));
        checkJSONResponse(json, layerRules);
        
        Document dom = getAsDOM( DATA_URI_XML);               
        //print(dom);
        checkXMLResponse(dom, layerRules);
        
        String[][] serviceRules=getDefaultServiceRules();
        json = (JSONObject) getAsJSON( SERVICE_URI_JSON);
        checkJSONResponse(json, serviceRules);        
        dom = getAsDOM( SERVICE_URI_XML);        
        checkXMLResponse(dom, serviceRules);
        
        String[][] restRules=getDefaultRestRules();
        dom = getAsDOM( REST_URI_XML);        
        checkXMLResponse(dom, restRules);        
        json = (JSONObject) getAsJSON( REST_URI_JSON);
        checkJSONResponse(json, restRules);        
                        
    }
    
    
    @Test
    public void testDelete() throws Exception {
        
        String[][] layerRules=getDefaultLayerRules();
                
        assertEquals( 200,deleteAsServletResponse(DATA_URI+"/"+layerRules[0][0]).getStatusCode());
        assertEquals( 404,deleteAsServletResponse(DATA_URI+"/"+layerRules[0][0]).getStatusCode());
                                        
        assertEquals( 404,deleteAsServletResponse(SERVICE_URI+"/wfs.getFeature").getStatusCode());
        
        String[][] restRules=getDefaultRestRulesForDelete();
                
        assertEquals( 200,deleteAsServletResponse(REST_URI+"/"+restRules[0][0]).getStatusCode());
        assertEquals( 404,deleteAsServletResponse(REST_URI+"/"+restRules[0][0]).getStatusCode());
                                
    }

    
    
    @Test
    public void testXMLPost() throws Exception {
        
        // layer rules
        String[][] rules=getDefaultLayerRules();
        String[][] toBeAdded = { rules[0],{"ws.layer1.r",TEST_ROLELIST}};  //conflict
        assertEquals( 409, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );
        
        // check if nothing changed
        Document dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
        // add 
        String[][] toBeAdded2 = { {"ws.layer1.w",TEST_ROLE1}, {"ws.layer1.r",TEST_ROLELIST}};  
        String[][] expected = { rules[0],rules[1],toBeAdded2[0],toBeAdded2[1]};
        assertEquals( 200, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded2),"text/xml").getStatusCode() );

        dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, expected);                        

        // service rules
        
        rules=getDefaultServiceRules();
        toBeAdded2 =  new String [][]{ {"ws.*",TEST_ROLE1}, {"ws2.GetFeature",TEST_ROLELIST}};  
        assertEquals( 200, postAsServletResponse(SERVICE_URI_XML,createXMLBody(toBeAdded2),"text/xml").getStatusCode() );
        expected = new String[][]{ rules[0],toBeAdded2[0],toBeAdded2[1]};
        
        dom = getAsDOM( SERVICE_URI_XML);               
        checkXMLResponse(dom, expected);                        
        
        // check conflict
        assertEquals( 409, postAsServletResponse(SERVICE_URI_XML,createXMLBody(toBeAdded2),"text/xml").getStatusCode() );
        
        // REST rules
        
        rules=getDefaultRestRules();
        toBeAdded = new String[][] { rules[0],{"/myworkspace/**:GET",TEST_ROLELIST}};  //conflict
        assertEquals( 409, postAsServletResponse(REST_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );
        
        // check if nothing changed
        dom = getAsDOM( REST_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
        // add 
        toBeAdded2 = new String[][]{ {"/myworkspace/**:PUT,POST",TEST_ROLE1}, {"/myworkspace/**:GET",TEST_ROLELIST}};  
        expected = new String[][] { rules[0],rules[1],toBeAdded2[0],toBeAdded2[1]};
        assertEquals( 200, postAsServletResponse(REST_URI_XML,createXMLBody(toBeAdded2),"text/xml").getStatusCode() );

        dom = getAsDOM( REST_URI_XML);               
        checkXMLResponse(dom, expected);                        
        
    }
    
    @Test
    public void testJSONPost() throws Exception {
        
        // layer rules
        String[][] rules=getDefaultLayerRules();
        String[][] toBeAdded = { rules[0],{"ws.layer1.r",TEST_ROLELIST}};  //conflict
        assertEquals( 409, postAsServletResponse(DATA_URI_JSON,createJSONBody(toBeAdded),"text/json").getStatusCode() );
        
        // check if nothing changed
        JSONObject json = (JSONObject) getAsJSON( DATA_URI_JSON);               
        checkJSONResponse(json, rules);                        
        
        // add 
        String[][] toBeAdded2 = { {"ws.layer1.w",TEST_ROLE1}, {"ws.layer1.r",TEST_ROLELIST}};  
        String[][] expected = { rules[0],rules[1],toBeAdded2[0],toBeAdded2[1]};
        assertEquals( 200, postAsServletResponse(DATA_URI_JSON,createJSONBody(toBeAdded2),"text/json").getStatusCode() );

        json = (JSONObject) getAsJSON( DATA_URI_JSON);               
        checkJSONResponse(json, expected);                        

        // service rules
        
        rules=getDefaultServiceRules();
        toBeAdded2 =  new String [][]{ {"ws.*",TEST_ROLE1}, {"ws2.GetFeature",TEST_ROLELIST}};  
        assertEquals( 200, postAsServletResponse(SERVICE_URI_JSON,createJSONBody(toBeAdded2),"text/json").getStatusCode() );
        expected = new String[][]{ rules[0],toBeAdded2[0],toBeAdded2[1]};
        
        json = (JSONObject) getAsJSON( SERVICE_URI_JSON);               
        checkJSONResponse(json, expected);                        
        
        // check conflict
        assertEquals( 409, postAsServletResponse(SERVICE_URI_JSON,createJSONBody(toBeAdded2),"text/json").getStatusCode() );
        
        // REST rules
        
        rules=getDefaultRestRules();
        toBeAdded = new String[][] { rules[0],{"/myworkspace/**:GET",TEST_ROLELIST}};  //conflict
        assertEquals( 409, postAsServletResponse(REST_URI_JSON,createJSONBody(toBeAdded),"text/json").getStatusCode() );
        
        // check if nothing changed
        json = (JSONObject) getAsJSON( REST_URI_JSON);               
        checkJSONResponse(json, rules);                        
        
        // add 
        toBeAdded2 = new String[][]{ {"/myworkspace/**:PUT,POST",TEST_ROLE1}, {"/myworkspace/**:GET",TEST_ROLELIST}};  
        expected = new String[][] { rules[0],rules[1],toBeAdded2[0],toBeAdded2[1]};
        assertEquals( 200, postAsServletResponse(REST_URI_JSON,createJSONBody(toBeAdded2),"text/json").getStatusCode() );

        json = (JSONObject) getAsJSON( REST_URI_JSON);               
        checkJSONResponse(json, expected);                        
        
    }


    @Test
    public void testJSONPut() throws Exception {
        
        // layer rules
        String[][] rules=getDefaultLayerRules();        
        String[][] toBeModified = { rules[0],{"ws.layer1.r",TEST_ROLELIST}};  //conflict
        assertEquals( 409, putAsServletResponse(DATA_URI_JSON,createJSONBody(toBeModified),"text/json").getStatusCode() );
        
        // check if nothing changed
        JSONObject json = (JSONObject) getAsJSON( DATA_URI_JSON);               
        checkJSONResponse(json, rules);                        
        
        // modify 
        String[][] toBeModified2 = { {rules[0][0],TEST_ROLE1}, {rules[1][0],TEST_ROLELIST}};          
        assertEquals( 200, putAsServletResponse(DATA_URI_JSON,createJSONBody(toBeModified2),"text/json").getStatusCode() );

        json = (JSONObject) getAsJSON( DATA_URI_JSON);               
        checkJSONResponse(json, toBeModified2);                        

        // service rules
        
        rules=getDefaultServiceRules();
        toBeModified2 =  new String [][]{ {"ws.*",TEST_ROLE1}, {"ws2.GetFeature",TEST_ROLELIST}};  // conflict
        assertEquals( 409, putAsServletResponse(SERVICE_URI_JSON,createJSONBody(toBeModified2),"text/json").getStatusCode() );
                        
        json = (JSONObject) getAsJSON( SERVICE_URI_JSON);               
        checkJSONResponse(json, rules);                        
        
        assertEquals( 200, putAsServletResponse(SERVICE_URI_JSON,createJSONBody(new String[][]{}),"text/json").getStatusCode() );
                
        // REST rules
        
        rules=getDefaultRestRules();
        toBeModified = new String[][] { rules[0],{"/myworkspace/**:GET",TEST_ROLELIST}};  //conflict
        assertEquals( 409, putAsServletResponse(REST_URI_JSON,createJSONBody(toBeModified),"text/json").getStatusCode() );
        
        // check if nothing changed
        json = (JSONObject) getAsJSON( REST_URI_JSON);               
        checkJSONResponse(json, rules);                        
        
        // modify 
        toBeModified2 = new String[][] {{rules[0][0],TEST_ROLE1}, {rules[1][0],TEST_ROLELIST}};          
        assertEquals( 200, putAsServletResponse(REST_URI_JSON,createJSONBody(toBeModified2),"text/json").getStatusCode() );

        json = (JSONObject) getAsJSON( REST_URI_JSON);               
        checkJSONResponse(json, toBeModified2);                        
        
    }
    
    @Test
    public void testXMLPut() throws Exception {
        
        // layer rules
        String[][] rules=getDefaultLayerRules();        
        String[][] toBeModified = { rules[0],{"ws.layer1.r",TEST_ROLELIST}};  //conflict
        assertEquals( 409, putAsServletResponse(DATA_URI_XML,createXMLBody(toBeModified),"text/xml").getStatusCode() );
        
        // check if nothing changed
        Document dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
        // modify 
        String[][] toBeModified2 = { {rules[0][0],TEST_ROLE1}, {rules[1][0],TEST_ROLELIST}};          
        assertEquals( 200, putAsServletResponse(DATA_URI_XML,createXMLBody(toBeModified2),"text/xml").getStatusCode() );

        dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, toBeModified2);                        

        // service rules
        
        rules=getDefaultServiceRules();
        toBeModified2 =  new String [][]{ {"ws.*",TEST_ROLE1}, {"ws2.GetFeature",TEST_ROLELIST}};  // conflict
        assertEquals( 409, putAsServletResponse(SERVICE_URI_XML,createXMLBody(toBeModified2),"text/xml").getStatusCode() );
                        
        dom = getAsDOM( SERVICE_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
        assertEquals( 200, putAsServletResponse(SERVICE_URI_XML,createXMLBody(new String[][]{}),"text/xml").getStatusCode() );
                
        // REST rules
        
        rules=getDefaultRestRules();
        toBeModified = new String[][] { rules[0],{"/myworkspace/**:GET",TEST_ROLELIST}};  //conflict
        assertEquals( 409, putAsServletResponse(REST_URI_XML,createXMLBody(toBeModified),"text/xml").getStatusCode() );
        
        // check if nothing changed
        dom = getAsDOM( REST_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
        // modify 
        toBeModified2 = new String[][] {{rules[0][0],TEST_ROLE1}, {rules[1][0],TEST_ROLELIST}};          
        assertEquals( 200, putAsServletResponse(REST_URI_XML,createXMLBody(toBeModified2),"text/xml").getStatusCode() );

        dom = getAsDOM( REST_URI_XML);               
        checkXMLResponse(dom, toBeModified2);                        
        
    }


    @Test
    public void testCatalogMode() throws Exception {

        JSONObject json = (JSONObject) getAsJSON( CATALOG_URI_JSON);        
        String mode = (String) json.get(CatalogModeResource.MODE_ELEMENT);
        assertEquals(CatalogMode.HIDE.toString(),mode);
        
        Document dom = getAsDOM( CATALOG_URI_XML);               
        print(dom);
        assertEquals( CatalogModeResource.XML_ROOT_ELEM, dom.getDocumentElement().getNodeName() );
        NodeList nl = dom.getElementsByTagName(CatalogModeResource.MODE_ELEMENT);
        assertEquals(1,nl.getLength());
        mode = nl.item(0).getTextContent();
        assertEquals(CatalogMode.HIDE.toString(),mode);

        String jsonTemplate = "'{'\""+CatalogModeResource.MODE_ELEMENT+"\":\"{0}\"'}'"; 
        String xmlTemplate="<"+CatalogModeResource.XML_ROOT_ELEM+">"+"\n";
        xmlTemplate+=" <"+CatalogModeResource.MODE_ELEMENT+">{0}";
        xmlTemplate+="</"+CatalogModeResource.MODE_ELEMENT+">"+"\n";
        xmlTemplate+="</"+CatalogModeResource.XML_ROOT_ELEM+">"+"\n";
        
                
        assertEquals( 404, putAsServletResponse(CATALOG_URI_JSON,"{\"modexxxxx\": \"HIDE\"}","text/json").getStatusCode() );
        assertEquals( 422, putAsServletResponse(CATALOG_URI_JSON,MessageFormat.format(jsonTemplate,"ABC"),"text/json").getStatusCode() );
        assertEquals( 422, putAsServletResponse(CATALOG_URI_XML,MessageFormat.format(xmlTemplate,"ABC"),"text/xml").getStatusCode() );
        
        assertEquals( 200, putAsServletResponse(CATALOG_URI_JSON,MessageFormat.format(jsonTemplate,CatalogMode.MIXED.toString()),"text/json").getStatusCode() );
        json = (JSONObject) getAsJSON( CATALOG_URI_JSON);        
        mode = (String) json.get(CatalogModeResource.MODE_ELEMENT);
        assertEquals(CatalogMode.MIXED.toString(),mode);
        
        assertEquals( 200, putAsServletResponse(CATALOG_URI_XML,MessageFormat.format(xmlTemplate,CatalogMode.CHALLENGE.toString()),"text/xml").getStatusCode() );
        dom = getAsDOM( CATALOG_URI_XML);                       
        nl = dom.getElementsByTagName(CatalogModeResource.MODE_ELEMENT);        
        mode = nl.item(0).getTextContent();
        assertEquals(CatalogMode.CHALLENGE.toString(),mode);                                                                              
    }
    
    
    @Test
    public void testInvalidRules() throws Exception {
        
        // layer rules
        
        String[][] rules=getDefaultLayerRules();
        String[][] toBeAdded = { {"ws.layer1.r.c",TEST_ROLELIST}};  
        assertEquals( 422, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        Document dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);
        
        
        toBeAdded =  new String[][] { {"ws.layer1.x",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);
        
        toBeAdded =  new String[][] { {"*.layer1.r",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);
        
        toBeAdded =  new String[][] { {"ws.layer1.a",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(DATA_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        dom = getAsDOM( DATA_URI_XML);               
        checkXMLResponse(dom, rules);                        


        // services
        rules = getDefaultServiceRules();
        toBeAdded =  new String[][] { {"ws.getMap.c",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(SERVICE_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        dom = getAsDOM( SERVICE_URI_XML);               
        checkXMLResponse(dom, rules);
        
        toBeAdded =  new String[][] { {"*.getMap",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(SERVICE_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );                        
        // check if nothing changed
        dom = getAsDOM( SERVICE_URI_XML);               
        checkXMLResponse(dom, rules);


        rules=getDefaultRestRules();
        toBeAdded = new String[][] { rules[0],{"/myworkspace/**!!!GET",TEST_ROLELIST}};  //conflict
        assertEquals( 422, postAsServletResponse(REST_URI_XML,createXMLBody(toBeAdded),"text/xml").getStatusCode() );
        
        // check if nothing changed
        dom = getAsDOM( REST_URI_XML);               
        checkXMLResponse(dom, rules);                        
        
    }


    
    
    @Test
    public void testNotAuthorized() throws Exception {  
        logout();
        
        assertEquals( 403, getAsServletResponse(DATA_URI_XML).getStatusCode() );
        assertEquals( 403, getAsServletResponse(DATA_URI_JSON).getStatusCode() );
        assertEquals( 403, getAsServletResponse(SERVICE_URI_XML).getStatusCode() );
        assertEquals( 403, getAsServletResponse(SERVICE_URI_JSON).getStatusCode() );
        assertEquals( 403, getAsServletResponse(REST_URI_XML).getStatusCode() );
        assertEquals( 403, getAsServletResponse(REST_URI_JSON).getStatusCode() );
        
        assertEquals( 403, putAsServletResponse(DATA_URI_XML).getStatusCode() );
        assertEquals( 403, putAsServletResponse(DATA_URI_JSON).getStatusCode() );
        assertEquals( 403, putAsServletResponse(SERVICE_URI_XML).getStatusCode() );
        assertEquals( 403, putAsServletResponse(SERVICE_URI_JSON).getStatusCode() );
        assertEquals( 403, putAsServletResponse(REST_URI_XML).getStatusCode() );
        assertEquals( 403, putAsServletResponse(REST_URI_JSON).getStatusCode() );
        
        assertEquals( 403, postAsServletResponse(DATA_URI_XML,"").getStatusCode() );
        assertEquals( 403, postAsServletResponse(DATA_URI_JSON,"").getStatusCode() );
        assertEquals( 403, postAsServletResponse(SERVICE_URI_XML,"").getStatusCode() );
        assertEquals( 403, postAsServletResponse(SERVICE_URI_JSON,"").getStatusCode() );
        assertEquals( 403, postAsServletResponse(REST_URI_XML,"").getStatusCode() );
        assertEquals( 403, postAsServletResponse(REST_URI_JSON,"").getStatusCode() );
        
        assertEquals( 403, deleteAsServletResponse(DATA_URI_XML).getStatusCode() );
        assertEquals( 403, deleteAsServletResponse(SERVICE_URI_XML).getStatusCode() );
        assertEquals( 403, deleteAsServletResponse(REST_URI_XML).getStatusCode() );
        
        assertEquals( 403, getAsServletResponse(CATALOG_URI_XML).getStatusCode() );
        assertEquals( 403, getAsServletResponse(CATALOG_URI_JSON).getStatusCode() );
        assertEquals( 403, putAsServletResponse(CATALOG_URI_XML).getStatusCode() );
        assertEquals( 403, putAsServletResponse(CATALOG_URI_JSON).getStatusCode() );


                        
    }
    
    
   
    
}