.. _community_sldservice:

SLD REST Service
================

The SLD Service is a GeoServer REST service that can be used to create SLD styles on published GeoServer
layers doing a classification on the layer data, following user provided directives.

The purpose of the service is to allow clients to dinamically publish data and create simple styles on it.

All the services are published under the common prefix ``/rest/sldservice/{layer}``, where **layer** is 
the layer to classify/query.

Query Vector Data Attributes
----------------------------
``/attributes[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Gets the list of attributes for the given layer (of vector type)
     - 200
     - HTML, XML, JSON
     - HTML

The service can be used to get the attributes list for the given vector layer.
This can be used by a client as a prerequisite for the **classify** service, to
get all the attributes usable for classification and let the user choose one.

Examples
~~~~~~~~~~
Get attributes for the states layer, in XML format
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/attributes.xml
          
.. code-block:: xml

    <Attributes layer="states">
      <Attribute>
        <name>P_FEMALE</name>
        <type>Double</type>
      </Attribute>
      <Attribute>
        <name>HOUSHOLD</name>
        <type>Double</type>
      </Attribute>
      <Attribute>
        <name>SERVICE</name>
        <type>Double</type>
      </Attribute>
      ...
    </Attributes>

Get attributes for the states layer, in JSON format
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/attributes.json
          
.. code-block:: javascript

    {  
       "Attributes":{  
          "@layer":"states",
          "Attribute":[  
             {  
                "name":"P_FEMALE",
                "type":"Double"
             },
             {  
                "name":"HOUSHOLD",
                "type":"Double"
             },
             {  
                "name":"SERVICE",
                "type":"Double"
             },
             ...
          ]
       }
    }
    
Classify Vector Data
--------------------
``/classify[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Create a set of SLD Rules for the given layer (of vector type)
     - 200
     - HTML, XML, JSON
     - HTML

The service can be used to create a set of SLD rules for the given vector
layer, specyfing the **attribute** used for classification, the  **classification 
type** (equalInterval, uniqueInterval, quantile, jenks) and one of the
**predefined color ranges** (red, blue, gray, jet, random, custom), together
with some other optional parameters.

Using the **CUSTOM** ColorMap, startColor and endColor (and optionally midColor)
have to be specified.

The parameters usable to customize the ColorMap are:

.. list-table::
   :header-rows: 1

   * - Parameter
     - Description
     - Values
     - Default Value
   * - intervals
     - Number of intervals (rules) for the SLD
     - integer numeric value
     - 2
   * - attribute (mandatory)
     - Classification attribute
     - one of the layer attribute names
     - 
   * - method
     - Classification method
     - equalInterval, uniqueInterval, quantile, jenks
     - equalInterval
   * - open
     - open or closed ranges
     - true, false
     - false
   * - reverse
     - normal or inverted ranges
     - true, false
     - false
   * - normalize
     - normalize (cast) attribute to double type (needed by some stores to handle integer types correctly)
     - true, false
     - false
   * - ramp (mandatory)
     - color ranges to use
     - red, blue, gray, jet, random, custom
     - 
   * - startColor
     - starting color for the custom ramp
     - 
     - 
   * - endColor
     - ending color for the custom ramp
     - 
     - 
   * - midColor
     - central color for the custom ramp
     - 
     -
    
Examples
~~~~~~~~~~
A default (equalInterval) classification on the states layer LAND_KM attribute using 
a red based color range.
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/classify.xml?attribute=LAND_KM&ramp=red
          
.. code-block:: xml
    
    <Rules>
      <Rule>
        <Title> &gt; 159.1 AND &lt;= 344189.1</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThanOrEqualTo>
              <PropertyName>LAND_KM</PropertyName>
              <Literal>159.1</Literal>
            </PropertyIsGreaterThanOrEqualTo>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>LAND_KM</PropertyName>
              <Literal>344189.1</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#680000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title> &gt; 344189.1 AND &lt;= 688219.2</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThan>
              <PropertyName>LAND_KM</PropertyName>
              <Literal>344189.1</Literal>
            </PropertyIsGreaterThan>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>LAND_KM</PropertyName>
              <Literal>688219.2</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#B20000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
    </Rules>
    
A uniqueInterval classification on the states layer SUB_REGION attribute using 
a red based color range.
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/classify.xml?attribute=SUB_REGION&ramp=red&method=uniqueInterval
          
.. code-block:: xml
    
    <Rules>
      <Rule>
        <Title>E N Cen</Title>
        <Filter>
          <PropertyIsEqualTo>
            <PropertyName>SUB_REGION</PropertyName>
            <Literal>E N Cen</Literal>
          </PropertyIsEqualTo>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#330000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title>E S Cen</Title>
        <Filter>
          <PropertyIsEqualTo>
            <PropertyName>SUB_REGION</PropertyName>
            <Literal>E S Cen</Literal>
          </PropertyIsEqualTo>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#490000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      ...
    </Rules>
    
A uniqueInterval classification on the states layer SUB_REGION attribute using 
a red based color range and 3 intervals.

 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/classify.xml?attribute=SUB_REGION&ramp=red&method=uniqueInterval&intervals=3
          
.. code-block:: xml
    
    <string>Intervals: 9</string>

A quantile classification on the states layer PERSONS attribute with a custom
color ramp and 3 **closed** intervals.
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/classify.xml?attribute=PERSONS&ramp=CUSTOM&method=quantile&intervals=3&startColor=0xFF0000&endColor=0x0000FF
          
.. code-block:: xml
    
    <Rules>
      <Rule>
        <Title> &gt; 453588.0 AND &lt;= 2477574.0</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThanOrEqualTo>
              <PropertyName>PERSONS</PropertyName>
              <Literal>453588.0</Literal>
            </PropertyIsGreaterThanOrEqualTo>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>PERSONS</PropertyName>
              <Literal>2477574.0</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#FF0000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title> &gt; 2477574.0 AND &lt;= 4866692.0</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThan>
              <PropertyName>PERSONS</PropertyName>
              <Literal>2477574.0</Literal>
            </PropertyIsGreaterThan>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>PERSONS</PropertyName>
              <Literal>4866692.0</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#AA0055</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title> &gt; 4866692.0 AND &lt;= 2.9760021E7</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThan>
              <PropertyName>PERSONS</PropertyName>
              <Literal>4866692.0</Literal>
            </PropertyIsGreaterThan>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>PERSONS</PropertyName>
              <Literal>2.9760021E7</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#5500AA</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
    </Rules>
    
A quantile classification on the states layer PERSONS attribute with a custom
color ramp and 3 **open** intervals.
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/states/classify.xml?attribute=PERSONS&ramp=CUSTOM&method=quantile&intervals=3&startColor=0xFF0000&endColor=0x0000FF&open=true
          
.. code-block:: xml
    
    <Rules>
      <Rule>
        <Title> &lt;= 2477574.0</Title>
        <Filter>
          <PropertyIsLessThanOrEqualTo>
            <PropertyName>PERSONS</PropertyName>
            <Literal>2477574.0</Literal>
          </PropertyIsLessThanOrEqualTo>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#FF0000</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title> &gt; 2477574.0 AND &lt;= 4866692.0</Title>
        <Filter>
          <And>
            <PropertyIsGreaterThan>
              <PropertyName>PERSONS</PropertyName>
              <Literal>2477574.0</Literal>
            </PropertyIsGreaterThan>
            <PropertyIsLessThanOrEqualTo>
              <PropertyName>PERSONS</PropertyName>
              <Literal>4866692.0</Literal>
            </PropertyIsLessThanOrEqualTo>
          </And>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#AA0055</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
      <Rule>
        <Title> &gt; 4866692.0</Title>
        <Filter>
          <PropertyIsGreaterThan>
            <PropertyName>PERSONS</PropertyName>
            <Literal>4866692.0</Literal>
          </PropertyIsGreaterThan>
        </Filter>
        <PolygonSymbolizer>
          <Fill>
            <CssParameter name="fill">#5500AA</CssParameter>
          </Fill>
          <Stroke/>
        </PolygonSymbolizer>
      </Rule>
    </Rules>

    
Classify Raster Data
--------------------
``/rasterize[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Create a ColorMap SLD for the given layer (of coverage type)
     - 200
     - HTML, XML, JSON, SLD
     - HTML

The service can be used to create a ColorMap SLD for the given coverage,
specyfing the **type of ColorMap** (VALUES, INTERVALS, RAMP) and one of the
**predefined color ranges** (RED, BLUE, GRAY, JET, RANDOM, CUSTOM).

Using the **CUSTOM** ColorMap, startColor and endColor (and optionally midColor)
have to be specified.

The parameters usable to customize the ColorMap are:

.. list-table::
   :header-rows: 1

   * - Parameter
     - Description
     - Values
     - Default Value
   * - min
     - Minimum value for classification
     - double numeric value
     - 0.0
   * - max
     - Maximum value for classification
     - double numeric value
     - 100.0
   * - classes
     - Number of classes for the created map
     - integer numeric value
     - 100
   * - digits
     - Number of fractional digits for class limits (in labels)
     - integer numeric value
     - 5
   * - type
     - ColorMap type
     - INTERVALS, VALUES, RAMP
     - RAMP
   * - ramp
     - ColorMap color ranges
     - RED, BLUE, GRAY, JET, RANDOM, CUSTOM
     - RED
   * - startColor
     - starting color for the CUSTOM ramp
     - 
     - 
   * - endColor
     - ending color for the CUSTOM ramp
     - 
     - 
   * - midColor
     - central color for the CUSTOM ramp
     - 
     - 

Examples
~~~~~~~~~~
A RED color ramp with 5 classes
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/sfdem/rasterize.sld?min=0&max=100&classes=5&type=RAMP&ramp=RED&digits=1
          
.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
        <sld:NamedLayer>
            <sld:Name>Default Styler</sld:Name>
            <sld:UserStyle>
                <sld:Name>Default Styler</sld:Name>
                <sld:FeatureTypeStyle>
                    <sld:Name>name</sld:Name>
                    <sld:FeatureTypeName>gray</sld:FeatureTypeName>
                    <sld:Rule>
                        <sld:RasterSymbolizer>
                            <sld:ColorMap>
                                <sld:ColorMapEntry color="#000000" opacity="0" quantity="-1.0E-9" label="transparent"/>
                                <sld:ColorMapEntry color="#420000" opacity="1.0" quantity="0.0" label="0.0"/>
                                <sld:ColorMapEntry color="#670000" opacity="1.0" quantity="25.0" label="25.0"/>
                                <sld:ColorMapEntry color="#8B0000" opacity="1.0" quantity="50.0" label="50.0"/>
                                <sld:ColorMapEntry color="#B00000" opacity="1.0" quantity="75.0" label="75.0"/>
                                <sld:ColorMapEntry color="#D40000" opacity="1.0" quantity="100.0" label="100.0"/>
                            </sld:ColorMap>
                        </sld:RasterSymbolizer>
                    </sld:Rule>
                </sld:FeatureTypeStyle>
            </sld:UserStyle>
        </sld:NamedLayer>
    </sld:StyledLayerDescriptor>

        
A CUSTOM color ramp with 5 classes, with colors ranging from RED (0xFF0000) to BLUE (0x0000FF).
 
.. code-block:: console

   curl -v -u admin:geoserver -XGET 
     http://localhost:8080/geoserver/rest/sldservice/sfdem/rasterize.sld?min=0&max=100&classes=5&type=RAMP&ramp=CUSTOM&digits=1&startColor=0xFF0000&endColor=0x0000FF
          
.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8"?>
    <sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
        <sld:NamedLayer>
            <sld:Name>Default Styler</sld:Name>
            <sld:UserStyle>
                <sld:Name>Default Styler</sld:Name>
                <sld:FeatureTypeStyle>
                    <sld:Name>name</sld:Name>
                    <sld:FeatureTypeName>gray</sld:FeatureTypeName>
                    <sld:Rule>
                        <sld:RasterSymbolizer>
                            <sld:ColorMap>
                                <sld:ColorMapEntry color="#000000" opacity="0" quantity="-1.0E-9" label="transparent"/>
                                <sld:ColorMapEntry color="#FF0000" opacity="1.0" quantity="0.0" label="0.0"/>
                                <sld:ColorMapEntry color="#CC0033" opacity="1.0" quantity="25.0" label="25.0"/>
                                <sld:ColorMapEntry color="#990066" opacity="1.0" quantity="50.0" label="50.0"/>
                                <sld:ColorMapEntry color="#660099" opacity="1.0" quantity="75.0" label="75.0"/>
                                <sld:ColorMapEntry color="#3300CC" opacity="1.0" quantity="100.0" label="100.0"/>
                            </sld:ColorMap>
                        </sld:RasterSymbolizer>
                    </sld:Rule>
                </sld:FeatureTypeStyle>
            </sld:UserStyle>
        </sld:NamedLayer>
    </sld:StyledLayerDescriptor>
 