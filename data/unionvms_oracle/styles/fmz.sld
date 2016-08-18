<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>fmz</sld:Name>
            <sld:Title/>
            <sld:FeatureTypeStyle>
                <sld:Name>FMZ</sld:Name>
                <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
                <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
                <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
                <sld:Rule>
                    <sld:Name>Fill Rule</sld:Name>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>ENABLED</ogc:PropertyName>
							<ogc:Literal>Y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
						<sld:Fill>
							<sld:GraphicFill>
								<sld:Graphic>
									<sld:Mark>
										<sld:WellKnownName>shape://dot</sld:WellKnownName>
										<sld:Fill/>
										<sld:Stroke>
											<sld:CssParameter name="stroke">#00906a</sld:CssParameter>
										</sld:Stroke>
									</sld:Mark>
									<sld:Size>5</sld:Size>
								</sld:Graphic>
							</sld:GraphicFill>
						</sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#00906a</sld:CssParameter>
							<sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>