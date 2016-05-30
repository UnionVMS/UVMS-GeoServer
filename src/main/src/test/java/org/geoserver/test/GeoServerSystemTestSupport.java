/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.codec.binary.Base64;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.TestHttpClientProvider;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.ServiceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.logging.LoggingUtils;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.AccessMode;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.util.logging.Log4JLoggerFactory;
import org.geotools.util.logging.Logging;
import org.geotools.xml.XSD;
import org.junit.After;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;
import com.mockrunner.mock.web.MockServletConfig;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.MockServletOutputStream;

import net.sf.json.JSON;
import net.sf.json.JSONSerializer;

/**
 * Base test class for GeoServer system tests that require a fully configured spring context and
 * work off a real data directory provided by {@link SystemTestData}.
 * <h2>Subclass Hooks</h2>
 * <p>
 * Subclasses extending this base class have the following hooks avaialble:
 * <ul>
 *   <li>{@link #setUpTestData(SystemTestData)} - Perform post configuration of the {@link SystemTestData} 
 *   instance</li>
 *   <li>{@link #onSetUp(SystemTestData)} - Perform setup after the system has been fully initialized
 *   <li>{@link #onTearDown(SystemTestData)} - Perform teardown before the system is to be shutdown 
 * </ul>
 * </p>
 * <h2>Test Setup Frequency</h2>
 * <p>
 * By default the setup cycle is executed once for extensions of this class. Subclasses that require
 * a different test setup frequency should annotate themselves with the appropriate {@link TestSetup}
 * annotation. For example to implement a repeated setup:
 * <code><pre> 
 *  {@literal @}TestSetup(run=TestSetupFrequency.REPEAT)
 *  public class MyTest extends GeoServerSystemTestSupport {
 *  
 *  }
 * </pre></code>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
@TestSetup(run=TestSetupFrequency.ONCE)
public class GeoServerSystemTestSupport extends GeoServerBaseTestSupport<SystemTestData> {

    protected SystemTestData createTestData() throws Exception {
        return new SystemTestData();
    }

    /**
     * spring application context containing the integrated geoserver
     */
    protected static GeoServerTestApplicationContext applicationContext;

    /**
     * credentials for mock requests
     */
    protected String username, password;

    /**
     * Cached dispatcher, it has its own app context, so it's expensive to build
     */
    protected static DispatcherServlet dispatcher;

    protected final void setUp(SystemTestData testData) throws Exception {
        // speed up xpath evaluations
        try {
            // see
            // http://stackoverflow.com/questions/6340802/java-xpath-apache-jaxp-implementation-performance
            Class.forName("com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault");
            System.setProperty("com.sun.org.apache.xml.internal.dtm.DTMManager",
                    "com.sun.org.apache.xml.internal.dtm.ref.DTMManagerDefault");
        } catch (Exception e) {
            // ignore on VM where this optimization does not apply
        }

        // disable security manager to speed up tests, we are spending a lot of time in privileged
        // blocks
        System.setSecurityManager(null);

        // setup quiet logging (we need to to this here because Data
        // is loaded before GoeServer has a chance to setup logging for good)
        try {
            Logging.ALL.setLoggerFactory(Log4JLoggerFactory.getInstance());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not configure log4j logging redirection", e);
        }
        System.setProperty(LoggingUtils.RELINQUISH_LOG4J_CONTROL, "true");
        GeoServerResourceLoader loader = new GeoServerResourceLoader(testData.getDataDirectoryRoot());
        LoggingUtils.configureGeoServerLogging(loader, getClass().getResourceAsStream(getLogConfiguration()), false, true, null);

        setUpTestData(testData);
        
        // put the mock http server in test mode
        TestHttpClientProvider.startTest();

        // if we have data, create a mock servlet context and start up the spring configuration
        if (testData.isTestDataAvailable()) {
            MockServletContext servletContext = new MockServletContext();
            servletContext.setInitParameter("GEOSERVER_DATA_DIR", testData.getDataDirectoryRoot()
                    .getPath());
            servletContext.setInitParameter("serviceStrategy", "PARTIAL-BUFFER2");
            
            //set up a fake WEB-INF directory
            if (testData.getDataDirectoryRoot().canWrite()) {
                File webinf = new File(testData.getDataDirectoryRoot(), "WEB-INF");
                webinf.mkdir();
                
                servletContext.setRealPath("WEB-INF", webinf.getAbsolutePath());
            }

            List<String> contexts = new ArrayList();
            setUpSpring(contexts);

            applicationContext = new GeoServerTestApplicationContext(
                    contexts.toArray(new String[contexts.size()]), servletContext);
            applicationContext.setUseLegacyGeoServerLoader(false);
            applicationContext.refresh();
            applicationContext.publishEvent(new ContextLoadedEvent(applicationContext));

            // set the parameter after a refresh because it appears a refresh
            // wipes
            // out all parameters
            servletContext.setAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

            dispatcher = buildDispatcher();

            onSetUp(testData);
        }
    }

    protected final void tearDown(SystemTestData testData) throws Exception {
        if(testData.isTestDataAvailable()) {
            onTearDown(testData);

            destroyGeoServer();
            
            TestHttpClientProvider.endTest();

            // some tests do need a kick on the GC to fully clean up
            if(isMemoryCleanRequired()) {
                System.gc(); 
                System.runFinalization();
            }
        }
    }

    protected void destroyGeoServer() {
        if (applicationContext == null) {
            return;
        }

        try {
            //dispose WFS XSD schema's - they will otherwise keep geoserver instance alive forever!!
            disposeIfExists(getXSD11());
            disposeIfExists(getXSD10());
    
            // kill the context
            applicationContext.destroy();
            
            // kill static caches
            GeoServerExtensionsHelper.init(null);
        } finally {
            applicationContext = null;
        }
    }

    @After
    public void doLogout() {
        logout();
    }

    //
    // subclass hooks
    //
    /**
     * Sets up the {@link SystemTestData} used for this test.
     * <p>
     * This method is used to add any additional data or configuration to the test setup and may 
     * be overridden or extended. The default implementation calls 
     * {@link SystemTestData#setUpDefaultLayers()} to add the default layers for the test.  
     * </p>
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpDefault();
    }

    /**
     * Subclass hook called after the system (ie spring context) has been fully initialized.
     * <p>
     * Subclasses should override for post setup that is needed. The default implementation does 
     * nothing. 
     * </p>
     */
    protected void onSetUp(SystemTestData testData) throws Exception {
    }

    /**
     * Subclass hook called before the system (ie spring context) is to be shut down.
     * <p>
     * Subclasses should override for any cleanup / teardown that should occur on system shutdown. 
     * </p>
     */
    protected void onTearDown(SystemTestData testData) throws Exception {
    }

    /**
     * Sets up the spring context locations to use in constructing the spring context for this 
     * system test.
     * <p>
     * Subclasses may override to provide additional context files/locations.
     * </p>
     */
    protected void setUpSpring(List<String> springContextLocations) {
        springContextLocations.add("classpath*:/applicationContext.xml");
        springContextLocations.add("classpath*:/applicationSecurityContext.xml");
    }

    //
    // test behaviour methods
    //
    /**
     * Returns the logging configuration path. The default value is "/TEST_LOGGING.properties", which
     * is a pretty quiet configuration. Should you need more verbose logging override this method
     * in subclasses and choose a different configuration, for example "/DEFAULT_LOGGING.properties".
     * @return
     */
    protected String getLogConfiguration() {
        if (isQuietTests()) {
            return "/QUIET_LOGGING.properties";
        }
        return "/TEST_LOGGING.properties";
    }

    /**
     * Subclasses may override this method to force memory cleaning before the 
     * test data dir is cleaned up. This is necessary on windows if coverages are used in the
     * test, since readers might still be around in the heap as garbage without having
     * been disposed of
     * @return
     */
    protected boolean isMemoryCleanRequired() {
        return false;
    }

    //
    // singleton access methods
    //
    /**
     * Accessor for global catalog instance from the test application context.
     */
    protected Catalog getCatalog() {
        return (Catalog) applicationContext.getBean("catalog");
    }

    /**
     * Accessor for global geoserver instance from the test application context.
     */
    protected GeoServer getGeoServer() {
        return (GeoServer) applicationContext.getBean("geoServer");
    }

    /**
     * Accesssor for global security manager instance from the test application context.
     */
    protected GeoServerSecurityManager getSecurityManager() {
        return (GeoServerSecurityManager) applicationContext.getBean("geoServerSecurityManager");
    }

    /**
     * Accessor for plain text password encoder.
     */
    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }

    /**
     * Accessor for digest password encoder.
     */
    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    /**
     * Accessor for regular (weak encryption) pbe password encoder.
     */
    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    /**
     * Accessor for strong encryption pbe password encoder.
     */
    protected GeoServerPBEPasswordEncoder getStrongPBEPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true);
    }

    /**
     * Accessor for global resource loader instance from the test application context.
     */
    protected GeoServerResourceLoader getResourceLoader() {
        return (GeoServerResourceLoader) applicationContext.getBean( "resourceLoader" );
    }

    /**
     * Accessor for WFS 1.0 XSD from the test application context.
     */
    protected XSD getXSD11() {
        if (applicationContext.containsBean("wfsXsd-1.1")) {
            return (XSD) applicationContext.getBean("wfsXsd-1.1");
        } else {
            return null;
        }
    }
    
    /**
     * Accessor for WFS 1.0 XSD from the test application context.
     */
    protected XSD getXSD10() {
        if (applicationContext.containsBean("wfsXsd-1.0")) {
            return (XSD) applicationContext.getBean("wfsXsd-1.0"); 
        } else {
            return null;
        }
    }

    /**
     * Flush XSD if exists.
     */
    protected static void disposeIfExists(XSD xsd) {
        if (xsd != null) {
            xsd.dispose();
        }
    }

    //
    // lookup/accessor helper methods 
    //

    protected GeoServerDataDirectory getDataDirectory() {
        return new GeoServerDataDirectory(getResourceLoader());
    }

    /**
     * Loads a feature source from the catalog.
     *
     * @param typeName The qualified type name of the feature source.
     */
    @SuppressWarnings("unchecked")
    protected SimpleFeatureSource getFeatureSource(QName typeName)
            throws IOException {
        // TODO: expand test support to DataAccess FeatureSource
        FeatureTypeInfo ft = getFeatureTypeInfo(typeName);
        return DataUtilities.simple(ft.getFeatureSource(null, null));
    }

    /**
     * Get the FeatureTypeInfo for a featuretype to allow configuration tweaks for tests.
     *
     * @param typename the QName for the type
     */
    protected FeatureTypeInfo getFeatureTypeInfo(QName typename){
        return getCatalog().getFeatureTypeByName( typename.getNamespaceURI(), typename.getLocalPart() ); 
    }

    /**
     * Get the FeatureTypeInfo for a featuretype by the layername that would be used in a request.
     *
     * @param typename the layer name for the type
     */
    protected FeatureTypeInfo getFeatureTypeInfo(String typename){
        return getFeatureTypeInfo(resolveLayerName(typename));
    }

    /**
     * Given a qualified layer name returns a string in the form "prefix:localPart" if prefix
     * is available, "localPart" if prefix is null
     * @param layerName
     * @return
     */
    public String getLayerId(QName layerName) {
        return toString(layerName);
    }

    //
    // catalog state helpers
    //

    /**
     * Recursively deletes a workspace and everything under it.
     * <p>
     * If the workspace does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param name Name of the workspace.
     */
    protected void removeWorkspace(String name) {
        Catalog cat = getCatalog();

        WorkspaceInfo ws = cat.getWorkspaceByName(name);
        if (ws != null) {
            new CascadeDeleteVisitor(cat).visit(ws);
        }
    }

    /**
     * Deletes a namespace.
     * <p>
     * If the namespace does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * To do a recursive delete of all the resources under the namespace the 
     * {@link #removeWorkspace(String)} method shoul be used.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param prefix The prefix of the namespace.
     */
    protected void removeNamespace(String prefix) {
        Catalog cat = getCatalog();
        NamespaceInfo ns = cat.getNamespaceByPrefix(prefix);
        if (ns != null) {
            cat.remove(ns);
        }
    }

    /**
     * Recursively deletes a store and everything under it.
     * <p>
     * If the store does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspaceName Name of the workspace of the store.
     * @param name Name of the store.
     */
    protected void removeStore(String workspaceName, String name) {
        Catalog cat = getCatalog();
        StoreInfo store = cat.getStoreByName(workspaceName, name, StoreInfo.class);
        if (store == null) {
            return;
        }

        CascadeDeleteVisitor v = new CascadeDeleteVisitor(getCatalog());
        store.accept(v);
    }

    /**
     * Recursively deletes a layer and every resource associated with it.
     * <p>
     * If the layer does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspaceName Name of the workspace/namespace of the layer.
     * @param name Name of the layer.
     */
    protected void removeLayer(String workspaceName, String name) {
        Catalog cat = getCatalog();
        ResourceInfo resource = cat.getResourceByName(workspaceName, name, ResourceInfo.class);
        if (resource == null) {
            return;
        }
        CascadeDeleteVisitor v = new CascadeDeleteVisitor(getCatalog());
        for (LayerInfo layer : cat.getLayers()) {
            if(resource.equals(layer.getResource())) {
                layer.accept(v);
            }
        }
    }

    /**
     * Recursively deletes a style.
     * <p>
     * If the style does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspaceName The optional workspace of the style, may be <code>null</code>.
     * @param name Name of the style.
     */
    protected void removeStyle(String workspaceName, String name) throws IOException {
        Catalog cat = getCatalog();

        StyleInfo s = workspaceName != null ? 
            cat.getStyleByName(workspaceName, name) : cat.getStyleByName(name); 
        if (s != null) {
            cat.remove(s);

            //remove the sld file as well
            cat.getResourcePool().deleteStyle(s, true);
        }
        else {
            //handle case of sld fiel still lying around
            File sld = workspaceName != null ? 
                cat.getResourceLoader().find("workspaces", workspaceName, "styles", name + ".sld") : 
                cat.getResourceLoader().find("styles", name + ".sld");
            if (sld != null) {
                sld.delete();
            }
        }
    }

    /**
     * Deletes a layer group.
     * <p>
     * If the layer group does not exist, this method will do nothing rather than fail.
     * </p>
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspaceName The optional workspace of the layer group, may be <code>null</code>.
     * @param name Name of the layer group.
     */
    protected void removeLayerGroup(String workspaceName, String name) {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = workspaceName != null ? 
            cat.getLayerGroupByName(workspaceName, name) : cat.getLayerGroupByName(name); 
        if (lg != null) {
            cat.remove(lg);
        }
    }

    /**
     * Reverts a layer back to its original state, both data and configuration.
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspace The name of the workspace/namespace containing the layer.
     * @param name The name of the layer.
     * 
     */
    protected void revertLayer(String workspace, String name) throws IOException {
        revertLayer(new QName(workspace, name));
    }

    /**
     * Reverts a layer back to its original state, both data and configuration.
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param qName The qualified name of the layer.
     * 
     */
    protected void revertLayer(QName qName) throws IOException {
        getTestData().addVectorLayer(qName, getCatalog());
    }

    /**
     * Reverts a service back to its original configuration state.
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param serviceClass The class/interface of the service object.
     * @param workspace The optional workspace containing the service config, may be <code>null</code>.
     * 
     */
    protected void revertService(Class<? extends ServiceInfo> serviceClass, String workspace) {
        getTestData().addService(serviceClass, workspace, getGeoServer());
    }

    /**
     * Reverts settings back to original configuration state.
     * <p>
     * This method is intended to be called after system startup. Typically from 
     * {@link #onSetUp(SystemTestData)} or a {@literal @}Before hook.  
     * </p>
     * @param workspace The optional workspace containing the settings config, may be <code>null</code>.
     * 
     */
    protected void revertSettings(String workspace) {
        getTestData().addSettings(workspace, getGeoServer());
    }
    //
    // authentication/security helpers
    //
    /**
     * Sets the authentication for this test run (will be removed during {@link #tearDownInternal()}
     * ). Use a null user name to turn off authentication again.
     * <p>
     * Remember to override the getFilters() method so that Spring Security filters are enabled
     * during testing (otherwise no authentication will take place):
     * 
     * <pre>
     * protected List&lt;javax.servlet.Filter&gt; getFilters() {
     *     return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
     *             .bean(&quot;filterChainProxy&quot;));
     * }
     * </pre>
     * <p>
     * Also remember to add the users in the user.properties file, for example:
     * 
     * <pre>
     * protected void populateDataDirectory(MockData dataDirectory) throws Exception {
     *     super.populateDataDirectory(dataDirectory);
     *     File security = new File(dataDirectory.getDataDirectoryRoot(), &quot;security&quot;);
     *     security.mkdir();
     * 
     *     File users = new File(security, &quot;users.properties&quot;);
     *     Properties props = new Properties();
     *     props.put(&quot;admin&quot;, &quot;geoserver,ROLE_ADMINISTRATOR&quot;);
     *     props.store(new FileOutputStream(users), &quot;&quot;);
     * }
     * </pre>
     * 
     * @param username
     * @param password
     */
    protected void setRequestAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Sets up the authentication context for the test.
     * <p>
     * This context lasts only for a single test case, it is cleared after every test has completed. 
     * </p>
     * @param username The username.
     * @param password The password.
     * @param roles Roles to assign.
     */
    protected void login(String username, String password, String... roles) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        List<GrantedAuthority> l= new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            l.add(new GrantedAuthorityImpl(role));
        }

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username,password,l));
    }

    protected void addUser(String username, String password, List<String> groups, List<String> roles) throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        GeoServerUserGroupService ugService = secMgr.loadUserGroupService("default");

        GeoServerUserGroupStore ugStore = ugService.createStore();
        GeoServerUser user = ugStore.createUserObject(username, password, true);
        ugStore.addUser(user);

        if (groups != null && !groups.isEmpty()) {
            for (String groupName : groups) {
                GeoServerUserGroup group = ugStore.getGroupByGroupname(groupName);
                if (group == null) {
                    group = ugStore.createGroupObject(groupName, true);
                    ugStore.addGroup(group);
                }
    
                ugStore.associateUserToGroup(user, group);
            }
        }
        ugStore.store();

        if (roles != null && !roles.isEmpty()) {
            GeoServerRoleService roleService = secMgr.getActiveRoleService();
            GeoServerRoleStore roleStore = roleService.createStore();
            for (String roleName : roles) {
                GeoServerRole role = roleStore.getRoleByName(roleName);
                if (role == null) {
                    role = roleStore.createRoleObject(roleName);
                    roleStore.addRole(role);
                }

                roleStore.associateRoleToUser(role, username);
            }

            roleStore.store();
        }
    }

    protected void addLayerAccessRule(String workspace, String layer, AccessMode mode, String... roles) throws IOException {
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        DataAccessRule rule = new DataAccessRule();
        rule.setWorkspace(workspace);
        rule.setLayer(layer);
        rule.setAccessMode(mode);
        rule.getRoles().addAll(Arrays.asList(roles));
        dao.addRule(rule);
        dao.storeRules();
    }

    /**
     * Clears the authentication context.
     * <p>
     * This method is called after each test case 
     * </p>
     */
    protected void logout() {
        SecurityContextHolder.clearContext();
    }

    //
    // request/response helpers
    //
    /**
     * Convenience method for subclasses to create mock http servlet requests.
     * <p>
     * Examples of using this method are:
     * <pre>
     * <code>
     *   createRequest( "wfs?request=GetCapabilities" );  //get
     *   createRequest( "wfs" ); //post
     * </code>
     * </pre>
     * </p>
     * @param path The path for the request and optional the query string.
     * @return
     */
    protected MockHttpServletRequest createRequest(String path) {
        MockHttpServletRequest request = new GeoServerMockHttpServletRequest();

        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/geoserver");
        request.setRequestURI(ResponseUtils.stripQueryString(ResponseUtils.appendPath(
                    "/geoserver/", path)));
        request.setRequestURL(ResponseUtils.appendPath("http://localhost:8080/geoserver", path ) );
        request.setQueryString(ResponseUtils.getQueryString(path));
        request.setRemoteAddr("127.0.0.1");
        request.setServletPath(ResponseUtils.makePathAbsolute( ResponseUtils.stripRemainingPath(path)) );
        request.setPathInfo(ResponseUtils.makePathAbsolute( ResponseUtils.stripBeginningPath( ResponseUtils.stripQueryString(path))));
        request.setHeader("Host", "localhost:8080");
        
        // deal with authentication
        if(username != null) {
            String token = username + ":";
            if(password != null) {
                token += password;
            }
            request.addHeader("Authorization",  "Basic " + new String(Base64.encodeBase64(token.getBytes())));
        }
        
        
        kvp(request, path);

        MockHttpSession session = new MockHttpSession();
        session.setupServletContext(new MockServletContext());
        request.setSession(session);

        request.setUserPrincipal(null);

        return request;
    }

    /**
     * Convenience method for subclasses to create mock http servlet requests.
     * <p>
     * Examples of using this method are:
     * <pre>
     * <code>
     *   Map kvp = new HashMap();
     *   kvp.put( "service", "wfs" );
     *   kvp.put( "request", "GetCapabilities" );
     *   
     *   createRequest( "wfs", kvp );
     * </code>
     * </pre>
     * </p>
     * @param path The path for the request, minus any query string parameters.
     * @param kvp The key value pairs to be put in teh query string. 
     * 
     */
    protected MockHttpServletRequest createRequest( String path, Map kvp ) {
        StringBuffer q = new StringBuffer();
        for ( Iterator e = kvp.entrySet().iterator(); e.hasNext(); ) {
            Map.Entry entry = (Map.Entry) e.next();
            q.append( entry.getKey() ).append("=").append( entry.getValue() );
            q.append( "&" );
        }
        q.setLength(q.length()-1);
        
        return createRequest( ResponseUtils.appendQueryString(path, q.toString() ) );
    }
    
    /**
     * Executes an ows request using the GET method.
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected InputStream get( String path ) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        return new ByteArrayInputStream( response.getOutputStreamContent().getBytes() );
    }
    
    /**
     * Executes an ows request using the GET method.
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * 
     * @return the mock servlet response
     * 
     * @throws Exception
     */
    protected MockHttpServletResponse getAsServletResponse( String path ) throws Exception {
        return getAsServletResponse(path, null);
    }
    
    /**
     * Executes an ows request using the GET method.
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * @param charset The character set of the response.
     * 
     * @return the mock servlet response
     */
    protected MockHttpServletResponse getAsServletResponse( String path, String charset ) throws Exception {
        MockHttpServletRequest request = createRequest( path ); 
        request.setMethod( "GET" );
        request.setBodyContent(new byte[]{});
        
        return dispatch( request, charset );
    }
        
    /**
     * Executes an ows request using the POST method with key value pairs 
     * form encoded. 
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected InputStream post( String path ) throws Exception {
        MockHttpServletRequest request = createRequest( path ); 
        request.setMethod( "POST" );
        request.setContentType( "application/x-www-form-urlencoded" );
        request.setBodyContent(new byte[]{});
        
        MockHttpServletResponse response = dispatch( request );
        return new ByteArrayInputStream( response.getOutputStreamContent().getBytes() );
    }

    /**
     * Executes a request with an empty body using the PUT method.
     *
     * @param path the portion of the request after the context, for example:
     *      "api/datastores.xml"
     *
     * @throws Exception
     */
    protected InputStream put(String path) throws Exception{
        return put(path, "");
    }

    /**
     * Executes a request with a default mimetype using the PUT method.
     *
     * @param path the portion of the request after the context, for example:
     *      "api/datastores.xml"
     * @param body the content to send as the body of the request
     *
     * @throws Exception
     */
    protected InputStream put(String path, String body) throws Exception{
        return put(path, body, "text/plain");
    }

    /**
     * Executes a request using the PUT method.
     *
     * @param path the portion of the request after the context, for example:
     *      "api/datastores.xml"
     * @param body the content to send as the body of the request
     * @param contentType the mime-type to set for the request being sent
     *
     * @throws Exception
     */
    protected InputStream put(String path, String body, String contentType) throws Exception {
        return put( path, body.getBytes(), contentType );
    }

    /**
     * Executes a request using the PUT method.
     *
     * @param path the portion of the request after the context, for example:
     *      "api/datastores.xml"
     * @param body the content to send as the body of the request
     * @param contentType the mime-type to set for the request being sent
     *
     * @throws Exception
     */
    protected InputStream put(String path, byte[] body, String contentType) throws Exception {
        MockHttpServletResponse response = putAsServletResponse(path, body, contentType);
        return new ByteArrayInputStream(response.getOutputStreamContent().getBytes());
    }
    
    protected MockHttpServletResponse putAsServletResponse(String path) throws Exception {
        return putAsServletResponse(path, new byte[]{}, "text/plain");
    }
    
    protected MockHttpServletResponse putAsServletResponse(String path, String body, String contentType ) 
    throws Exception {
        return putAsServletResponse(path, body != null ? body.getBytes() : (byte[]) null, contentType);
    }
    
    protected MockHttpServletResponse putAsServletResponse(String path, byte[] body, String contentType ) 
        throws Exception {
        
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("PUT");
        request.setContentType(contentType);
        request.setBodyContent(body);
        request.setHeader( "Content-type", contentType );

        return dispatch(request);
    }
    
    /**
     * Executes an ows request using the POST method.
     * <p>
     * 
     * </p>
     * @param path The porition of the request after the context ( no query string ), 
     *      example: 'wms'. 
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected InputStream post( String path , String xml ) throws Exception {
        MockHttpServletResponse response = postAsServletResponse(path, xml);
        return new ByteArrayInputStream(response.getOutputStreamContent().getBytes());
    }

    /**
     * Executes an ows request using the POST method, with xml as body content. 
     * 
     * 
     * @param path
     *            The porition of the request after the context ( no query
     *            string ), example: 'wms'.
     * @param xml The body content.
     * 
     * @return the servlet response
     * 
     * @throws Exception
     */
    protected MockHttpServletResponse postAsServletResponse(String path, String xml)
            throws Exception {
        
        return postAsServletResponse(path, xml, "application/xml");
    }

    /**
     * Extracts the true binary stream out of the response. The usual way (going
     * thru {@link MockHttpServletResponse#getOutputStreamContent()}) mangles
     * bytes if the content is not made of chars.
     * 
     * @param response
     * @return
     */
    protected ByteArrayInputStream getBinaryInputStream(MockHttpServletResponse response) {
        return new ByteArrayInputStream(getBinary(response));
    }
    
    /**
     * Extracts the true binary stream out of the response. The usual way (going
     * thru {@link MockHttpServletResponse#getOutputStreamContent()}) mangles
     * bytes if the content is not made of chars.
     * 
     * @param response
     * @return
     */
    protected byte[] getBinary(MockHttpServletResponse response) {
        try {
            MockServletOutputStream os = (MockServletOutputStream) response.getOutputStream();
            final Field field = os.getClass().getDeclaredField("buffer");
            field.setAccessible(true);
            ByteArrayOutputStream bos = (ByteArrayOutputStream) field.get(os);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Whoops, did you change the MockRunner version? "
                    + "If so, you might want to change this method too");
        }
    }
            
            
    /**
     * Executes an ows request using the POST method.
     * 
     * @param path
     *            The porition of the request after the context ( no query
     *            string ), example: 'wms'.
     * 
     * @param body
     *            the body of the request
     * @param contentType
     *            the mimetype to set for the request
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected InputStream post(String path, String body, String contentType) throws Exception{
        MockHttpServletResponse response = postAsServletResponse(path, body, contentType);
        return new ByteArrayInputStream(response.getOutputStreamContent().getBytes());
    }
    
    protected MockHttpServletResponse postAsServletResponse(String path, String body, String contentType) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(contentType);
        request.setBodyContent(body);
        request.setHeader("Content-type",  contentType );

        return dispatch(request);
    }
    
    protected MockHttpServletResponse postAsServletResponse(String path, String body, String contentType, String charset) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(contentType);
        request.setBodyContent(body);
        request.setHeader("Content-type",  contentType );
        return dispatch(request, charset);
    }

    protected MockHttpServletResponse postAsServletResponse(String path, byte[] body, String contentType )
            throws Exception {

        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType(contentType);
        request.setBodyContent(body);
        request.setHeader( "Content-type", contentType );

        return dispatch(request);
    }

    /**
     * Execultes a request using the DELETE method.
     * 
     * @param path The path of the request.
     * 
     * @return The http status code.
     */
    protected MockHttpServletResponse deleteAsServletResponse(String path) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("DELETE");
        
        return dispatch(request);
    }
    
    /**
     * Executes an ows request using the GET method and returns the result as an 
     * xml document.
     * 
     * @param path The portion of the request after the context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * @param the list of validation errors encountered during document parsing (validation
     *        will be activated only if this list is non null)
     * 
     * @return A result of the request parsed into a dom.
     * 
     * @throws Exception
     */
    protected Document getAsDOM(final String path)
            throws Exception {
        return getAsDOM(path, true);
    }

    /**
     * Executes an ows request using the GET method and returns the result as an 
     * xml document, with the ability to override the XML document encoding. 
     * 
     * @param path The portion of the request after the context, 
     *   example: 'wms?request=GetMap&version=1.1.1&..."
     * @param encoding Override for the encoding of the document.
     * 
     * @return A result of the request parsed into a dom.
     * 
     * @throws Exception
     */
    protected Document getAsDOM(final String path, String encoding) throws Exception {
        return getAsDOM(path, true, encoding);
    }
    /**
     * Executes a request using the GET method and parses the result as a json object.
     * 
     * @param path The path to request.
     *  
     * @return The result parsed as json.
     */
    protected JSON getAsJSON(final String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        return json(response);
    }
    
    protected JSON json(MockHttpServletResponse response) {
        String content = response.getOutputStreamContent();
        return JSONSerializer.toJSON(content);
    }
    
    /**
     * Retries the request result as a BufferedImage, checking the mime type is the expected one
     * @param path
     * @param mime
     * @return
     * @throws Exception
     */
    protected BufferedImage getAsImage(String path, String mime) throws Exception {
        MockHttpServletResponse resp = getAsServletResponse(path);
        assertEquals(mime, resp.getContentType());
        InputStream is = getBinaryInputStream(resp);
        return ImageIO.read(is);
    }

    /**
     * Executes an ows request using the GET method and returns the result as an xml document.
     * 
     * @param path
     *                The portion of the request after the context, example:
     *                'wms?request=GetMap&version=1.1.1&..."
     * @param skipDTD
     *                if true, will avoid loading and validating against the response document
     *                schema or DTD
     * 
     * @return A result of the request parsed into a dom.
     * 
     * @throws Exception
     */
    protected Document getAsDOM(final String path, final boolean skipDTD)
    throws Exception {
        return dom(get(path), skipDTD);
    }

    /**
     * Executes an ows request using the GET method and returns the result as an xml document.
     * 
     * @param path
     *                The portion of the request after the context, example:
     *                'wms?request=GetMap&version=1.1.1&..."
     * @param skipDTD
     *                if true, will avoid loading and validating against the response document
     *                schema or DTD
     *
     * @param encoding 
     *                Overide for the encoding of the document.
     * 
     * @return A result of the request parsed into a dom.
     * 
     * @throws Exception
     */
    protected Document getAsDOM(final String path, final boolean skipDTD, String encoding)
            throws Exception {
        return dom(get(path), skipDTD, encoding);
    }

    /**
     * Executes an ows request using the POST method with key value pairs 
     * form encoded, returning the result as a dom.
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * @param the list of validation errors encountered during document parsing (validation
     *        will be activated only if this list is non null)     
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected Document postAsDOM( String path ) throws Exception {
        return postAsDOM(path, (List<Exception>) null);
    }
    
    /**
     * Executes an ows request using the POST method with key value pairs 
     * form encoded, returning the result as a dom.
     *
     * @param path The porition of the request after hte context, 
     *      example: 'wms?request=GetMap&version=1.1.1&..."
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected Document postAsDOM( String path, List<Exception> validationErrors ) throws Exception {
        return dom( post( path ));
    }
    
    /**
     * Executes an ows request using the POST method and returns the result as an
     * xml document.
     * <p>
     * 
     * </p>
     * @param path The porition of the request after the context ( no query string ), 
     *      example: 'wms'. 
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected Document postAsDOM( String path, String xml ) throws Exception {
        return postAsDOM(path, xml, null);
    }
    
    /**
     * Executes an ows request using the POST method and returns the result as an
     * xml document.
     * <p>
     * 
     * </p>
     * @param path The porition of the request after the context ( no query string ), 
     *      example: 'wms'. 
     * 
     * @return An input stream which is the result of the request.
     * 
     * @throws Exception
     */
    protected Document postAsDOM( String path, String xml, List<Exception> validationErrors ) throws Exception {
        return dom(post( path, xml ));
    }
    
    protected String getAsString(String path) throws Exception {
        return string(get(path));
    }
    
    /**
     * Parses a stream into a dom.
     */
    protected Document dom(InputStream is) throws ParserConfigurationException, SAXException, IOException {
        return dom(is, true);
    }
    
    /**
     * Parses a stream into a dom.
     * @param input
     * @param skipDTD If true, will skip loading and validating against the associated DTD
     */
    protected Document dom(InputStream input, boolean skipDTD) throws ParserConfigurationException, SAXException, IOException {
        return dom(input, skipDTD, null);
    }

    protected Document dom(InputStream stream, boolean skipDTD, String encoding) 
        throws ParserConfigurationException, SAXException, IOException {

        InputSource input = new InputSource(stream);
        if (encoding != null) {
            input.setEncoding(encoding);
        }

        if(skipDTD) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            
            factory.setNamespaceAware( true );
            factory.setValidating( false );
           
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(new EmptyResolver());
            Document dom = builder.parse( input );
    
            return dom;
        } else {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(input);
        }
    }

    protected MockHttpServletResponse dispatch( HttpServletRequest request ) throws Exception {
        return dispatch(request, (String) null);
    }
    
    protected MockHttpServletResponse dispatch( HttpServletRequest request, String charset ) 
        throws Exception {
        MockHttpServletResponse response = null;
        if (charset == null) {
            response = new MockHttpServletResponse() {
                public void setCharacterEncoding( String encoding ) {
                        
                }
            };
        }
        else {
            response = new MockHttpServletResponse();
            response.setCharacterEncoding(charset);
        }

        dispatch(request, response);
        return response;
    } 
    
    protected DispatcherServlet getDispatcher() throws Exception {
        return dispatcher;
    }

    protected DispatcherServlet buildDispatcher() throws ServletException {
        // create an instance of the spring dispatcher
        ServletContext context = applicationContext.getServletContext();

        MockServletConfig config = new MockServletConfig();
        config.setServletContext(context);
        config.setServletName("dispatcher");

        DispatcherServlet dispatcher = new DispatcherServlet(applicationContext);

        dispatcher.setContextConfigLocation(GeoServerAbstractTestSupport.class.getResource(
                "dispatcher-servlet.xml").toString());
        dispatcher.init(config);

        return dispatcher;
    }
 
    private void dispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final DispatcherServlet dispatcher = getDispatcher();
        
        // build a filter chain so that we can test with filters as well
        MockFilterChain chain = new MockFilterChain();
        List<Filter> filters = getFilters();
        if(filters != null) {
            for (Filter filter : filters) {
                chain.addFilter(filter);
            }
        }
        chain.setServlet(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                try {
                    //excute the pre handler step
                    Collection interceptors = 
                        GeoServerExtensions.extensions(HandlerInterceptor.class, applicationContext );
                    for ( Iterator i = interceptors.iterator(); i.hasNext(); ) {
                        HandlerInterceptor interceptor = (HandlerInterceptor) i.next();
                        interceptor.preHandle( request, response, dispatcher );
                    }
                    
                    //execute 
                    //dispatcher.handleRequest( request, response );
                    dispatcher.service(request, response);
                    
                    //execute the post handler step
                    for ( Iterator i = interceptors.iterator(); i.hasNext(); ) {
                        HandlerInterceptor interceptor = (HandlerInterceptor) i.next();
                        interceptor.postHandle( request, response, dispatcher, null );
                    }
                } catch(RuntimeException e) {
                    throw e;
                } catch(IOException e) {
                    throw e;
                } catch(ServletException e) {
                    throw e;
                } catch(Exception e) {
                    throw (IOException) new IOException("Failed to handle the request").initCause(e);
                }
            }
        });
        
        chain.doFilter(request, response);
        
    }

    /*
     * Helper method to create the kvp params from the query string.
     */
    private void kvp(MockHttpServletRequest request, String path) {
         Map<String, Object> params = KvpUtils.parseQueryString(path);
         for (String key : params.keySet()) {
            Object value = params.get(key);
            if(value instanceof String) {
                request.setupAddParameter(key, (String) value);
            } else {
                String[] values = (String[]) value;
                request.setupAddParameter(key, values);
            }
        }
         
    }

    /**
     * Assert that a GET request to a path will have a particular status code for the response.
     * @param code the number of the HTTP status code that is expected
     * @param path the path to which a GET request should be made, without the protocol, server and servlet context.
     * For example, to make a request to "http://localhost:8080/geoserver/ows" the path would be "ows"
     *
     * @throws Exception on test failure
     */
    protected void assertStatusCodeForGet(int code, String path) throws Exception{
        assertStatusCodeForRequest(code, "GET", path, "", "");
    }

    /**
     * Assert that a POST request to a path will have a particular status code for the response.
     * @param code the number of the HTTP status code that is expected
     * @param path the path to which a POST request should be made, without the protocol, server and servlet context.
     * For example, to make a request to "http://localhost:8080/geoserver/ows" the path would be "ows"
     * @param body the body to send with the request. May be empty, but must not be null.
     * @param type the mimetype to report for the body
     *
     * @throws Exception on test failure
     */
    protected void assertStatusCodeForPost(int code, String path, String body, String type) throws Exception {
        assertStatusCodeForRequest(code, "POST", path, body, type);
    }

    /**
     * Assert that a PUT request to a path will have a particular status code for the response.
     * @param code the number of the HTTP status code that is expected
     * @param path the path to which a PUT request should be made, without the protocol, server and servlet context.
     * For example, to make a request to "http://localhost:8080/geoserver/ows" the path would be "ows"
     * @param body the body to send with the request. May be empty, but must not be null.
     * @param type the mimetype to report for the body
     *
     * @throws Exception on test failure
     */
    protected void assertStatusCodeForPut(int code, String path, String body, String type) throws Exception {
        assertStatusCodeForRequest(code, "PUT", path, body, type);
    }

    /**
     * Assert that an HTTP request will have a particular status code for the response.
     * @param code the number of the HTTP status code that is expected
     * @param method the HTTP method for the request (eg, GET, PUT)
     * @param path the path for the request, excluding the protocol, server, port, and servlet context.
     * For example, to make a request to "http://localhost:8080/geoserver/ows" the path would be "ows"
     * @param body the body for the request.  May be empty, but must not be null.
     * @param type the mimetype for the request.
     */
    protected void assertStatusCodeForRequest(int code, String method, String path, String body, String type) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod(method);
        request.setBodyContent(body);
        request.setContentType(type);

        CodeExpectingHttpServletResponse response = new CodeExpectingHttpServletResponse(new MockHttpServletResponse());
        dispatch(request, response);
        assertEquals(code, response.getErrorCode());
    }

    //
    // xml validation helpers
    //
    /**
     * Resolves everything to an empty xml document, useful for skipping errors due to missing
     * dtds and the like
     * @author Andrea Aime - TOPP
     */
    static class EmptyResolver implements org.xml.sax.EntityResolver {
        public InputSource resolveEntity(String publicId, String systemId)
                throws org.xml.sax.SAXException, IOException {
            StringReader reader = new StringReader(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            InputSource source = new InputSource(reader);
            source.setPublicId(publicId);
            source.setSystemId(systemId);

            return source;
        }
    }
            
    protected void checkValidationErorrs(Document dom, String schemaLocation) throws SAXException, IOException {
        final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = factory.newSchema(new File(schemaLocation));
        checkValidationErrors(dom, schema);
    }

    /**
     * Given a dom and a schema, checks that the dom validates against the schema 
     * of the validation errors instead
     * @param validationErrors
     * @throws IOException 
     * @throws SAXException 
     */
    protected void checkValidationErrors(Document dom, Schema schema) throws SAXException, IOException {
        final Validator validator = schema.newValidator();
        final List<Exception> validationErrors = new ArrayList<Exception>();
        validator.setErrorHandler(new ErrorHandler() {
            
            public void warning(SAXParseException exception) throws SAXException {
                System.out.println(exception.getMessage());
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                validationErrors.add(exception);
            }

            public void error(SAXParseException exception) throws SAXException {
                validationErrors.add(exception);
            }

          });
        validator.validate(new DOMSource(dom));
        if (validationErrors != null && validationErrors.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Exception ve : validationErrors) {
                sb.append(ve.getMessage()).append("\n");
            }
            fail(sb.toString());
        }
    }
    
    /**
     * Performs basic checks on an OWS 1.0 exception, to ensure it's well formed
     */
    protected void checkOws10Exception(Document dom) throws Exception {
        checkOws10Exception(dom,null, null);
    }
    
    /**
     * Performs basic checks on an OWS 1.0 exception, to ensure it's well formed
     * and ensuring that a particular exceptionCode is used.
     */
    protected void checkOws10Exception(Document dom, String exceptionCode) throws Exception {
        checkOws10Exception(dom, exceptionCode, null);
    }
    
    /**
     * Performs basic checks on an OWS 1.0 exception, to ensure it's well formed
     * and ensuring that a particular exceptionCode is used.
     */
    protected void checkOws10Exception(Document dom, String exceptionCode, String locator) throws Exception {
        Element root = dom.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName() );
        assertEquals( "1.0.0", root.getAttribute( "version") );
        assertEquals("http://www.opengis.net/ows", root.getAttribute( "xmlns:ows"));
        assertEquals( 1, dom.getElementsByTagName( "ows:Exception").getLength() );
        
        Element ex = (Element) dom.getElementsByTagName( "ows:Exception").item(0);
        if ( exceptionCode != null ) {
            assertEquals( exceptionCode, ex.getAttribute( "exceptionCode") );
        }
        if(locator != null) {
            assertEquals( locator, ex.getAttribute( "locator") );
        }
    }
    
    /**
     * Performs basic checks on an OWS 1.1 exception, to ensure it's well formed
     */
    protected void checkOws11Exception(Document dom) throws Exception {
        checkOws11Exception(dom,null);
    }
    /**
     * Performs basic checks on an OWS 1.1 exception, to ensure it's well formed
     * and ensuring that a particular exceptionCode is used.
     */
    protected void checkOws11Exception(Document dom, String exceptionCode) throws Exception {
        checkOws11Exception(dom, exceptionCode, null);
    }
    
    /**
     * Performs basic checks on an OWS 1.1 exception, to ensure it's well formed
     * and ensuring that a particular exceptionCode is used.
     */
    protected void checkOws11Exception(Document dom, String exceptionCode, String locator) throws Exception {
        Element root = dom.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName() );
        assertEquals( "1.1.0", root.getAttribute( "version") );
        assertEquals("http://www.opengis.net/ows/1.1", root.getAttribute( "xmlns:ows"));
        
        if ( exceptionCode != null ) {
            assertEquals( 1, dom.getElementsByTagName( "ows:Exception").getLength() );
            Element ex = (Element) dom.getElementsByTagName( "ows:Exception").item(0);
            assertEquals( exceptionCode, ex.getAttribute( "exceptionCode") );
        }
        
        if( locator != null)  {
            assertEquals( 1, dom.getElementsByTagName( "ows:Exception").getLength() );
            Element ex = (Element) dom.getElementsByTagName( "ows:Exception").item(0);
            assertEquals( locator, ex.getAttribute( "locator") );
        }
    }

    
    /**
     * Performs basic checks on an OWS 2.0 exception. The check for status, exception code and locator
     * is optional, leave null if you don't want to check it. 
     * @returns Returns the message of the inner exception.
     */
    protected String checkOws20Exception(MockHttpServletResponse response, Integer status,
            String exceptionCode, String locator) throws Exception {
        // check the http level
        assertEquals("application/xml", response.getContentType());
        if (status != null) {
            assertEquals(status.intValue(), response.getStatusCode());
        }

        // check the returned xml
        Document dom = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));
        Element root = dom.getDocumentElement();
        assertEquals("ows:ExceptionReport", root.getNodeName());
        assertEquals("2.0.0", root.getAttribute("version"));
        assertEquals("http://www.opengis.net/ows/2.0", root.getAttribute("xmlns:ows"));

        // look into exception code and locator
        assertEquals(1, dom.getElementsByTagName("ows:Exception").getLength());
        Element ex = (Element) dom.getElementsByTagName("ows:Exception").item(0);
        if (exceptionCode != null) {
            assertEquals(exceptionCode, ex.getAttribute("exceptionCode"));
        }
        if (locator != null) {
            assertEquals(locator, ex.getAttribute("locator"));
        }

        assertEquals(1, dom.getElementsByTagName("ows:ExceptionText").getLength());
        return dom.getElementsByTagName("ows:ExceptionText").item(0).getTextContent();
    }

    //
    // misc utilities
    //
    /**
     * Reloads the catalog and configuration from disk.
     * <p>
     * This method can be used by subclasses from a test method after they have
     * changed the configuration on disk.
     * </p>
     */
    protected void reloadCatalogAndConfiguration() throws Exception {
        GeoServerLoaderProxy loader = GeoServerExtensions.bean( GeoServerLoaderProxy.class , applicationContext );
        loader.reload();
    }

    /**
     * Get the QName for a layer specified by the layername that would be used in a request.
     * @param typename the layer name for the type
     */
    protected QName resolveLayerName(String typename){
        int i = typename.indexOf(":");
        String prefix = typename.substring(0, i);
        String name = typename.substring(i + 1);
        NamespaceInfo ns = getCatalog().getNamespaceByPrefix(prefix);
        QName qname = new QName(ns.getURI(), name, ns.getPrefix());
        return qname;
    }

    /**
     * Parses a stream into a String
     */
    protected String string(InputStream input) throws Exception {
        BufferedReader reader = null;
        StringBuffer sb = new StringBuffer();
        char[] buf = new char[8192];
        try {
            reader = new BufferedReader(new InputStreamReader(input));
            String line = null;
            while((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } finally {
            if(reader != null)
                reader.close();
        }
        return sb.toString();
    }
    
    /**
     * Utility method to print out a dom.
     */
    protected void print( Document dom ) throws Exception {
        if (isQuietTests()) {
            return;
        }
        print (dom, System.out);
    }

    /**
     * Pretty-print a {@link Document} to an {@link OutputStream}.
     * 
     * @param document
     *            document to be prettified
     * @param output
     *            stream to which output is written
     */
    protected void print(Document document, OutputStream output) {
        try {
            Transformer tx = TransformerFactory.newInstance().newTransformer();
            tx.setOutputProperty(OutputKeys.INDENT, "yes");
            tx.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            tx.transform(new DOMSource(document), new StreamResult(output));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility method to print out the contents of an input stream.
     */
    protected void print( InputStream in ) throws Exception {
        if (isQuietTests()) {
            return;
        }
        BufferedReader r = new BufferedReader( new InputStreamReader( in ) );
        String line = null;
        while( (line = r.readLine()) != null ) {
            System.out.println( line );
        }
    }
    
    /**
     * Utility method to print out the contents of a json object.
     */
    protected void print( JSON json ) {
        if (isQuietTests()) {
            return;
        }
        System.out.println(json.toString(2));
    }
    
    /**
     * Convenience method for element.getElementsByTagName() to return the 
     * first element in the resulting node list.
     */
    protected Element getFirstElementByTagName( Element element, String name ) {
        NodeList elements = element.getElementsByTagName(name);
        if ( elements.getLength() > 0 ) {
            return (Element) elements.item(0);
        }
        
        return null;
    }
    
    /**
     * Convenience method for element.getElementsByTagName() to return the 
     * first element in the resulting node list.
     */
    protected Element getFirstElementByTagName( Document dom, String name ) {
        return getFirstElementByTagName( dom.getDocumentElement(), name );
    }

    /**
     * Subclasses needed to do integration tests with servlet filters can override this method
     * and return the list of filters to be used during mocked requests
     * @return
     */
    protected List<Filter> getFilters() {
        return null;
    }
    
    /**
     * Parses a raw set of kvp's into a parsed set of kvps.
     *
     * @param kvp Map of String,String.
     */
    protected Map parseKvp(Map /*<String,String>*/ raw)
        throws Exception {
        
        // parse like the dispatcher but make sure we don't change the original map
        HashMap input = new HashMap(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if(errors != null && errors.size() > 0)
            throw (Exception) errors.get(0);
        
        return caseInsensitiveKvp(input);
    }

    protected Map caseInsensitiveKvp(HashMap input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map result = new HashMap();
        for (Iterator it = input.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap(result);
    }

 
    /**
     * Checks the image and its sources are all deferred loaded, that is, there is no BufferedImage in the chain
     * @param image
     */
    protected void assertDeferredLoading(RenderedImage image) {
        if(image instanceof BufferedImage) {
            fail("Found a buffered image in the chain, the original image is not fully deferred loaded");
        } else {
            for (RenderedImage ri : image.getSources()) {
                assertDeferredLoading(ri);
            }
        }
    }

    public static class GeoServerMockHttpServletRequest extends MockHttpServletRequest {
        private byte[] myBody;
        
        @Override
        public void setBodyContent(byte[] body) {
            myBody = body;
        }
        
        @Override
        public void setBodyContent(String body) {
            myBody = body.getBytes();
        }
        
        
        
        @Override
        public BufferedReader getReader() throws IOException {
            if (null == myBody)
                return null;
            return new BufferedReader(new StringReader(new String(myBody)));
        }
        
        public ServletInputStream getInputStream() {
            return new GeoServerMockServletInputStream(myBody);
        }
    }

    private static class GeoServerMockServletInputStream extends ServletInputStream {
        private byte[] myBody;
        private int myOffset = 0;
        private int myMark = -1;

        public GeoServerMockServletInputStream(byte[] body){
            myBody = body;
        }
        
        public int available() {
            return myBody.length - myOffset;
        }

        public void close(){}

        public void mark(int readLimit){
            myMark = myOffset;
        }
        
        public void reset() {
            if (myMark < 0 || myMark >= myBody.length){
                throw new IllegalStateException("Can't reset when no mark was set.");
            }
            
            myOffset = myMark;
        }

        public boolean markSupported(){ return true; }

            public int read(){
                byte[] b = new byte[1];
                return read(b, 0, 1) == -1 ? -1 : b[0];
            }

        public int read(byte[] b){
            return read(b, 0, b.length);
        }

        public int read(byte[] b, int offset, int length){
            int realOffset = offset + myOffset;
            int i;

            if ( realOffset >= myBody.length ) {
                return -1;
            }
            for (i = 0; (i < length) && (i + myOffset < myBody.length); i++){
                b[offset + i] = myBody[myOffset + i];
            }

            myOffset += i;

            return i;
        }

        public int readLine(byte[] b, int offset, int length){
            int realOffset = offset + myOffset;
            int i;

            for (i = 0; (i < length) && (i + myOffset < myBody.length); i++){
                b[offset + i] = myBody[myOffset + i];
                if (myBody[myOffset + i] == '\n') break;
            }

            myOffset += i;

            return i;
        }
    }
}
