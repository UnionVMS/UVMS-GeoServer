/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static junit.framework.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.ows10.GetCapabilitiesType;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.eclipse.emf.common.util.EList;
import org.geoserver.csw.kvp.GetCapabilitiesKvpRequestReader;
import org.geoserver.csw.xml.v2_0_2.CSWXmlReader;
import org.geoserver.ows.xml.v1_0.OWS;
import org.geoserver.platform.ServiceException;
import org.geotools.csw.CSWConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXParseException;

public class GetCapabilitiesDecoratorTest extends CSWSimpleTestSupport {

	static XpathEngine xpath = XMLUnit.newXpathEngine();

	@Override
	protected void setUpSpring(List<String> springContextLocations) {
		super.setUpSpring(springContextLocations);
		springContextLocations
				.add("classpath*:/capabilitiesDecoratorApplicationContext.xml");
	}

	@Test
	public void testRepeatedCapabilitiesCall() throws Exception {
		// repeat calls, make sure there is no accumulation of the text/xml extra format
		for (int i = 0; i < 2; i++) {
			Document dom = getAsDOM(BASEPATH
					+ "?service=csw&version=2.0.2&request=GetCapabilities");
			// print(dom);
			checkValidationErrors(dom);

			// get to the GetRecord operation, check we have what we expect
			assertEquals(
					"2",
					xpath.evaluate(
							"count(//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='outputFormat']/ows:Value)",
							dom));
			assertXpathExists(
					"//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='outputFormat' and ows:Value='application/xml']",
					dom);
			assertXpathExists(
					"//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='outputFormat' and ows:Value='text/xml']",
					dom);
		}
	}
}
