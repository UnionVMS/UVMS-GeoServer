/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;

import org.geoserver.wms.GetCapabilitiesRequest;
import org.geoserver.util.EntityResolverProvider;
import org.junit.Test;

/**
 * 
 * @author Gabriel Roldan
 */
public class CapabilitiesXmlReaderTest {

    @Test
    public void testParseXmlGetCapabilities() throws Exception {
        CapabilitiesXmlReader reader = new CapabilitiesXmlReader(
                EntityResolverProvider.RESOLVE_DISABLED_PROVIDER);

        String plainRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //
                + "<ogc:GetCapabilities xmlns:ogc=\"http://www.opengis.net/ows\" " //
                + "         xmlns:gml=\"http://www.opengis.net/gml\" " //
                + "         version=\"1.2.0\" updateSequence=\"1\" " //
                + "        service=\"WMS\"> " //
                + "</ogc:GetCapabilities>";

        Reader input = new StringReader(plainRequest);

        Object read = reader.read(null, input, null);
        assertTrue(read instanceof GetCapabilitiesRequest);

        GetCapabilitiesRequest request = (GetCapabilitiesRequest) read;
        assertEquals("GetCapabilities", request.getRequest());
        assertEquals("1.2.0", request.getVersion());
        assertEquals("1", request.getUpdateSequence());
    }
}
