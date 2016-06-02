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
					<sld:TextSymbolizer>
                        <sld:Label>
                            <ogc:PropertyName>name</ogc:PropertyName><![CDATA[
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
                                    <sld:DisplacementY>10.0</sld:DisplacementY>
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
				<sld:Rule>
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
					<sld:TextSymbolizer>
                        <sld:Label>
                            <ogc:PropertyName>name</ogc:PropertyName><![CDATA[
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
                                    <sld:DisplacementY>10.0</sld:DisplacementY>
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