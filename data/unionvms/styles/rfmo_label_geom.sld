<?xml version="1.0" encoding="UTF-8"?>
<sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:UserLayer>
        <sld:LayerFeatureConstraints>
            <sld:FeatureTypeConstraint/>
        </sld:LayerFeatureConstraints>
        <sld:UserStyle>
            <sld:Name>rfmo</sld:Name>
            <sld:Title/>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>CCAMLR</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>CCAMLR</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#9ac24d</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#9ac24d</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>CCBSP</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>CCBSP</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#4dc2a9</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#4dc2a9</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>ICCAT</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>ICCAT</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#ffd380</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#ffd380</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>SIOFA</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>SIOFA</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#da8e6b</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#da8e6b</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>WCPFC</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>WCPFC</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#47c079</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#47c079</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>SPRFMO</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>SPRFMO</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
							<sld:CssParameter name="fill">#e99eff</sld:CssParameter>
							<sld:CssParameter name="fill-opacity">0.5</sld:CssParameter>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#e99eff</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>IATTC</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>IATTC</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://slash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#c2734d</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>15.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#c2734d</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
            <sld:FeatureTypeStyle>
                <sld:Rule>
                    <sld:Name>AIDCP</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>AIDCP</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://slash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#ebec4d</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#ebec4d</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>CCSBT</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>CCSBT</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
                    <sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://backslash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#e79dff</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#e79dff</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
			</sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>GFCM</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>GFCM</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://slash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#9f2aff</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#9f2aff</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>IOTC</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>IOTC</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://backslash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#e35355</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>15.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#e35355</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>NASCO</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>NASCO</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://slash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#73c24d</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#73c24d</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>NEAFC</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>NEAFC</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://horline</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#abab8e</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#abab8e</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>NAFO</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>NAFO</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://vertline</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#ff7f00</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#ff7f00</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
                    <sld:Name>SEAFO</sld:Name>
					<ogc:Filter>
						<ogc:And>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>enabled</ogc:PropertyName>
								<ogc:Literal>Y</ogc:Literal>
							</ogc:PropertyIsEqualTo>
							<ogc:PropertyIsEqualTo>
								<ogc:PropertyName>code</ogc:PropertyName>
								<ogc:Literal>SEAFO</ogc:Literal>
							</ogc:PropertyIsEqualTo>
						</ogc:And>						
					</ogc:Filter>
					<sld:PolygonSymbolizer>
                        <sld:Fill>
                            <sld:GraphicFill>
                                <sld:Graphic>
                                    <sld:Mark>
                                        <sld:WellKnownName>shape://slash</sld:WellKnownName>
                                        <sld:Fill/>
                                        <sld:Stroke>
                                            <sld:CssParameter name="stroke">#4d9ded</sld:CssParameter>
                                        </sld:Stroke>
                                    </sld:Mark>
                                    <sld:Size>10.0</sld:Size>
                                </sld:Graphic>
                            </sld:GraphicFill>
                        </sld:Fill>
                        <sld:Stroke>
                            <sld:CssParameter name="stroke">#4d9ded</sld:CssParameter>
                            <sld:CssParameter name="stroke-width">2.0</sld:CssParameter>
                        </sld:Stroke>
                    </sld:PolygonSymbolizer>
                </sld:Rule>
            </sld:FeatureTypeStyle>
			<sld:FeatureTypeStyle>
				<sld:Rule>
					<sld:TextSymbolizer>
						<sld:Geometry>
							<ogc:Function name="centroid">
								<ogc:PropertyName>geom</ogc:PropertyName>
							</ogc:Function>
						</sld:Geometry>
                        <sld:Label>
                            <ogc:PropertyName>code</ogc:PropertyName>
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