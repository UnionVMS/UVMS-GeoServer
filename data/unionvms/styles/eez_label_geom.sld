<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>eez</sld:Name>
            <sld:Title/>
            <sld:FeatureTypeStyle>
                <sld:Name>EEZ</sld:Name>
                <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
                <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
                <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
                <sld:Rule>
                    <sld:Name>Fill Rule</sld:Name>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>enabled</ogc:PropertyName>
							<ogc:Literal>Y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#F9AA0F</sld:CssParameter>
							<sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
					<sld:TextSymbolizer>
						<sld:Geometry>
							<ogc:Function name="interiorPoint">
								<ogc:PropertyName>geom</ogc:PropertyName>
							</ogc:Function>
						</sld:Geometry>
                        <sld:Label>
                            <ogc:PropertyName>sovereign</ogc:PropertyName><![CDATA[
							]]> [<ogc:PropertyName>code</ogc:PropertyName>]
                        </sld:Label>
                        <sld:Font>
                            <sld:CssParameter name="font-family">SansSerif</sld:CssParameter>
                            <sld:CssParameter name="font-size">10.0</sld:CssParameter>
                            <sld:CssParameter name="font-style">normal</sld:CssParameter>
                            <sld:CssParameter name="font-weight">bold</sld:CssParameter>
                        </sld:Font>
                        <sld:LabelPlacement>
                            <sld:PointPlacement>
                                <sld:AnchorPoint>
                                    <sld:AnchorPointX>0.5</sld:AnchorPointX>
                                    <sld:AnchorPointY>0.0</sld:AnchorPointY>
                                </sld:AnchorPoint>
                                <sld:Displacement>
                                    <sld:DisplacementX>0.0</sld:DisplacementX>
                                    <sld:DisplacementY>0.0</sld:DisplacementY>
                                </sld:Displacement>
                            </sld:PointPlacement>
                        </sld:LabelPlacement>
                        <sld:Halo>
                            <sld:Radius>2</sld:Radius>
                            <sld:Fill>
                                <sld:CssParameter name="fill">#FFFFFF</sld:CssParameter>
                            </sld:Fill>
                        </sld:Halo>
                        <sld:Fill>
                            <sld:CssParameter name="fill">#575757</sld:CssParameter>
                        </sld:Fill>
						<VendorOption name="conflictResolution">true</VendorOption>
						<VendorOption name="maxDisplacement">20</VendorOption>
                    </sld:TextSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>