/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.data.test.LiveSystemTestData;
import org.geotools.util.logging.Logging;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

/**
 * Extends LiveData to deal with a CAS server (central authentication server)
 * @author christian
 * 
 */
public class LiveCasData extends LiveSystemTestData {
    private static final Logger LOGGER = Logging.getLogger(LiveCasData.class);
    private static final String CAS_SERVER_PROPERTY = "casserverurlprefix";
    private static final String CAS_SERVICE_PROPERTY = "service";
    private static final String CAS_PROXYCALLBACK_PROPERTY = "proxycallbackurlprefix";

    /**
     * The property file containing the token -> value pairs used to get
     * a CAS server Url
     * 
     * @return
     */
    protected File fixture;
    protected URL serverURLPrefix, serviceURL,loginURL, proxyCallbackURLPrefix;    
    protected File keyStoreFile; 
    protected X509TrustManager trustManager;
    


    /**
     * List of file paths (relative to the source data directory) that will be
     * subjected to token filtering. By default only <code>catalog.xml</code>
     * will be filtered.
     */

    
    public URL getServerURLPrefix() {
        return serverURLPrefix;
    }

    public URL getLoginURL() {
        return loginURL;
    }

    public void setLoginURL(URL loginURL) {
        this.loginURL = loginURL;
    }

    public URL getServiceURL() {
        return serviceURL;
    }

    public URL getProxyCallbackURLPrefix() {
        return proxyCallbackURLPrefix;
    }

    public void setProxyCallbackURLPrefix(URL proxyCallbackURLPrefix) {
        this.proxyCallbackURLPrefix = proxyCallbackURLPrefix;
    }

    /**
     * constant fixture id
     */
    protected String fixtureId="cas";

    public LiveCasData(File dataDirSourceDirectory ) throws IOException {
        super(dataDirSourceDirectory);
        this.fixture = lookupFixture(fixtureId);
    }
    
    public File getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(File keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }


    /**
     * Looks up the fixture file in the home directory provided that the 
     * @param fixtureId
     * @return
     */
    private File lookupFixture(String fixtureId) {
        // first of all, make sure the fixture was not disabled using a system
        // variable
        final String property = System.getProperty("gs." + fixtureId);
        if (property != null && "false".equals(property.toLowerCase())) {
            return null;
        }

        // then look in the user home directory
        File base = new File(System.getProperty("user.home"), ".geoserver");
        // create the hidden folder, this is handy especially on windows where
        // a user cannot create a directory starting with . from the UI 
        // (works only from the command line)
        if(!base.exists())
            base.mkdir();
        File fixtureFile = new File(base, fixtureId + ".properties");
        if (!fixtureFile.exists()) {
            final String warning = "Disabling test based on fixture " + fixtureId + " since the file "
                    + fixtureFile + " could not be found";
            disableTest(warning);
            return null;
        }
                
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(fixtureFile));
            String tmp = props.getProperty(CAS_SERVER_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            serverURLPrefix=new URL(tmp);
            loginURL=new URL(tmp+"/login");
            
            tmp = props.getProperty(CAS_SERVICE_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            serviceURL=new URL(tmp);
            
            tmp = props.getProperty(CAS_PROXYCALLBACK_PROPERTY);
            if (tmp==null) tmp=""; // avoid NPE
            proxyCallbackURLPrefix=new URL(tmp);

            
        } catch (Exception e) {
            disableTest("Error in fixture file: "+e.getMessage());
            return null;
        }
                
        // check connection
        try {
            HttpURLConnection huc =  (HttpURLConnection)  loginURL.openConnection(); 
            huc.setRequestMethod("GET"); 
            huc.connect(); 
            if (huc.getResponseCode()!=HttpServletResponse.SC_OK) {
                disableTest("Cannot connect to "+loginURL.toString());
                return null;
            }
        } catch (Exception ex) {
            disableTest("problem with cas connection: "+ex.getMessage());
            return null;
        }

        keyStoreFile = new File(base,"keystore.jks");
        if (keyStoreFile.exists()==false) {
            disableTest("Keystore not found: "+ keyStoreFile.getAbsolutePath());
            return null;
        }


        trustManager = new  X509TrustManager() {
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                //return new java.security.cert.X509Certificate[0];
                return null;
            }
            
            @Override
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
            
            @Override
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
            }
        }; 

//        HttpsServer httpsServer=null;
//        try {
//            httpsServer=createSSLServer();
//            httpsServer.start();
//            checkSSLServer();
//        } catch (Exception ex) {
//            disableTest("problem simulating ssl server: "+ex.getMessage());
//            return null;                        
//        } finally {
//            if (httpsServer!=null)
//                httpsServer.stop(0);
//        }
        
        return fixtureFile;
    }

    public boolean isTestDataAvailable() {
        return fixture != null;
    }

    @Override
    public void setUp() throws Exception {
        // if the test was disabled we don't need to run the setup
        if (fixture == null)
            return;

        super.setUp();
    }

    
    

    /**
     * Permanently disable this test logging the specificed warning message (the reason
     * why the test is being disabled)
     * @param warning
     */
    private void disableTest(final String warning) {
        LOGGER.warning(warning);
        fixture = null;
        System.setProperty("gs." + fixtureId, "false");
    }

    protected HttpsServer createSSLServer() throws Exception{
            
    //        keytool -genkey -alias alias -keypass simulator \
    //        -keystore lig.keystore -storepass simulator
    
             
            InetSocketAddress address = new InetSocketAddress ( getProxyCallbackURLPrefix().getPort() );
    
            // initialise the HTTPS server
            HttpsServer httpsServer = HttpsServer.create ( address, 0 );
            SSLContext sslContext = SSLContext.getInstance ( "TLS" );
    
            // initialise the keystore
            char[] password = "changeit".toCharArray ();
            KeyStore ks = KeyStore.getInstance ( "JKS" );
            File base = new File(System.getProperty("user.home"), ".geoserver");
            File keystore = new File(base,"keystore.jks");
            FileInputStream fis = new FileInputStream ( keystore );
            ks.load ( fis, password );
    
            // setup the key manager factory
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance ( KeyManagerFactory.getDefaultAlgorithm() );
            kmf.init ( ks, password );
    
    
            // setup the trust manager factory
            //TrustManagerFactory tmf = TrustManagerFactory.getInstance ( TrustManagerFactory.getDefaultAlgorithm() );
            //tmf.init ( ks );
    
            // setup the HTTPS context and parameters
            sslContext.init ( kmf.getKeyManagers (), new TrustManager[]{trustManager}, null );
            httpsServer.setHttpsConfigurator(  new HttpsConfigurator( sslContext )
            {
                public void configure ( HttpsParameters params )
                {
                    try
                    {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault ();
                        SSLEngine engine = c.createSSLEngine ();
                        params.setNeedClientAuth ( false );
                        params.setCipherSuites ( engine.getEnabledCipherSuites () );
                        params.setProtocols ( engine.getEnabledProtocols () );
    
                        // get the default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters ();
                        params.setSSLParameters ( defaultSSLParameters );
                    }
                    catch ( Exception ex )
                    {
                        throw new RuntimeException(ex);
                    }
                }
            } );                
            
            
            httpsServer.createContext("/test", new HttpHandler() {            
                @Override
                public void handle(HttpExchange t) throws IOException {
                    LOGGER.info("https server working");
                    t.getRequestBody().close();                                                                
                    t.sendResponseHeaders(200, 0);
                    t.getResponseBody().close();
                }
            });
            
            httpsServer.setExecutor(null); // creates a default executor
            return httpsServer;
        }

    protected void checkSSLServer() throws Exception {
        
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, new TrustManager[] { trustManager} , null);
        SSLSocketFactory fac = HttpsURLConnection.getDefaultSSLSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
        URL testSSLURL = new URL(getProxyCallbackURLPrefix().getProtocol(),
                getProxyCallbackURLPrefix().getHost(),getProxyCallbackURLPrefix().getPort(),"/test");
        HttpURLConnection con = (HttpURLConnection) testSSLURL.openConnection();
        con.getInputStream().close();
        HttpsURLConnection.setDefaultSSLSocketFactory(fac);        
    }

}
