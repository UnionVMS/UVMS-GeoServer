/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.NumberRange;

/**
 * Provides utility methods required to build the capabilities document.
 * 
 * @author Mauricio Pazos
 *
 */
final class CapabilityUtil {
	
	private CapabilityUtil(){
		//utility class
	}
	
	/**
	 * Searches the Max and Min scale denominators in the layer's styles.  
	 * 
	 * <pre>
	 * If the Min or Max values aren't present, the following default are assumed:
	 * 
	 * Min Scale: 0.0
	 * Max Scale: infinity
	 * </pre> 
	 * @param minScaleDenominator Min scale attribute (or element) name
	 * @param maxScaleDenominator Max scale attribute (or element) name
	 * @param layer
	 * 
	 * @return Max and Min denominator
	 * @throws IOException 
	 */
	public static NumberRange<Double> searchMinMaxScaleDenominator(final LayerInfo layer) 
		throws IOException{

		Set<StyleInfo> stylesCopy;
		StyleInfo defaultStyle;
		synchronized (layer) {
			stylesCopy = new HashSet<StyleInfo>( layer.getStyles() );
			defaultStyle = layer.getDefaultStyle();
		}
		if(!stylesCopy.contains(defaultStyle) ){
			stylesCopy.add(defaultStyle);
		}
		
		// searches the maximum and minimum denominator in the style's rules that are contained in the style set. 
		double minScaleDenominator =  Double.POSITIVE_INFINITY;
		double maxScaleDenominator = Double.NEGATIVE_INFINITY;

		for (StyleInfo styleInfo : stylesCopy) {

			Style style = styleInfo.getStyle();
		    for (FeatureTypeStyle fts : style.featureTypeStyles()) {
		    	
		        for ( Rule rule : fts.rules() ) {
		       
		            if ( rule.getMinScaleDenominator() < minScaleDenominator ) {
		            	minScaleDenominator = rule.getMinScaleDenominator();
		            }
		            if ( rule.getMaxScaleDenominator() > maxScaleDenominator ) {
		            	maxScaleDenominator = rule.getMaxScaleDenominator();
		            }
		        }
		    }
		}
		// If the initial values weren't changed by any rule in the previous step, 
		// then the default values, Min=0.0 and Max=infinity, are set. 
		if(minScaleDenominator ==  Double.POSITIVE_INFINITY) {
			minScaleDenominator = 0.0;
		}
		if( maxScaleDenominator == Double.NEGATIVE_INFINITY) {
			maxScaleDenominator = Double.POSITIVE_INFINITY;
		}
		assert minScaleDenominator <= maxScaleDenominator : "Min <= Max scale is expected";

	    return new NumberRange<Double>(Double.class, minScaleDenominator, maxScaleDenominator);
	}

	/**
	 * Computes the rendering scale taking into account the standard pixel size and the real world scale denominator.
	 * 
	 * @param scaleDenominator
	 * @return the rendering scale.
	 */
	public static Double computeScaleHint(final Double scaleDenominator) {
		
		// According to OGC SLD 1.0 specification: The "standardized rendering pixel size" is defined to be 0.28mm × 0.28mm (millimeters).
		final Double sizeStandardRenderPixel = 0.00028;//(meters) 
		
		Double scaleHint = Math.sqrt(Math.pow((scaleDenominator * sizeStandardRenderPixel), 2) * 2);
		
		return scaleHint;
	}
}
