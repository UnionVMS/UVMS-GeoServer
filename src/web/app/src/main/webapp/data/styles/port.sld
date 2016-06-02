<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
		<sld:UserStyle>
			<sld:Name>port</sld:Name>
			<sld:Title/>
			<sld:FeatureTypeStyle>
                <sld:Name>PORT</sld:Name>
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
					<sld:MinScaleDenominator>9500000</sld:MinScaleDenominator>
                    <sld:PointSymbolizer>
						<sld:Graphic>
							<sld:Mark>
								<sld:WellKnownName>square</sld:WellKnownName>
								<sld:Fill>
									<sld:CssParameter name="fill">#306bb7</sld:CssParameter>
								</sld:Fill>
								<sld:Stroke>
									<sld:CssParameter name="stroke">#ffffff</sld:CssParameter>
									<sld:CssParameter name="stroke-weidth">1.0</sld:CssParameter>
								</sld:Stroke>
							</sld:Mark>
							<sld:Size>5</sld:Size>
							<sld:Rotation>45</sld:Rotation>
						</sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
				<sld:Rule>
                    <sld:Name>Fill Rule</sld:Name>
					<ogc:Filter>
						<ogc:PropertyIsEqualTo>
							<ogc:PropertyName>enabled</ogc:PropertyName>
							<ogc:Literal>Y</ogc:Literal>
						</ogc:PropertyIsEqualTo>
					</ogc:Filter>
					<sld:MaxScaleDenominator>9500000</sld:MaxScaleDenominator>
                    <sld:PointSymbolizer>
						<sld:Graphic>
							<sld:Mark>
								<sld:WellKnownName>square</sld:WellKnownName>
								<sld:Fill>
									<sld:CssParameter name="fill">#306bb7</sld:CssParameter>
								</sld:Fill>
								<sld:Stroke>
									<sld:CssParameter name="stroke">#ffffff</sld:CssParameter>
									<sld:CssParameter name="stroke-weidth">1.0</sld:CssParameter>
								</sld:Stroke>
							</sld:Mark>
							<sld:Size>8</sld:Size>
							<sld:Rotation>45</sld:Rotation>
						</sld:Graphic>
                    </sld:PointSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
		</sld:UserStyle>
	</sld:UserLayer>
</sld:StyledLayerDescriptor>