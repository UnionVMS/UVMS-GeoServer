/* (c) 2014 -2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.validator.MaxSizeValidator;
import org.geoserver.wps.validator.MultiplicityValidator;
import org.geoserver.wps.validator.NumberRangeValidator;
import org.geotools.data.DataUtilities;
import org.geotools.feature.NameImpl;
import org.geotools.process.geometry.GeometryProcessFactory;
import org.geotools.process.raster.RasterProcessFactory;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class InputLimitsTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // need no layers for this test
        testData.setUpSecurity();
    }

    @Before
    public void setUpInternal() throws Exception {
        // make extra sure we don't have anything else going
        MonkeyProcess.clearCommands();
    }

    @Before
    public void resetExecutionTimes() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxSynchronousExecutionTime(0);
        wps.setMaxAsynchronousExecutionTime(0);
        getGeoServer().save(wps);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);

        // add some limits to the processes
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        // global size limits
        wps.setMaxComplexInputSize(10);

        // for the buffer process
        ProcessGroupInfo geoGroup = new ProcessGroupInfoImpl();
        geoGroup.setFactoryClass(GeometryProcessFactory.class);
        geoGroup.setEnabled(true);
        wps.getProcessGroups().add(geoGroup);
        ProcessInfo buffer = new ProcessInfoImpl();
        buffer.setEnabled(true);
        buffer.setName(new NameImpl("geo", "buffer"));
        buffer.getValidators().put("geom", new MaxSizeValidator(1));
        buffer.getValidators().put("distance",
                new NumberRangeValidator(new NumberRange<Double>(Double.class, 0d, 100d)));
        buffer.getValidators().put("quadrantSegments",
                new NumberRangeValidator(new NumberRange<Integer>(Integer.class, 2, 20)));
        geoGroup.getFilteredProcesses().add(buffer);

        // simplify process distance
        ProcessInfo simplify = new ProcessInfoImpl();
        simplify.setEnabled(true);
        simplify.setName(new NameImpl("geo", "simplify"));
        simplify.getValidators().put("distance",
                new NumberRangeValidator(new NumberRange<Double>(Double.class, null, 100d)));
        geoGroup.getFilteredProcesses().add(simplify);

        // densify process
        ProcessInfo densify = new ProcessInfoImpl();
        densify.setEnabled(true);
        densify.setName(new NameImpl("geo", "densify"));
        densify.getValidators().put("distance",
                new NumberRangeValidator(new NumberRange<Double>(Double.class, 0d, null)));
        geoGroup.getFilteredProcesses().add(densify);

        // for the contour process
        ProcessGroupInfo rasterGroup = new ProcessGroupInfoImpl();
        rasterGroup.setFactoryClass(RasterProcessFactory.class);
        rasterGroup.setEnabled(true);
        wps.getProcessGroups().add(rasterGroup);
        ProcessInfo contour = new ProcessInfoImpl();
        contour.setEnabled(true);
        contour.setName(new NameImpl("ras", "Contour"));
        contour.getValidators().put("data", new MaxSizeValidator(1));
        contour.getValidators().put("levels",
                new NumberRangeValidator(new NumberRange<Double>(Double.class, -8000d, 8000d)));
        contour.getValidators().put("levels", new MultiplicityValidator(3));
        rasterGroup.getFilteredProcesses().add(contour);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void testDescribeBuffer() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=geo:buffer");
        // print(d);
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter, geometry
        assertXpathExists(base + "/Input[1]", d);
        assertXpathEvaluatesTo("geom", base + "/Input[1]/ows:Identifier", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);
        assertXpathEvaluatesTo("1", base + "/Input[1]/ComplexData/@maximumMegabytes", d);

        // second parameter (float range)
        assertXpathExists(base + "/Input[2]", d);
        assertXpathEvaluatesTo("distance", base + "/Input[2]/ows:Identifier", d);
        assertXpathExists(base + "/Input[2]/LiteralData", d);
        assertXpathEvaluatesTo("closed", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/@ows:rangeClosure", d);
        assertXpathEvaluatesTo("0.0", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MinimumValue", d);
        assertXpathEvaluatesTo("100.0", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MaximumValue", d);

        // third parameter (integer range)
        assertXpathExists(base + "/Input[3]", d);
        assertXpathEvaluatesTo("quadrantSegments", base + "/Input[3]/ows:Identifier", d);
        assertXpathExists(base + "/Input[3]/LiteralData", d);
        assertXpathEvaluatesTo("closed", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/@ows:rangeClosure", d);
        assertXpathEvaluatesTo("2", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/ows:MinimumValue", d);
        assertXpathEvaluatesTo("20", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/ows:MaximumValue", d);
    }

    @Test
    public void testDescribeSimplify() throws Exception {
        Document d = getAsDOM(root()
                + "service=wps&request=describeprocess&identifier=geo:simplify");
        // print(d);
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter, geometry
        assertXpathExists(base + "/Input[1]", d);
        assertXpathEvaluatesTo("geom", base + "/Input[1]/ows:Identifier", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);
        assertXpathEvaluatesTo("10", base + "/Input[1]/ComplexData/@maximumMegabytes", d);

        // distance, half range
        assertXpathExists(base + "/Input[2]", d);
        assertXpathEvaluatesTo("distance", base + "/Input[2]/ows:Identifier", d);
        assertXpathExists(base + "/Input[2]/LiteralData", d);
        assertXpathEvaluatesTo("open-closed", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/@ows:rangeClosure", d);
        assertXpathNotExists(base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MinimumValue", d);
        assertXpathEvaluatesTo("100.0", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MaximumValue", d);
    }

    @Test
    public void testDescribeDensify() throws Exception {
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=geo:densify");
        // print(d);
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter, geometry
        assertXpathExists(base + "/Input[1]", d);
        assertXpathEvaluatesTo("geom", base + "/Input[1]/ows:Identifier", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);
        assertXpathEvaluatesTo("10", base + "/Input[1]/ComplexData/@maximumMegabytes", d);

        // distance, half range
        assertXpathExists(base + "/Input[2]", d);
        assertXpathEvaluatesTo("distance", base + "/Input[2]/ows:Identifier", d);
        assertXpathExists(base + "/Input[2]/LiteralData", d);
        assertXpathEvaluatesTo("closed-open", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/@ows:rangeClosure", d);
        assertXpathEvaluatesTo("0.0", base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MinimumValue", d);
        assertXpathNotExists(base
                + "/Input[2]/LiteralData/ows:AllowedValues/ows:Range/ows:MaximumValue", d);
    }

    @Test
    public void testDescribeArea() throws Exception {
        // this one has no custom limits, but there is the global limit for the complex input size
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=geo:area");
        // print(d);
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter, geometry
        assertXpathExists(base + "/Input[1]", d);
        assertXpathEvaluatesTo("geom", base + "/Input[1]/ows:Identifier", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);
        assertXpathEvaluatesTo("10", base + "/Input[1]/ComplexData/@maximumMegabytes", d);
    }

    @Test
    public void testDescribeContour() throws Exception {
        // this one has no custom limits, but there is the global limit for the complex input size
        Document d = getAsDOM(root() + "service=wps&request=describeprocess&identifier=ras:Contour");
        // print(d);
        // first off, let's check it's schema compliant
        checkValidationErrors(d);
        assertXpathExists("/wps:ProcessDescriptions", d);

        String base = "/wps:ProcessDescriptions/ProcessDescription/DataInputs";

        // first parameter, the raster
        assertXpathExists(base + "/Input[1]", d);
        assertXpathEvaluatesTo("data", base + "/Input[1]/ows:Identifier", d);
        assertXpathExists(base + "/Input[1]/ComplexData", d);
        assertXpathEvaluatesTo("1", base + "/Input[1]/ComplexData/@maximumMegabytes", d);

        // the levels
        assertXpathExists(base + "/Input[3]", d);
        assertXpathEvaluatesTo("levels", base + "/Input[3]/ows:Identifier", d);
        assertXpathEvaluatesTo("0", base + "/Input[3]/@minOccurs", d);
        assertXpathEvaluatesTo("3", base + "/Input[3]/@maxOccurs", d);
        assertXpathExists(base + "/Input[3]/LiteralData", d);
        assertXpathEvaluatesTo("closed", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/@ows:rangeClosure", d);
        assertXpathEvaluatesTo("-8000.0", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/ows:MinimumValue", d);
        assertXpathEvaluatesTo("8000.0", base
                + "/Input[3]/LiteralData/ows:AllowedValues/ows:Range/ows:MaximumValue", d);

        // first parameter, the roid geometry
        assertXpathExists(base + "/Input[7]", d);
        assertXpathEvaluatesTo("roi", base + "/Input[7]/ows:Identifier", d);
        assertXpathExists(base + "/Input[7]/ComplexData", d);
        assertXpathEvaluatesTo("10", base + "/Input[7]/ComplexData/@maximumMegabytes", d);
    }

    @Test
    public void testBufferLargeInlineGeometry() throws Exception {
        String wkt = buildLineString((Short.MAX_VALUE + 1) * 2 + 1);

        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                + "<ows:Identifier>geo:buffer</ows:Identifier>"
                + "<wps:DataInputs>"
                + "<wps:Input>"
                + "<ows:Identifier>distance</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:LiteralData>1</wps:LiteralData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "<wps:Input>"
                + "<ows:Identifier>geom</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:ComplexData mimeType=\"application/wkt\">"
                + "<![CDATA["
                + wkt
                + "]]>"
                + "</wps:ComplexData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "</wps:DataInputs>"
                + "<wps:ResponseForm>"
                + "<wps:ResponseDocument storeExecuteResponse='false'>"
                + "<wps:Output>"
                + "<ows:Identifier>result</ows:Identifier>"
                + "</wps:Output>"
                + "</wps:ResponseDocument>" + "</wps:ResponseForm>" + "</wps:Execute>";

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        assertXpathEvaluatesTo("geom",
                "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/@locator", dom);
        String message = xp
                .evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("exceeds the maximum allowed"));
        assertTrue(message.contains("geom"));
    }

    @Test
    public void testBufferLargeFileReferenceGeometry() throws Exception {
        // write the large string to a file
        String wkt = buildLineString((Short.MAX_VALUE + 10000) * 2);
        File tmp = getResourceLoader().find("tmp");
        File wktDataFile = new File(tmp, "line.wkt");
        FileUtils.writeStringToFile(wktDataFile, wkt);

        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                + "<ows:Identifier>geo:buffer</ows:Identifier>"
                + "<wps:DataInputs>"
                + "<wps:Input>"
                + "<ows:Identifier>distance</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:LiteralData>1</wps:LiteralData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "<wps:Input>"
                + "<ows:Identifier>geom</ows:Identifier>"
                + "  <wps:Reference mimeType=\"application/wkt\" "
                + "xlink:href=\""
                + DataUtilities.fileToURL(wktDataFile)
                + "\"/>\n"
                + "</wps:Input>"
                + "</wps:DataInputs>"
                + "<wps:ResponseForm>"
                + "<wps:ResponseDocument storeExecuteResponse='false'>"
                + "<wps:Output>"
                + "<ows:Identifier>result</ows:Identifier>"
                + "</wps:Output>"
                + "</wps:ResponseDocument>" + "</wps:ResponseForm>" + "</wps:Execute>";
        FileUtils.writeStringToFile(new File("/tmp/request.xml"), xml);

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        assertXpathEvaluatesTo("geom",
                "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/@locator", dom);
        String message = xp
                .evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("exceeds maximum allowed size"));
        assertTrue(message.contains("geom"));
    }

    @Test
    public void testBufferLargeDistance() throws Exception {
        String wkt = buildLineString(10);

        String xml = "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                + "<ows:Identifier>geo:buffer</ows:Identifier>"
                + "<wps:DataInputs>"
                + "<wps:Input>"
                + "<ows:Identifier>distance</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:LiteralData>200</wps:LiteralData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "<wps:Input>"
                + "<ows:Identifier>geom</ows:Identifier>"
                + "<wps:Data>"
                + "<wps:ComplexData mimeType=\"application/wkt\">"
                + "<![CDATA["
                + wkt
                + "]]>"
                + "</wps:ComplexData>"
                + "</wps:Data>"
                + "</wps:Input>"
                + "</wps:DataInputs>"
                + "<wps:ResponseForm>"
                + "<wps:ResponseDocument storeExecuteResponse='false'>"
                + "<wps:Output>"
                + "<ows:Identifier>result</ows:Identifier>"
                + "</wps:Output>"
                + "</wps:ResponseDocument>" + "</wps:ResponseForm>" + "</wps:Execute>";

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        assertXpathEvaluatesTo("distance",
                "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/@locator", dom);
        String message = xp
                .evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("distance"));
        assertTrue(message.contains("Value 200.0 is out of the valid range [0.0, 100.0]"));
    }

    private String buildLineString(int coordinateCount) {
        // generate a geometry that's more than 1MB worth of data (according to the estimator)
        StringBuilder sb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coordinateCount; i++) {
            sb.append(i).append(" ").append(i);
            if (i < coordinateCount - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        String wkt = sb.toString();
        return wkt;
    }

    @Test
    public void testCountourTooManyLevels() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>ras:Contour</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>data</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                + "            <ows:Identifier>"
                + getLayerId(MockData.TASMANIA_DEM)
                + "</ows:Identifier>\n"
                + "            <wcs:DomainSubset>\n"
                + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                + "                <ows:LowerCorner>-180 -90</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>180 90</ows:UpperCorner>\n"
                + "              </gml:BoundingBox>\n"
                + "            </wcs:DomainSubset>\n"
                + "            <wcs:Output format=\"image/tiff\"/>\n"
                + "          </wcs:GetCoverage>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>levels</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>0</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>levels</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>10</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>levels</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>20</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>levels</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>30</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n" + "</wps:Execute>";

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);

        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        assertXpathEvaluatesTo("levels",
                "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/@locator", dom);
        String message = xp
                .evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("levels"));
        assertTrue(message.contains("Too many values"));
    }

    @Test
    public void testCountourLevelsOutOfRange() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>ras:Contour</ows:Identifier>\n"
                + "  <wps:DataInputs>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>data</ows:Identifier>\n"
                + "      <wps:Reference mimeType=\"image/tiff\" xlink:href=\"http://geoserver/wcs\" method=\"POST\">\n"
                + "        <wps:Body>\n"
                + "          <wcs:GetCoverage service=\"WCS\" version=\"1.1.1\">\n"
                + "            <ows:Identifier>"
                + getLayerId(MockData.TASMANIA_DEM)
                + "</ows:Identifier>\n"
                + "            <wcs:DomainSubset>\n"
                + "              <gml:BoundingBox crs=\"http://www.opengis.net/gml/srs/epsg.xml#4326\">\n"
                + "                <ows:LowerCorner>-180 -90</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>180 90</ows:UpperCorner>\n"
                + "              </gml:BoundingBox>\n"
                + "            </wcs:DomainSubset>\n"
                + "            <wcs:Output format=\"image/tiff\"/>\n"
                + "          </wcs:GetCoverage>\n"
                + "        </wps:Body>\n"
                + "      </wps:Reference>\n"
                + "    </wps:Input>\n"
                + "    <wps:Input>\n"
                + "      <ows:Identifier>levels</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>10000</wps:LiteralData>\n"
                + "      </wps:Data>\n"
                + "    </wps:Input>\n"
                + "  </wps:DataInputs>\n"
                + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput mimeType=\"text/xml; subtype=wfs-collection/1.0\">\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n"
                + "  </wps:ResponseForm>\n" + "</wps:Execute>";

        Document dom = postAsDOM("wps", xml);
        // print(dom);
        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);

        assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        assertXpathEvaluatesTo("levels",
                "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/@locator", dom);
        String message = xp
                .evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("levels"));
        assertTrue(message.contains(" Value 10000.0 is out of the valid range [-8000.0, 8000.0]"));
    }

    @Test
    public void testAsyncExecutionLimits() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxAsynchronousExecutionTime(2);
        getGeoServer().save(wps);

        // submit asynch request with no updates
        String request = "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&storeExecuteResponse=true&DataInputs="
                + urlEncode("id=x2");
        Document dom = getAsDOM(request);
        assertXpathExists("//wps:ProcessAccepted", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String fullStatusLocation = xpath.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);

        // wait for end, pinging the process to make it fail
        dom = waitForProcessEnd(statusLocation, 60, new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                // schedule an update that will make it fail. Same progress every time,
                // happily for the moment we don't have optimizations that would
                // make this repeated pings be ignored
                MonkeyProcess.progress("x2", 50f, true);
                Thread.sleep(100);
                return null;
            }

        });
        // print(dom);
        XMLAssert.assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        String message = xpath.evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("went beyond the configured limits of 2 seconds"));
    }

    @Test
    public void testSyncExecutionLimits() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        wps.setMaxSynchronousExecutionTime(1);
        getGeoServer().save(wps);

        // setup the set of commands for the monkey process
        MonkeyProcess.wait("x2", 2000);
        MonkeyProcess.progress("x2", 50f, false);

        // submit synch request
        String request = "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&DataInputs="
                + urlEncode("id=x2");
        Document dom = getAsDOM(request);
        // print(dom);
        XMLAssert.assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);
        assertTrue(message.contains("went beyond the configured limits of 1 seconds"));
    }

    String urlEncode(String string) throws Exception {
        return URLEncoder.encode(string, "ASCII");
    }

}
