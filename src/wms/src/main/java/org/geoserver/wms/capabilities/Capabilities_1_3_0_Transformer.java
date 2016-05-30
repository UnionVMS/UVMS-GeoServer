/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.asc;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.ows.util.ResponseUtils.appendQueryString;
import static org.geoserver.ows.util.ResponseUtils.buildSchemaURL;
import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.geoserver.ows.util.ResponseUtils.params;

import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataLinkInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.ExtendedCapabilitiesProvider;
import org.geoserver.wms.GetCapabilities;
import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.capabilities.DimensionHelper.Mode;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.InternationalString;
import org.springframework.util.Assert;
import org.vfny.geoserver.util.ResponseUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Geotools xml framework based encoder for a Capabilities WMS 1.3.0 document.
 * 
 * @author Gabriel Roldan
 * @version $Id
 * @see GetCapabilities#run(GetCapabilitiesRequest)
 * @see GetCapabilitiesResponse#write(Object, java.io.OutputStream,
 *      org.geoserver.platform.Operation)
 */
public class Capabilities_1_3_0_Transformer extends TransformerBase {

    private static final String NAMESPACE = "http://www.opengis.net/wms";

    /** fixed MIME type for the returned capabilities document */
    public static final String WMS_CAPS_MIME = "text/xml";

    /** the WMS supported exception formats */
    static final String[] EXCEPTION_FORMATS = { "XML", "INIMAGE", "BLANK" };

    /**
     * The geoserver base URL to append it the schemas/wms/1.3.0/exceptions_1_3_0.xsd schema
     * location
     */
    private String schemaBaseURL;

    /** The list of output formats to state as supported for the GetMap request */
    private Collection<GetMapOutputFormat> getMapFormats;

    /** The list of all extended capabilities providers */
    private Collection<ExtendedCapabilitiesProvider> extCapsProviders;

    private WMS wmsConfig;

    /**
     * Creates a new WMSCapsTransformer object.
     * 
     * @param wms
     * 
     * @param schemaBaseUrl
     *            the base URL of the current request (usually "http://host:port/geoserver")
     * @param getMapFormats
     *            the list of supported output formats to state for the GetMap request
     */
    public Capabilities_1_3_0_Transformer(WMS wms, String schemaBaseUrl,
            Collection<GetMapOutputFormat> getMapFormats,
            Collection<ExtendedCapabilitiesProvider> extCapsProviders) {
        super();
        Assert.notNull(wms);
        Assert.notNull(schemaBaseUrl, "baseURL");
        Assert.notNull(getMapFormats, "getMapFormats");

        this.wmsConfig = wms;
        this.getMapFormats = getMapFormats;
        this.extCapsProviders = extCapsProviders;
        this.schemaBaseURL = schemaBaseUrl;
        this.setNamespaceDeclarationEnabled(true);

        setIndentation(2);
        final Charset encoding = wms.getCharSet();
        setEncoding(encoding);
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new Capabilities_1_3_0_Translator(handler, wmsConfig, getMapFormats,
                extCapsProviders, schemaBaseURL);
    }

    /**
     * @author Gabriel Roldan
     * @version $Id
     */
    private static class Capabilities_1_3_0_Translator extends TranslatorSupport {

        private static final String XML_SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

        private static final Logger LOGGER = Logging.getLogger(Capabilities_1_3_0_Translator.class);

        private static final String EPSG = "EPSG:";

        private static final String XLINK_NS = "http://www.w3.org/1999/xlink";

        /**
         * The request from wich all the information needed to produce the capabilities document can
         * be obtained
         */
        private GetCapabilitiesRequest request;

        private Collection<GetMapOutputFormat> getMapFormats;

        private Collection<ExtendedCapabilitiesProvider> extCapsProviders;

        private WMS wmsConfig;

        private String schemaBaseURL;
        
        DimensionHelper dimensionHelper;

        private boolean skipping;
        
        private LegendSample legendSample;

        /**
         * Creates a new CapabilitiesTranslator object.
         * 
         * @param handler
         *            content handler to send sax events to.
         * @param schemaBaseURL
         * @param schemaLoc
         * 
         */
        public Capabilities_1_3_0_Translator(ContentHandler handler, WMS wmsConfig,
                Collection<GetMapOutputFormat> getMapFormats,
                Collection<ExtendedCapabilitiesProvider> extCapsProviders, String schemaBaseURL) {
            super(handler, null, null);
            this.wmsConfig = wmsConfig;
            this.getMapFormats = getMapFormats;
            this.extCapsProviders = extCapsProviders;
            this.schemaBaseURL = schemaBaseURL;
            
            this.dimensionHelper = new DimensionHelper(Mode.WMS13, wmsConfig) {
                
                @Override
                protected void element(String element, String content, Attributes atts) {
                    Capabilities_1_3_0_Translator.this.element(element, content, atts);
                    
                }
                
                @Override
                protected void element(String element, String content) {
                    Capabilities_1_3_0_Translator.this.element(element, content);
                }
            };
            legendSample = GeoServerExtensions.bean(LegendSample.class);
            this.skipping = 
                ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                    wmsConfig.getGeoServer().getGlobal().getResourceErrorHandling());

            // register namespaces provided by extended capabilities
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                cp.registerNamespaces(getNamespaceSupport());
            }
        }

        private AttributesImpl attributes(String... kvp) {
            String[] atts = kvp;
            AttributesImpl attributes = new AttributesImpl();
            for (int i = 0; i < atts.length; i += 2) {
                String name = atts[i];
                String value = atts[i + 1];
                attributes.addAttribute("", name, name, "", value);
            }
            return attributes;
        }

        /**
         * @param o
         *            the {@link GetCapabilitiesRequest}
         * @throws IllegalArgumentException
         *             if {@code o} is not of the expected type
         */
        public void encode(Object o) throws IllegalArgumentException {
            if (!(o instanceof GetCapabilitiesRequest)) {
                throw new IllegalArgumentException();
            }

            this.request = (GetCapabilitiesRequest) o;

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(new StringBuffer("producing a capabilities document for ").append(
                        request).toString());
            }

            String schemaLocation = buildSchemaLocation();
            String updateSequence = String.valueOf(wmsConfig.getUpdateSequence());
            AttributesImpl rootAtts = attributes("version", "1.3.0", "updateSequence",
                    updateSequence, "xmlns", NAMESPACE, "xmlns:xlink", XLINK_NS, "xmlns:xsi",
                    XML_SCHEMA_INSTANCE, "xsi:schemaLocation", schemaLocation);

            start("WMS_Capabilities", rootAtts);
            handleService();
            handleCapability();
            end("WMS_Capabilities");
        }

        private String buildSchemaLocation() {
            StringBuffer schemaLocation = new StringBuffer();
            schemaLocation.append(schemaLocation(NAMESPACE, "wms/1.3.0/capabilities_1_3_0.xsd"));

            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                String[] locations = cp.getSchemaLocations(schemaBaseURL);
                try {
                    for (int i = 0; i < locations.length - 1; i += 2) {
                        schemaLocation.append(" ");
                        schemaLocation.append(schemaLocation(locations[i], locations[i + 1]));
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new ServiceException("Extended capabilities provider returned improper "
                            + "set of namespace,location pairs from getSchemaLocations()", e);
                }
            }

            return schemaLocation.toString();
        }

        String schemaLocation(String namespace, String uri) {
            String location = null;
            try {
                new URL(uri);

                // external location
                location = uri;
            } catch (MalformedURLException e) {
                // means the url is relative
                location = buildSchemaURL(schemaBaseURL, uri);
            }

            return namespace + " " + location;
        }

        /**
         * Encodes the service metadata section of a WMS capabilities document.
         */
        private void handleService() {
            start("Service");

            final WMSInfo serviceInfo = wmsConfig.getServiceInfo();
            element("Name", "WMS");
            element("Title", serviceInfo.getTitle());
            element("Abstract", serviceInfo.getAbstract());

            handleKeywordList(serviceInfo.getKeywords());

            String onlineResource = serviceInfo.getOnlineResource();
            if (onlineResource == null || onlineResource.trim().length() == 0) {
                String requestBaseUrl = request.getBaseUrl();
                onlineResource = buildURL(requestBaseUrl, null, null, URLType.SERVICE);
            } else {
                try {
                    new URL(onlineResource);
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.WARNING, "WMS online resource seems to be an invalid URL: '"
                            + onlineResource + "'");
                }
            }
            AttributesImpl attributes = attributes("xlink:type", "simple", "xlink:href",
                    onlineResource);
            element("OnlineResource", null, attributes);

            GeoServer geoServer = wmsConfig.getGeoServer();
            ContactInfo contact = geoServer.getSettings().getContact();
            handleContactInfo(contact);

            String fees = serviceInfo.getFees();
            element("Fees", fees == null ? "none" : fees);
            String constraints = serviceInfo.getAccessConstraints();
            element("AccessConstraints", constraints == null ? "none" : constraints);

            // TODO: LayerLimit, MaxWidth and MaxHeight have no equivalence in GeoServer config so
            // far
            end("Service");
        }

        /**
         * Encodes contact information in the WMS capabilities document
         */
        public void handleContactInfo(ContactInfo contact) {
            start("ContactInformation");

            start("ContactPersonPrimary");
            element("ContactPerson", contact.getContactPerson());
            element("ContactOrganization", contact.getContactOrganization());
            end("ContactPersonPrimary");

            element("ContactPosition", contact.getContactPosition());

            start("ContactAddress");
            element("AddressType", contact.getAddressType());
            element("Address", contact.getAddress());
            element("City", contact.getAddressCity());
            element("StateOrProvince", contact.getAddressState());
            element("PostCode", contact.getAddressPostalCode());
            element("Country", contact.getAddressCountry());
            end("ContactAddress");

            element("ContactVoiceTelephone", contact.getContactVoice());
            element("ContactFacsimileTelephone", contact.getContactFacsimile());
            element("ContactElectronicMailAddress", contact.getContactEmail());

            end("ContactInformation");
        }

        /**
         * Turns the keyword list to XML
         * 
         * @param keywords
         */
        private void handleKeywordList(List<KeywordInfo> keywords) {
            start("KeywordList");

            if (keywords != null) {
                for (KeywordInfo kw : keywords) {
                    AttributesImpl atts = new AttributesImpl();
                    if (kw.getVocabulary() != null) {
                        atts.addAttribute("", "vocabulary", "vocabulary", "", kw.getVocabulary());
                    }
                    element("Keyword", kw.getValue(), atts);
                }
            }

            end("KeywordList");
        }

        /**
         * Turns the metadata URL list to XML
         * 
         * @param keywords
         */
        private void handleMetadataList(Collection<MetadataLinkInfo> metadataURLs) {
            if (metadataURLs == null) {
                return;
            }

            for (MetadataLinkInfo link : metadataURLs) {

                AttributesImpl lnkAtts = new AttributesImpl();
                lnkAtts.addAttribute("", "type", "type", "", link.getMetadataType());
                start("MetadataURL", lnkAtts);

                element("Format", link.getType());

                String content = ResponseUtils.proxifyMetadataLink(link, request.getBaseUrl());

                AttributesImpl orAtts = attributes("xlink:type", "simple", "xlink:href", content);
                element("OnlineResource", null, orAtts);

                end("MetadataURL");
            }
        }

        /**
         * Turns the data URL list to XML
         * 
         * @param keywords
         */
        private void handleDataList(Collection<DataLinkInfo> dataURLs) {
            if (dataURLs == null) {
                return;
            }

            for (DataLinkInfo link : dataURLs) {

                start("DataURL");

                element("Format", link.getType());

                String content = ResponseUtils.proxifyDataLink(link, request.getBaseUrl());

                AttributesImpl orAtts = attributes("xlink:type", "simple", "xlink:href", content);
                element("OnlineResource", null, orAtts);

                end("DataURL");
            }
        }

        /**
         * Encodes the capabilities metadata section of a WMS capabilities document
         */
        private void handleCapability() {
            start("Capability");
            handleRequest();
            handleException();
            handleExtendedCapabilities();
            handleLayers();
            end("Capability");
        }

        private void handleRequest() {
            start("Request");

            start("GetCapabilities");
            element("Format", WMS_CAPS_MIME);

            // build the service URL and make sure it ends with &
            String serviceUrl = buildURL(request.getBaseUrl(), "ows", params("SERVICE", "WMS"),
                    URLType.SERVICE);
            serviceUrl = appendQueryString(serviceUrl, "");

            handleDcpType(serviceUrl, serviceUrl);
            end("GetCapabilities");

            start("GetMap");

            Set<String> formats = new LinkedHashSet();

            // return only mime types, since the cite tests dictate that a format
            // name must match the mime type
            for (GetMapOutputFormat format : getMapFormats) {
                if (format.getOutputFormatNames().contains(format.getMimeType())) {
                    formats.add(format.getMimeType());
                } else {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning("Map output format "
                                + format.getMimeType() + " (" + format.getClass() + ")"
                                + " does "
                                + "not include mime type in output format names. Will be excluded from"
                                + " capabilities document.");
                    }
                }
            }

            List<String> sortedFormats = new ArrayList(formats);
            Collections.sort(sortedFormats);
            // this is a hack necessary to make cite tests pass: we need an output format
            // that is equal to the mime type as the first one....
            if (sortedFormats.contains("image/png")) {
                sortedFormats.remove("image/png");
                sortedFormats.add(0, "image/png");
            }
            for (String format : sortedFormats) {
                element("Format", format);
            }

            handleDcpType(serviceUrl, null);// only GET method
            end("GetMap");

            start("GetFeatureInfo");

            for (String format : wmsConfig.getAllowedFeatureInfoFormats()) {
                element("Format", format);
            }

            handleDcpType(serviceUrl, null); // only GET method
            end("GetFeatureInfo");

            // no DescribeLayer in 1.3.0
            // start("DescribeLayer");
            // element("Format", DescribeLayerResponse.DESCLAYER_MIME_TYPE);
            // handleDcpType(serviceUrl, null);
            // end("DescribeLayer");

            // same thing, not defined for 1.3.0
            // start("GetLegendGraphic");
            //
            // for (String format : getLegendGraphicFormats) {
            // element("Format", format);
            // }
            //
            // handleDcpType(serviceUrl, null);
            // end("GetLegendGraphic");

            // no way
            // start("GetStyles");
            // element("Format", GetStylesResponse.SLD_MIME_TYPE);
            // handleDcpType(serviceUrl, null);
            // end("GetStyles");

            // but there are _ExtendedOperations in WMS 1.3.0, seems to be calling for an extension
            // point
            // TODO: define extension point for _ExtendedOperation
            end("Request");
        }

        /**
         * Encodes a <code>DCPType</code> fragment for HTTP GET and POST methods.
         * 
         * @param getUrl
         *            the URL of the onlineresource for HTTP GET method requests
         * @param postUrl
         *            the URL of the onlineresource for HTTP POST method requests
         */
        private void handleDcpType(String getUrl, String postUrl) {
            AttributesImpl orAtts = new AttributesImpl();
            orAtts.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
            orAtts.addAttribute(XLINK_NS, "href", "xlink:href", "", getUrl);
            start("DCPType");
            start("HTTP");

            if (getUrl != null) {
                start("Get");
                element("OnlineResource", null, orAtts);
                end("Get");
            }

            if (postUrl != null) {
                orAtts.setAttribute(1, "", "href", "xlink:href", "", postUrl);
                start("Post");
                element("OnlineResource", null, orAtts);
                end("Post");
            }

            end("HTTP");
            end("DCPType");
        }

        private void handleException() {
            start("Exception");

            for (String exceptionFormat : EXCEPTION_FORMATS) {
                element("Format", exceptionFormat);
            }

            end("Exception");
        }

        private void handleExtendedCapabilities() {
            for (ExtendedCapabilitiesProvider cp : extCapsProviders) {
                try {
                    cp.encode(new ExtendedCapabilitiesProvider.Translator() {
                        public void start(String element) {
                            Capabilities_1_3_0_Translator.this.start(element);
                        }

                        public void start(String element, Attributes attributes) {
                            Capabilities_1_3_0_Translator.this.start(element, attributes);
                        }

                        public void chars(String text) {
                            Capabilities_1_3_0_Translator.this.chars(text);
                        }

                        public void end(String element) {
                            Capabilities_1_3_0_Translator.this.end(element);
                        }
                    }, wmsConfig.getServiceInfo(), request);
                } catch (Exception e) {
                    throw new ServiceException("Extended capabilities provider threw error", e);
                }
            }
        }

        /**
         * Handles the encoding of the layers elements.
         * 
         * <p>
         * This method does a search over the SRS of all the layers to see if there are at least a
         * common one, as needed by the spec: "<i>The root Layer element shall include a sequence of
         * zero or more &lt;SRS&gt; elements listing all SRSes that are common to all subsidiary
         * layers. Use a single SRS element with empty content (like so: "&lt;SRS&gt;&lt;/SRS&gt;")
         * if there is no common SRS."</i>
         * </p>
         * 
         * <p>
         * By the other hand, this search is also used to collecto the whole latlon bbox, as stated
         * by the spec: <i>"The bounding box metadata in Capabilities XML specify the minimum
         * enclosing rectangle for the layer as a whole."</i>
         * </p>
         * 
         * @task TODO: manage this differently when we have the layer list of the WMS service
         *       decoupled from the feature types configured for the server instance. (This involves
         *       nested layers, gridcoverages, etc)
         */
        private void handleLayers() {
            start("Layer");

            //ask for enabled and advertised to start with
            Filter filter;
            {
                Filter enabled = equal("enabled", Boolean.TRUE);
                Filter advertised = equal("advertised", Boolean.TRUE);
                filter = and(enabled, advertised);
            }
            
            // filter the layers if a namespace filter has been set
            if (request.getNamespace() != null) {
                //build a query predicate for the namespace prefix
                final String nsPrefix = request.getNamespace();
                final String nsProp = "resource.namespace.prefix";
                Filter equals = equal(nsProp, nsPrefix);
                filter = and(filter, equals);
            }

            final Catalog catalog = wmsConfig.getCatalog();
                        
            WMSInfo serviceInfo = wmsConfig.getServiceInfo();
            element("Title", serviceInfo.getTitle());
            element("Abstract", serviceInfo.getAbstract());

            List<String> srsList = serviceInfo.getSRS();
            Set<String> srs = new HashSet<String>();
            if (srsList != null) {
                srs.addAll(srsList);
            }
            for (ExtendedCapabilitiesProvider provider : extCapsProviders) {
                provider.customizeRootCrsList(srs);
            }
            handleRootCrsList(srs);

            CloseableIterator<LayerInfo> layers;
            layers = catalog.list(LayerInfo.class, filter);
            try{
                handleRootBbox(layers);
            }finally{
                layers.close();
            }

            // handle AuthorityURL
            handleAuthorityURL(serviceInfo.getAuthorityURLs());
            
            // handle identifiers
            handleLayerIdentifiers(serviceInfo.getIdentifiers());

            Set<LayerInfo> layersAlreadyProcessed = new HashSet<LayerInfo>();
            
            // encode layer groups
            CloseableIterator<LayerGroupInfo> layerGroups;
            {
                final Filter lgFilter = Predicates.acceptAll();
                SortBy layerGroupOrder = asc("name");
                layerGroups = catalog.list(LayerGroupInfo.class, lgFilter, null, null,
                        layerGroupOrder);
            }            
            try {
                layersAlreadyProcessed = handleLayerGroups(layerGroups);
            } catch (Exception e) {
                throw new RuntimeException("Can't obtain Envelope of Layer-Groups: "
                        + e.getMessage(), e);
            } finally {
                layerGroups.close();
            }            
            
            // now encode each layer individually
            SortBy layerOrder = asc("name");
            layers = catalog.list(LayerInfo.class, filter, null, null, layerOrder);
            try {
                handleLayerTree(layers, layersAlreadyProcessed);
            } finally {
                layers.close();
            }

            end("Layer");
        }

        /**
         * Called by <code>handleLayers()</code>, writes down list of supported CRS's for the root
         * Layer.
         * <p>
         * If <code>epsgCodes</code> is not empty, the list of supported CRS identifiers written
         * down to the capabilities document is limited to those in the <code>epsgCodes</code> list.
         * Otherwise, all the GeoServer supported CRS identifiers are used.
         * </p>
         * 
         * @param epsgCodes
         *            possibly empty set of CRS identifiers to limit the number of SRS elements to.
         */
        private void handleRootCrsList(final Set<String> epsgCodes) {
            final Set<String> capabilitiesCrsIdentifiers;
            if (epsgCodes.isEmpty()) {
                comment("All supported EPSG projections:");
                capabilitiesCrsIdentifiers = new LinkedHashSet<String>();
                for (String code : CRS.getSupportedCodes("AUTO")) {
                    if ("WGS84(DD)".equals(code))
                        continue;
                    capabilitiesCrsIdentifiers.add("AUTO:" + code);
                }
                capabilitiesCrsIdentifiers.addAll(CRS.getSupportedCodes("EPSG"));
            } else {
                comment("Limited list of EPSG projections:");
                capabilitiesCrsIdentifiers = new TreeSet<String>(epsgCodes);
            }

            try {
                Iterator<String> it = capabilitiesCrsIdentifiers.iterator();
                String currentSRS;

                while (it.hasNext()) {
                    currentSRS = qualifySRS(it.next());
                    element("CRS", currentSRS);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }

            // the default CRS:84
            element("CRS", "CRS:84");
        }

        /**
         * prefixes an srs code with "EPSG:" if it is not already prefixed.
         */
        private String qualifySRS(String srs) {
            if (srs.indexOf(':') == -1) {
                srs = EPSG + srs;
            }
            return srs;
        }

        /**
         * Called by <code>handleLayers()</code>, iterates over the available featuretypes and
         * coverages to summarize their LatLonBBox'es and write the aggregated bounds for the root
         * layer.
         * 
         * @param ftypes
         *            the collection of FeatureTypeInfo and CoverageInfo objects to traverse
         */
        private void handleRootBbox(Iterator<LayerInfo> layers) {

            final Envelope world = new Envelope(-180, 180, -90, 90);
            
            Envelope latlonBbox = new Envelope();
            Envelope layerBbox = null;

            LOGGER.finer("Collecting summarized latlonbbox and common SRS...");

            while(layers.hasNext()) {
                LayerInfo layer = layers.next();
                ResourceInfo resource = layer.getResource();
                layerBbox = resource.getLatLonBoundingBox();
                if (layerBbox != null) {
                    latlonBbox.expandToInclude(layerBbox);    
                }

                //short cut for the case where we already reached the whole world bounds
                if(latlonBbox.contains(world)){
                    break;
                }
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Summarized LatLonBBox is " + latlonBbox);
            }

            handleGeographicBoundingBox(latlonBbox);
            handleBBox(latlonBbox, "CRS:84");
            handleAdditionalBBox(
                new ReferencedEnvelope(latlonBbox, DefaultGeographicCRS.WGS84), null, null);
        }

        private void handleLayerTree(final Iterator<LayerInfo> layers, Set<LayerInfo> layersAlreadyProcessed) {
            // Build a LayerTree only for the layers that have a wms path set. Process the ones that
            // don't first
            LayerTree nestedLayers = new LayerTree();
            
            //handle non nested layers
            while (layers.hasNext()) {
                LayerInfo layer = layers.next();
                if(layersAlreadyProcessed.contains(layer) || !isExposable(layer)){
                    continue;
                }
                final String path = layer.getPath();
                if(path != null && path.length() > 0 && !"/".equals(path)){
                    nestedLayers.add(layer);
                    continue;
                }

                doHandleLayer(layer);
            }
            
            //handle nested layers
            handleLayerTree(nestedLayers);
        }
        
        /**
         * @param layerTree
         */
        private void handleLayerTree(final LayerTree layerTree) {
            final List<LayerInfo> data = new ArrayList<LayerInfo>(layerTree.getData());
            final Collection<LayerTree> children = layerTree.getChildrens();

            Collections.sort(data, new Comparator<LayerInfo>() {
                public int compare(LayerInfo o1, LayerInfo o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            for (LayerInfo layer : data) {
                // no sense in exposing a geometryless layer through wms...
                boolean wmsExposable = isExposable(layer);
                if (wmsExposable) {
                    doHandleLayer(layer);
                }
            }

            for (LayerTree childLayerTree : children) {
                start("Layer");
                element("Name", childLayerTree.getName());
                element("Title", childLayerTree.getName());
                handleLayerTree(childLayerTree);
                end("Layer");
            }
        }

        private void doHandleLayer(LayerInfo layer) {
            try {
                mark();
                handleLayer(layer);
                commit();
            } catch (Exception e) {
                // report what layer we failed on to help the admin locate and fix it
                if (skipping) {
                    reset();
                } else { 
                    throw new ServiceException(
                        "Error occurred trying to write out metadata for layer: " + 
                        layer.getName(), e);
                }
            }
        }

        private boolean isExposable(LayerInfo layer) {
            // we filtered by the isEnabled property,but check for enabled() to account for the
            // resource and store
            if (!layer.enabled()) {
                return false;
            }

            boolean wmsExposable = false;
            if (layer.getType() == PublishedType.RASTER || layer.getType() == PublishedType.WMS) {
                wmsExposable = true;
            } else {
                try {
                    wmsExposable = layer.getType() == PublishedType.VECTOR
                            && ((FeatureTypeInfo) layer.getResource()).getFeatureType()
                                    .getGeometryDescriptor() != null;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "An error occurred trying to determine if"
                            + " the layer is geometryless", e);
                }
            }
            return wmsExposable;
        }

        /**
         * @throws IOException 
         * @throws RuntimeException 
         */
        protected void handleLayer(final LayerInfo layer) throws IOException {
            boolean queryable = wmsConfig.isQueryable(layer);
            AttributesImpl qatts = attributes("queryable", queryable ? "1" : "0");
            boolean opaque = wmsConfig.isOpaque(layer);
            qatts.addAttribute("", "opaque", "opaque", "", opaque ? "1" : "0");
            Integer cascadedHopCount = wmsConfig.getCascadedHopCount(layer);
            if (cascadedHopCount != null) {
                qatts.addAttribute("", "cascaded", "cascaded", "", String.valueOf(cascadedHopCount));
            }
            start("Layer", qatts);
            element("Name", layer.prefixedName());
            // REVISIT: this is bad, layer should have title and anbstract by itself
            element("Title", layer.getResource().getTitle());
            element("Abstract", layer.getResource().getAbstract());
            handleKeywordList(layer.getResource().getKeywords());

            final String crs = layer.getResource().getSRS();
            element("CRS", crs);

            // always handle the CRS:84 crs
            element("CRS", "CRS:84");

            ReferencedEnvelope llbbox = layer.getResource().getLatLonBoundingBox();
            handleGeographicBoundingBox(llbbox);
            handleBBox(llbbox, "CRS:84");

            ReferencedEnvelope bbox;
            try {
                bbox = layer.getResource().boundingBox();
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error obtaining bounding box for layer "
                        + layer.getName(), e);
            }
            // the native bbox might be null
            if (bbox != null) {
                handleBBox(bbox, crs);
                handleAdditionalBBox(bbox, crs, layer);
            }

            // handle dimensions
            if (layer.getType() == PublishedType.VECTOR) {
                dimensionHelper.handleVectorLayerDimensions(layer);
            } else if (layer.getType() == PublishedType.RASTER) {
                dimensionHelper.handleRasterLayerDimensions(layer);
            }

            // handle data attribution
            handleAttribution(layer);

            // handle AuthorityURL
            handleAuthorityURL(layer.getAuthorityURLs());
            
            // handle identifiers
            handleLayerIdentifiers(layer.getIdentifiers());

            // handle metadata URLs
            handleMetadataList(layer.getResource().getMetadataLinks());

            // handle DataURLs
            handleDataList(layer.getResource().getDataLinks());
            // TODO: FeatureListURL
            handleStyles(layer);
            
            handleScaleDenominator(layer);

            end("Layer");
        }

        /**
         * Inserts the scale denominator elements in the layer information.
         * 
         * <pre>
         * <code>MinScaleDenominator</code>
         * <code>MaxScaleDenominator</code>
         * </pre>
         * 
         * @param layer
         */
        private void handleScaleDenominator(final LayerInfo layer) {

            try {
                NumberRange<Double> scaleDenominators = CapabilityUtil
                        .searchMinMaxScaleDenominator(layer);

                // allow extension points to customize
                for (ExtendedCapabilitiesProvider provider : extCapsProviders) {
                    scaleDenominators = provider
                            .overrideScaleDenominators(layer, scaleDenominators);
                }

                if (scaleDenominators.getMinimum() != 0.0) {
                    element("MinScaleDenominator", String.valueOf(scaleDenominators.getMinimum()));
                }

                if (scaleDenominators.getMaximum() != Double.POSITIVE_INFINITY) {
                    element("MaxScaleDenominator", String.valueOf(scaleDenominators.getMaximum()));
                }

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }

		private void handleStyles(final LayerInfo layer) {
            if (layer.getResource() instanceof WMSLayerInfo) {
                // do nothing for the moment, we may want to list the set of cascaded named styles
                // in the future (when we add support for that)
            } else {
                // add the layer style
                start("Style");

                StyleInfo defaultStyle = layer.getDefaultStyle();
                if (defaultStyle == null) {
                    throw new NullPointerException("Layer " + layer.getName()
                            + " has no default style");
                }
                Style ftStyle;
                try {
                    ftStyle = defaultStyle.getStyle();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                element("Name", defaultStyle.prefixedName());
                if (ftStyle.getDescription() != null) {
                    element("Title", ftStyle.getDescription().getTitle());
                    element("Abstract", ftStyle.getDescription().getAbstract());
                }
                handleLegendURL(layer, layer.getLegend(), null, layer.getDefaultStyle());
                end("Style");

                Set<StyleInfo> styles = layer.getStyles();

                if(styles != null){
                    for (StyleInfo styleInfo : styles) {
                        try {
                            ftStyle = styleInfo.getStyle();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        start("Style");
                        element("Name", styleInfo.prefixedName());
                        if (ftStyle.getDescription() != null) {
                            element("Title", ftStyle.getDescription().getTitle());
                            element("Abstract", ftStyle.getDescription().getAbstract());
                        }
                        handleLegendURL(layer, null, styleInfo, styleInfo);
                        end("Style");
                    }
                }
            }
        }

        private void element(String element, InternationalString is) {
            if (is != null) {
                element(element, is.toString());
            }
        }
        
        protected Set<LayerInfo> handleLayerGroups(Iterator<LayerGroupInfo> layerGroups) throws FactoryException,
                TransformException, IOException {
            Set<LayerInfo> layersAlreadyProcessed = new HashSet<LayerInfo>();
            
            if (layerGroups == null) {
                return layersAlreadyProcessed;
            }
            
            List<LayerGroupInfo> topLevelGroups = filterNestedGroups(layerGroups);

            for (LayerGroupInfo group : topLevelGroups) {
                try {
                    mark();
                    handleLayerGroup(group, layersAlreadyProcessed);
                    commit();
                } catch (Exception e) {
                    // report what layer we failed on to help the admin locate and fix it
                    if (skipping) {
                        if(group != null) {
                            LOGGER.log(Level.WARNING, "Skipping layer group " + group.getName() + " as its caps document element failed to generate", e);
                        } else {
                            LOGGER.log(Level.WARNING, "Skipping a null layer group during caps during caps document generation", e);
                        }

                        reset();
                    } else { 
                        throw new ServiceException(
                            "Error occurred trying to write out metadata for layer group: " + 
                            group.getName(), e);
                    }
                }
            }
            
            return layersAlreadyProcessed;
        }
        
        /**
         * Returns a list of top level groups, that is, the ones that are not nested within
         * other layer groups
         * 
         * @param allGroups
         * @return
         */
        private List<LayerGroupInfo> filterNestedGroups(Iterator<LayerGroupInfo> iterator) {
            List<LayerGroupInfo> allGroups = new ArrayList<LayerGroupInfo>();
            while(iterator.hasNext()) {
                allGroups.add(iterator.next());
            }
            LinkedHashSet<LayerGroupInfo> result = new LinkedHashSet<LayerGroupInfo>(allGroups);
            for(LayerGroupInfo group : allGroups) {
                for(PublishedInfo pi : group.getLayers()) {
                    if(pi instanceof LayerGroupInfo) {
                        result.remove(pi);
                    }
                }
            }
            
            return new ArrayList<LayerGroupInfo>(result);
        }
        


        protected void handleLayerGroup(LayerGroupInfo layerGroup, Set<LayerInfo> layersAlreadyProcessed) throws TransformException, FactoryException, IOException {
            String layerName = layerGroup.prefixedName();

            AttributesImpl qatts = new AttributesImpl();
            boolean queryable = wmsConfig.isQueryable(layerGroup);
            qatts.addAttribute("", "queryable", "queryable", "", queryable ? "1" : "0");
            start("Layer", qatts);
            
            if (!LayerGroupInfo.Mode.CONTAINER.equals(layerGroup.getMode())) {
                element("Name", layerName);
            }
            
            if (StringUtils.isEmpty(layerGroup.getTitle())) {
                element("Title", layerName);                    
            } else {
                element("Title", layerGroup.getTitle());
            }

            if (StringUtils.isEmpty(layerGroup.getAbstract())) {
                element("Abstract", "Layer-Group type layer: " + layerName);
            } else {
                element("Abstract", layerGroup.getAbstract());
            }
            
            final ReferencedEnvelope layerGroupBounds = layerGroup.getBounds();
            final ReferencedEnvelope latLonBounds = layerGroupBounds.transform(
                    DefaultGeographicCRS.WGS84, true);

            String authority = layerGroupBounds.getCoordinateReferenceSystem().getIdentifiers()
                    .toArray()[0].toString();

            element("CRS", authority);

            handleGeographicBoundingBox(latLonBounds);
            handleBBox(layerGroupBounds, authority);

            if (LayerGroupInfo.Mode.EO.equals(layerGroup.getMode())) {
                LayerInfo rootLayer = layerGroup.getRootLayer();
                
                // handle dimensions
                if (rootLayer.getType() == PublishedType.VECTOR) {
                    dimensionHelper.handleVectorLayerDimensions(rootLayer);
                } else if (rootLayer.getType() == PublishedType.RASTER) {
                    dimensionHelper.handleRasterLayerDimensions(rootLayer);
                }
                
                layersAlreadyProcessed.add(layerGroup.getRootLayer());
            }                
            
            // handle AuthorityURL
            handleAuthorityURL(layerGroup.getAuthorityURLs());
            
            // handle identifiers
            handleLayerIdentifiers(layerGroup.getIdentifiers());
            
            // Aggregated metadata links (see GEOS-4500)
            Set<MetadataLinkInfo> aggregatedLinks = new HashSet<MetadataLinkInfo>();
            for (LayerInfo layer : Iterables.filter(layerGroup.getLayers(), LayerInfo.class)) {
                List<MetadataLinkInfo> metadataLinks = layer.getResource().getMetadataLinks();
                if (metadataLinks != null) {
                    aggregatedLinks.addAll(metadataLinks);
                }
            }
            handleMetadataList(aggregatedLinks);

            // handle children layers and groups
            if (!LayerGroupInfo.Mode.SINGLE.equals(layerGroup.getMode())) {
                for (PublishedInfo child : layerGroup.getLayers()) {
                    if (child instanceof LayerInfo) {
                        LayerInfo layer = (LayerInfo) child;
                        if (isExposable(layer)) {
                            handleLayer((LayerInfo) child);
                            layersAlreadyProcessed.add((LayerInfo) child);
                        }
                    } else {
                        handleLayerGroup((LayerGroupInfo) child, layersAlreadyProcessed);
                    }
                }
            }
            
            // the layer style is not provided since the group does just have
            // one possibility, the lack of styles that will make it use
            // the default ones for each layer

            end("Layer");            
        }
        
        protected void handleAttribution(LayerInfo layer) {
            AttributionInfo attribution = layer.getAttribution();

            String title = attribution.getTitle();
            String url = attribution.getHref();
            String logoURL = attribution.getLogoURL();
            String logoType = attribution.getLogoType();
            int logoWidth = attribution.getLogoWidth();
            int logoHeight = attribution.getLogoHeight();

            boolean titleGood = (title != null), urlGood = (url != null), logoGood = (logoURL != null
                    && logoType != null && logoWidth > 0 && logoHeight > 0);

            if (titleGood || urlGood || logoGood) {
                start("Attribution");
                if (titleGood)
                    element("Title", title);

                if (urlGood) {
                    AttributesImpl urlAttributes = new AttributesImpl();
                    urlAttributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                    urlAttributes.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                    urlAttributes.addAttribute(XLINK_NS, "href", "xlink:href", "", url);
                    element("OnlineResource", null, urlAttributes);
                }

                if (logoGood) {
                    AttributesImpl logoAttributes = new AttributesImpl();
                    logoAttributes.addAttribute("", "", "height", "", "" + logoHeight);
                    logoAttributes.addAttribute("", "", "width", "", "" + logoWidth);
                    start("LogoURL", logoAttributes);
                    element("Format", logoType);

                    AttributesImpl urlAttributes = new AttributesImpl();
                    urlAttributes.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                    urlAttributes.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                    urlAttributes.addAttribute(XLINK_NS, "href", "xlink:href", "", logoURL);

                    element("OnlineResource", null, urlAttributes);
                    end("LogoURL");
                }

                end("Attribution");
            }
        }

        /**
         * Writes layer LegendURL pointing to the user supplied icon URL, if any, or to the proper
         * GetLegendGraphic operation if an URL was not supplied.
         * 
         * <p>
         * It is common practice to supply a URL to a WMS accesible legend graphic when it is
         * difficult to create a dynamic legend for a layer.
         * </p>
         * 
         * @param layerName
         *            The name of the layer.
         * @param legend
         *            The user specified legend url. If null a default url pointing back to the
         *            GetLegendGraphic operation will be automatically created.
         * @param style
         *            The styel for the layer.
         * @param sampleStyle
         *            The styel to use for sample sizing.
         * 
         */
        protected void handleLegendURL(LayerInfo layer, LegendInfo legend,
                StyleInfo style, StyleInfo sampleStyle) {
            if (legend != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("using user supplied legend URL");
                }

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "width", "width", "", String.valueOf(legend.getWidth()));
                attrs.addAttribute("", "height", "height", "", String.valueOf(legend.getHeight()));

                start("LegendURL", attrs);

                element("Format", legend.getFormat());
                attrs.clear();
                attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legend.getOnlineResource());

                element("OnlineResource", null, attrs);

                end("LegendURL");
            } else {
                int legendWidth = GetLegendGraphicRequest.DEFAULT_WIDTH;
                int legendHeight = GetLegendGraphicRequest.DEFAULT_HEIGHT;
                
                if(sampleStyle != null) {
                    // delegate to legendSample the calculus of proper legend size for
                    // the given style
                    Dimension dimension;
                    try {
                        dimension = legendSample.getLegendURLSize(sampleStyle);
                        if(dimension != null) {
                            legendWidth = (int)dimension.getWidth();
                            legendHeight = (int)dimension.getHeight();
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error getting LegendURL dimensions from sample", e);
                    }
                }
                
                String defaultFormat = GetLegendGraphicRequest.DEFAULT_FORMAT;

                if (null == wmsConfig.getLegendGraphicOutputFormat(defaultFormat)) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(new StringBuffer("Default legend format (")
                                .append(defaultFormat)
                                .append(")is not supported (jai not available?), can't add LegendURL element")
                                .toString());
                    }

                    return;
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Adding GetLegendGraphic call as LegendURL");
                }

                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "width", "width", "",
                        String.valueOf(legendWidth));

                attrs.addAttribute("", "height", "height", "",
                        String.valueOf(legendHeight));

                start("LegendURL", attrs);

                element("Format", defaultFormat);
                attrs.clear();

                String layerName = layer.prefixedName();
                Map<String, String> params = params("service", "WMS", "request",
                        "GetLegendGraphic", "format", defaultFormat, "width",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_WIDTH), "height",
                        String.valueOf(GetLegendGraphicRequest.DEFAULT_HEIGHT), "layer", layerName);
                if (style != null) {
                    params.put("style", style.getName());
                }
                String legendURL = buildURL(request.getBaseUrl(), "ows", params, URLType.SERVICE);

                attrs.addAttribute("", "xmlns:xlink", "xmlns:xlink", "", XLINK_NS);
                attrs.addAttribute(XLINK_NS, "type", "xlink:type", "", "simple");
                attrs.addAttribute(XLINK_NS, "href", "xlink:href", "", legendURL);
                element("OnlineResource", null, attrs);

                end("LegendURL");
            }
        }

        /**
         * Encodes a LatLonBoundingBox for the given Envelope.
         * 
         * @param bbox
         */
        private void handleGeographicBoundingBox(Envelope bbox) {
            String minx = String.valueOf(bbox.getMinX());
            String miny = String.valueOf(bbox.getMinY());
            String maxx = String.valueOf(bbox.getMaxX());
            String maxy = String.valueOf(bbox.getMaxY());

            start("EX_GeographicBoundingBox");
            element("westBoundLongitude", minx);
            element("eastBoundLongitude", maxx);
            element("southBoundLatitude", miny);
            element("northBoundLatitude", maxy);
            end("EX_GeographicBoundingBox");
        }

        /**
         * Encodes a BoundingBox for the given Envelope.
         * 
         * @param bbox
         */
        private void handleBBox(Envelope bbox, String srs) {
            String minx = String.valueOf(bbox.getMinX());
            String miny = String.valueOf(bbox.getMinY());
            String maxx = String.valueOf(bbox.getMaxX());
            String maxy = String.valueOf(bbox.getMaxY());

            // we need to report geographic coordinate as latitude/longitude
            CoordinateReferenceSystem crs = null;
            try {
                crs = CRS.decode(WMS.toInternalSRS(srs, WMS.VERSION_1_3_0));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to decode " + srs, e);
            }

            if (crs != null && CRS.getAxisOrder(crs) == AxisOrder.NORTH_EAST) {
                String tmp = minx;
                minx = miny;
                miny = tmp;
                tmp = maxx;
                maxx = maxy;
                maxy = tmp;
            }

            AttributesImpl bboxAtts = attributes("CRS", srs, //
                    "minx", minx, //
                    "miny", miny,//
                    "maxx", maxx,//
                    "maxy", maxy);

            element("BoundingBox", null, bboxAtts);
        }

        private void handleAdditionalBBox(ReferencedEnvelope bbox, String crs, LayerInfo layer) {
            WMSInfo info = wmsConfig.getServiceInfo();
            if (info.isBBOXForEachCRS() && !info.getSRS().isEmpty()) {
                //output bounding box for each supported service srs
                for (String srs : info.getSRS()) {
                    srs = qualifySRS(srs);
                    if (crs != null && srs.equals(crs)) {
                        continue; //already did this one
                    }

                    try {
                        ReferencedEnvelope tbbox = bbox.transform(CRS.decode(srs), true);
                        handleBBox(tbbox, srs);
                    } 
                    catch(Exception e) {
                        LOGGER.warning(String.format("Unable to transform bounding box for '%s' layer" +
                            " to %s", layer != null ? layer.getName() : "root", srs));
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, e.getLocalizedMessage(), e);
                        }
                    }
                }
            }
        }
        
        /**
         * e.g.
         * {@code <AuthorityURL name="DIF_ID"><OnlineResource xlink:type="simple" xlink:href="http://gcmd.gsfc.nasa.gov/difguide/whatisadif.html"/></AuthorityURL>}
         */
        private void handleAuthorityURL(List<AuthorityURLInfo> authorityURLs) {
            if (authorityURLs == null || authorityURLs.isEmpty()) {
                return;
            }

            String name;
            String href;
            AttributesImpl atts = new AttributesImpl();
            for (AuthorityURLInfo url : authorityURLs) {
                name = url.getName();
                href = url.getHref();
                if (name == null || href == null) {
                    LOGGER.warning("Ignoring AuthorityURL, name: " + name + ", href: " + href);
                    continue;
                }
                atts.clear();
                atts.addAttribute("", "name", "name", "", name);
                start("AuthorityURL", atts);

                atts.clear();
                atts.addAttribute("", "xlink:type", "xlink:type", "", "simple");
                atts.addAttribute("", "xlink:href", "xlink:href", "", href);
                element("OnlineResource", null, atts);
                end("AuthorityURL");
            }
        }

        /**
         * e.g. {@code <Identifier authority="gcmd">id_value</Identifier>}
         */
        private void handleLayerIdentifiers(List<LayerIdentifierInfo> identifiers) {
            if (identifiers == null || identifiers.isEmpty()) {
                return;
            }

            String authority;
            String id;
            AttributesImpl atts = new AttributesImpl();
            for (LayerIdentifierInfo identifier : identifiers) {
                authority = identifier.getAuthority();
                id = identifier.getIdentifier();
                if (authority == null || id == null) {
                    LOGGER.warning("Ignoring layer Identifier, authority: " + authority
                            + ", identifier: " + id);
                    continue;
                }
                atts.clear();
                atts.addAttribute("", "authority", "authority", "", authority);
                element("Identifier", id, atts);
            }
        }
    }
}
