<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>statrect</sld:Name>
            <sld:Title/>
            <sld:FeatureTypeStyle>
                <sld:Name>statistical rectangle</sld:Name>
                <sld:FeatureTypeName>Feature</sld:FeatureTypeName>
                <sld:SemanticTypeIdentifier>generic:geometry</sld:SemanticTypeIdentifier>
                <sld:SemanticTypeIdentifier>simple</sld:SemanticTypeIdentifier>
                <sld:Rule>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>enabled</ogc:PropertyName>
							<ogc:Literal>Y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:MaxScaleDenominator>9500000</sld:MaxScaleDenominator>
                    <sld:PolygonSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#aaaaaa</sld:CssParameter>
							<sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
				<sld:Rule>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>enabled</ogc:PropertyName>
							<ogc:Literal>Y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:MinScaleDenominator>9500000</sld:MinScaleDenominator>
                    <sld:PolygonSymbolizer>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#aaaaaa</sld:CssParameter>
							<sld:CssParameter name="stroke-width">1.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
        </sld:UserStyle>
    </sld:UserLayer>
</sld:StyledLayerDescriptor>