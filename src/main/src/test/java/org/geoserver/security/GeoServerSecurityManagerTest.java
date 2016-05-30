/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Category(SystemTest.class)
public class GeoServerSecurityManagerTest extends GeoServerSecurityTestSupport {

    @Test
    public void testAdminRole() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "geoserver", 
            (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
        auth.setAuthenticated(true);
        assertTrue(secMgr.checkAuthenticationForAdminRole(auth));
    }
    
    @Test
    public void testMasterPasswordForMigration() throws Exception {
        
        // simulate no user.properties file
        GeoServerSecurityManager secMgr = getSecurityManager();
        char[] generatedPW= secMgr.extractMasterPasswordForMigration(null);
        assertTrue(generatedPW.length==8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        //dumpPWInfoFile();
        
        Properties props = new Properties();
        String adminUser="user1";
        String noAdminUser="user2";
        
        // check all users with default password
        String defaultMasterePassword = new String(GeoServerSecurityManager.MASTER_PASSWD_DEFAULT);
        props.put(GeoServerUser.ADMIN_USERNAME, defaultMasterePassword+","+GeoServerRole.ADMIN_ROLE);
        props.put(adminUser, defaultMasterePassword+","+GeoServerRole.ADMIN_ROLE);
        props.put(noAdminUser, defaultMasterePassword+",ROLE_WFS");
        
        generatedPW= secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length==8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        assertFalse(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        assertFalse(masterPWInfoFileContains(adminUser));
        assertFalse(masterPWInfoFileContains(noAdminUser));
        //dumpPWInfoFile();
        
        // valid master password for noadminuser
        props.put(noAdminUser, "validPassword"+",ROLE_WFS");
        generatedPW= secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length==8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));

        // password to short  for adminuser
        props.put(adminUser, "abc"+","+GeoServerRole.ADMIN_ROLE);
        generatedPW= secMgr.extractMasterPasswordForMigration(props);
        assertTrue(generatedPW.length==8);
        assertTrue(masterPWInfoFileContains(new String(generatedPW)));
        
        // valid password for user having admin role
        
        String validPassword =  "validPassword";
        props.put(adminUser, validPassword+","+GeoServerRole.ADMIN_ROLE);
        generatedPW= secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(adminUser));
        //dumpPWInfoFile();
        
        // valid password for "admin" user
        props.put(GeoServerUser.ADMIN_USERNAME, validPassword+","+GeoServerRole.ADMIN_ROLE);
        generatedPW= secMgr.extractMasterPasswordForMigration(props);
        assertEquals(validPassword, new String(generatedPW));
        assertFalse(masterPWInfoFileContains(validPassword));
        assertTrue(masterPWInfoFileContains(GeoServerUser.ADMIN_USERNAME));
        //dumpPWInfoFile();                
    }
    
    @Test
    public void testMasterPasswordDump() throws Exception{
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try {
            assertFalse(secMgr.dumpMasterPassword(f));

            TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "geoserver",
                    (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(secMgr.dumpMasterPassword(f));
            dumpPWInfoFile(f);
            assertTrue(masterPWInfoFileContains(f, new String(secMgr.getMasterPassword())));
        } finally {
            f.delete();
        }
    }
    
    @Test
    public void testMasterPasswordDumpNotAuthorized() throws Exception{
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        File f = File.createTempFile("masterpw", "info");
        try {
            assertFalse(secMgr.dumpMasterPassword(f));

            TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", "geoserver",
                    (List) Arrays.asList(GeoServerRole.ADMIN_ROLE));
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(secMgr.dumpMasterPassword(f));
        } finally {
            f.delete();
        }
    }

    
    void dumpPWInfoFile(File infoFile) throws Exception {
        
        
        BufferedReader bf = new BufferedReader(new FileReader(infoFile));
        String line;
        while (( line = bf.readLine()) != null) {
            System.out.println(line);
        }
        bf.close();
        
    }

    
    void dumpPWInfoFile() throws Exception {
        dumpPWInfoFile(new File(getSecurityManager().getSecurityRoot(),GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME));                
    }

    boolean masterPWInfoFileContains(File infoFile,String searchString) throws Exception {        
        
        BufferedReader bf = new BufferedReader(new FileReader(infoFile));
        String line;
        while (( line = bf.readLine()) != null) {
            if (line.indexOf(searchString)!= -1) {
                bf.close();
                return true;
            }
        }
        bf.close();
        return false;
    }

    
    
    boolean masterPWInfoFileContains(String searchString) throws Exception {
        return masterPWInfoFileContains(new File(getSecurityManager().getSecurityRoot(),
                GeoServerSecurityManager.MASTER_PASSWD_INFO_FILENAME),searchString);
        
    }

    @Test
    public void testWebLoginChainSessionCreation() throws Exception {
        //GEOS-6077
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig config = secMgr.loadSecurityConfig();

        RequestFilterChain chain = 
            config.getFilterChain().getRequestChainByName(GeoServerSecurityFilterChain.WEB_LOGIN_CHAIN_NAME);
        assertTrue(chain.isAllowSessionCreation());
    }
}
