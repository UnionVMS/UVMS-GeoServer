/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;

import org.w3c.dom.Document;

/**
 * Test feature chaining with simple content type, e.g. for gml:name.
 * 
 * @author Rini Angreani, CSIRO Earth Science and Resource Engineering
 */
public class SimpleAttributeFeatureChainWfsTest extends AbstractAppSchemaTestSupport {

    @Override
    protected SimpleAttributeFeatureChainMockData createTestData() {
        return new SimpleAttributeFeatureChainMockData();
    }

    /**
     * Test that feature chaining for gml:name works.
     */
    @Test
    public void testGetFeature() {
        String path = "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature";
        Document doc = getAsDOM(path);
        LOGGER.info("MappedFeature with name feature chained Response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        checkMf1(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "133.8855 -23.6701",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf2: extra values from denormalised tables
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf3
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // mf4
        checkMf4(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.3 53.5 -1.3 53.6 -1.2 53.6 -1.2 52.5 -1.3 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    /**
     * Test reprojecting with feature chained geometry.
     */
    @Test
    public void testReprojecting() {
        String path = "wfs?request=GetFeature&version=1.1.0&typeName=gsml:MappedFeature&srsName=EPSG:4326";
        Document doc = getAsDOM(path);
        LOGGER.info("Reprojected MappedFeature with name feature chained Response:\n"
                + prettyString(doc));
        assertXpathEvaluatesTo("4", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(4, "//gsml:MappedFeature", doc);

        // mf1
        checkMf1(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "133.8855 -23.6701",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf2: extra values from denormalised tables
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        // mf3
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.50000003577337 -1.2000000000000002 53.60000003565695 -1.1 53.60000003565695 -1.1 53.50000003577337 -1.2 53.50000003577337",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // mf4
        checkMf4(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2999999999999998 53.50000003577337 -1.3 53.60000003565695 -1.2000000000000002 53.60000003565695 -1.2 52.50000003687648 -1.2999999999999998 53.50000003577337",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    /**
     * Test that filtering feature chained values works.
     */
    @Test
    public void testAttributeFilter() {
        // filter by name
        String xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 4</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 3</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);

        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">" //
                + "        <ogc:Filter>" //
                + "            <ogc:PropertyIsEqualTo>" //
                + "                <ogc:PropertyName>gml:name</ogc:PropertyName>" //
                + "                <ogc:Literal>nametwo 2</ogc:Literal>" //
                + "            </ogc:PropertyIsEqualTo>" //
                + "        </ogc:Filter>" //
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));

        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf2(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "167.9388 -29.0434",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4326",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/@srsName",
                doc);
    }

    /**
     * Test filtering client properties.
     */
    @Test
    public void testClientPropertiesFilter() {
        // filter by codespace coming from parent table
        String xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES
                + ">" //
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                + "                <ogc:Literal>some:uri:mf3</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // filter by codespace coming from chained feature
        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">"
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:PropertyName>gml:name/@codeSpace</ogc:PropertyName>"
                + "                <ogc:Literal>some uri 4</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);

        // filter by xlink:href coming from chained feature
        xml = //
        "<wfs:GetFeature " //
                + FeatureChainingWfsTest.GETFEATURE_ATTRIBUTES //
                + ">"
                + "    <wfs:Query typeName=\"gsml:MappedFeature\">"
                + "        <ogc:Filter>"
                + "            <ogc:PropertyIsEqualTo>"
                + "                <ogc:PropertyName>gml:name/@xlink:href</ogc:PropertyName>"
                + "                <ogc:Literal>some:uri:4</ogc:Literal>"
                + "            </ogc:PropertyIsEqualTo>"
                + "        </ogc:Filter>"
                + "    </wfs:Query> " //
                + "</wfs:GetFeature>";
        doc = postAsDOM("wfs", xml);
        LOGGER.info("WFS filter GetFeature response:\n" + prettyString(doc));
        assertXpathEvaluatesTo("1", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(1, "//gsml:MappedFeature", doc);
        checkMf3(doc);
        // chaining geometry
        assertXpathEvaluatesTo(
                "-1.2 53.5 -1.2 53.6 -1.1 53.6 -1.1 53.5 -1.2 53.5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4283",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Polygon/@srsName",
                doc);
    }

    private void checkMf1(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("GUNTHORPE FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:1",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gml:name[4]/@xlink:href",
                doc);
    }

    private void checkMf2(Document doc) {
        // mf2: extra values from denormalised tables
        assertXpathCount(7, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("MERCIA MUDSTONE GROUP",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[3]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[4]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[5]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:2",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[6]/@xlink:href",
                doc);
        assertXpathEvaluatesTo("some:uri:3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gml:name[7]/@xlink:href",
                doc);
    }

    private void checkMf3(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("CLIFTON FORMATION",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf3",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gml:name[4]/@xlink:href",
                doc);
    }

    private void checkMf4(Document doc) {
        assertXpathCount(4, "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name", doc);
        // gml:name with values coming from the main table
        assertXpathEvaluatesTo("MURRADUC BASALT",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[1]", doc);
        // gml:name with values coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("nameone 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]", doc);
        // client property coming from another table(MappedFeatureNameOne)
        assertXpathEvaluatesTo("some uri 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[2]/@codeSpace",
                doc);
        // gml:name with values coming from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("nametwo 5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]", doc);
        // client property coming from the parent table
        assertXpathEvaluatesTo("some:uri:mf4",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[3]/@codeSpace",
                doc);
        // gml:name as xlink:href from another table(MappedFeatureNameTwo)
        assertXpathEvaluatesTo("some:uri:5",
                "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gml:name[4]/@xlink:href",
                doc);
    }

}
