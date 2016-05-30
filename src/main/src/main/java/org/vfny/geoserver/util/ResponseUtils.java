/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.geoserver.catalog.DataLinkInfo;

import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.KvpUtils;
import org.geotools.util.logging.Logging;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;


public final class ResponseUtils {

    static Logger LOGGER = Logging.getLogger(ResponseUtils.class);

    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#encodeXML(String)}.
     */
    public static String encodeXML(String inData) {
        return org.geoserver.ows.util.ResponseUtils.encodeXML(inData);
    }

    /**
     * @deprecated moved to {@link org.geoserver.ows.util.ResponseUtils#writeEscapedString(Writer, String)}
     */
    public static void writeEscapedString(Writer writer, String string)
        throws IOException {
        org.geoserver.ows.util.ResponseUtils.writeEscapedString(writer, string);
    }

    /*
    Profixies a link url interpreting a localhost url as a back reference to the server.
    */
    private static String proxifyLink(String content, String baseURL) {
        try {
            URI uri = new URI(content);
            try {
                if (uri.getHost() == null) {
                    //interpret no host as backreference to server
                    Map<String, String> kvp = null;
                    if (uri.getQuery() != null && !"".equals(uri.getQuery())) {
                        Map<String, Object> parsed = KvpUtils.parseQueryString("?" + uri.getQuery());
                        kvp = new HashMap<String, String>();
                        for (Entry<String, Object> entry : parsed.entrySet()) {
                            kvp.put(entry.getKey(), (String) entry.getValue());
                        }
                    }

                    content = buildURL(baseURL, uri.getPath(), kvp, URLType.RESOURCE);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Unable to create proper back reference for url: "
                                + content, e);
            }
        } catch (URISyntaxException e) {
        }
        return content;        
    }
    /**
     * Profixies a metadata link url interpreting a localhost url as a back reference to the server.
     * <p>
     * If <tt>link</tt> is not a localhost url it is left untouched.
     * </p>
     */
    public static String proxifyMetadataLink(MetadataLinkInfo link, String baseURL) {
        String content = link.getContent();
        content = proxifyLink(content, baseURL);
        return content;
    }

    /**
     * Profixies a data link url interpreting a localhost url as a back reference to the server.
     * <p>
     * If <tt>link</tt> is not a localhost url it is left untouched.
     * </p>
     */
    public static String proxifyDataLink(DataLinkInfo link, String baseURL) {
        String content = link.getContent();
        content = proxifyLink(content, baseURL);
        return content;
    }

    public static List validate(InputSource xml, URL schemaURL, boolean skipTargetNamespaceException) {
        StreamSource source = null;
        if (xml.getCharacterStream() != null) { 
            source = new StreamSource(xml.getCharacterStream());
        }
        else if (xml.getByteStream() != null) {
            source = new StreamSource(xml.getByteStream());
        }
        else {
            throw new IllegalArgumentException("Could not turn input source to stream source");
        }
        return validate(source, schemaURL, skipTargetNamespaceException);
    }

    public static List validate(Source xml, URL schemaURL, boolean skipTargetNamespaceException) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            Schema schema = 
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaURL);
            Validator v = schema.newValidator();
            Handler handler = new Handler(skipTargetNamespaceException);
            v.setErrorHandler(handler);
            v.validate(xml);
            return handler.errors;
        } catch (SAXException e) {
            return exception(e);
        } catch (IOException e) {
            return exception(e);
        }
    }

    // errors in the document will be put in "errors".
    // if errors.size() ==0  then there were no errors.
    private static class Handler extends DefaultHandler {
        public ArrayList errors = new ArrayList();

        boolean skipTargetNamespaceException;
        
        Handler (boolean skipTargetNamespaceExeption) {
            this.skipTargetNamespaceException = skipTargetNamespaceExeption;
        }

        public void error(SAXParseException exception)
            throws SAXException {
            if (skipTargetNamespaceException && exception.getMessage()
                .startsWith("TargetNamespace.2: Expecting no namespace, but the schema document has a target name")) {
                    return;
            }

            errors.add(exception);
        }

        public void fatalError(SAXParseException exception)
            throws SAXException {
            errors.add(exception);
        }

        public void warning(SAXParseException exception)
            throws SAXException {
            //do nothing
        }
    }
    static List exception(Exception e) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Validation error", e);
        }
        return Arrays.asList(new SAXParseException(e.getLocalizedMessage(), null));
    }
}
