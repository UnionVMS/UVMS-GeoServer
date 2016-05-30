package org.geoserver.wcs2_0.xml;

import static junit.framework.TestCase.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geoserver.wcs2_0.WCSTestSupport;
import org.geoserver.wcs2_0.response.GMLCoverageResponseDelegate;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
/**
 * Testing {@link GMLCoverageResponseDelegate}
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GMLGetCoverageTest extends WCSTestSupport {

   
    @Test 
    public void testGMLExtension() throws Exception {
        final File xml= new File("./src/test/resources/requestGetCoverageGML.xml");
        final String request= FileUtils.readFileToString(xml);

        MockHttpServletResponse response = postAsServletResponse("wcs", request);
        
        assertEquals("application/gml+xml", response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getOutputStreamContent().getBytes()));     
//        print(dom);
        
        // validate
//        checkValidationErrors(dom, WCS20_SCHEMA);
        
        // check it is good
//        assertXpathEvaluatesTo("wcs__BlueMarble", "//wcs:CoverageDescription//wcs:CoverageId", dom);
//        assertXpathEvaluatesTo("3", "count(//wcs:CoverageDescription//gmlcov:rangeType//swe:DataRecord//swe:field)", dom);

    }
}
