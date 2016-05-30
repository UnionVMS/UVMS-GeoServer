/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * ParameterFilter which allows the styles of the back end layer as legal values. Maintains a set 
 * of allowed layers which are intersected with those available on the layer. The default specified 
 * by the layer can be overridden and will be expended to its name rather than left null. 
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
@XStreamAlias("styleParameterFilter")
public class StyleParameterFilter extends ParameterFilter {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayerInfoImpl.class);

    private Set<String> allowedStyles;
    
    // The following two fields are omitted from REST
    private Set<String> availableStyles;
    private String defaultStyle;
    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Check that setLayer has been called
     */
    protected void checkInitialized(){
        checkState(availableStyles!=null, "Current styles of layer not available.");
    }
    
    public StyleParameterFilter(){
        super("STYLES");
    }
    
    @Override
    public String getDefaultValue() {
        checkInitialized();
        String name = super.getDefaultValue();
        if(name.isEmpty()) {
            // Default is not set so use the default from the layer
            if(defaultStyle==null) return "";
            return defaultStyle;
        } else {
            // Default is set so use it
            return name;
        }
    }
    
    @Override
    public boolean applies(String parameterValue) {
        checkInitialized();
        return parameterValue==null || getLegalValues().contains(parameterValue);
    }

    @Override
    public String apply(String str) throws ParameterException {
        checkInitialized();
        if(str == null || str.isEmpty()) {
            // Use the default
            return getDefaultValue();
        } else {
            for(String value: getLegalValues()){
                // Find a matching style
                if (value.equalsIgnoreCase(str)) {
                    return value;
                }
            }
            // no match so fail
            throw new ParameterException(str);
        }
    }
    
    @Override
    public void setKey(String key) {
        checkArgument(key.equalsIgnoreCase("STYLES"));
    }
    
    @Override
    public void setDefaultValue(String defaultValue) {
        if(defaultValue==null) defaultValue="";
        if(!defaultValue.isEmpty() && availableStyles!=null && !availableStyles.contains(defaultValue)) {
            LOGGER.log(Level.WARNING, "Selected default style "+defaultValue+" is not in the available styles "+availableStyles+".");
        }
        super.setDefaultValue(defaultValue);
    }
    
    /**
     * Returns the default style name, or an empty string if set to use the layer specified default
     * @return
     */
    public String getRealDefault() {
        // Bypass the special processing this class normally does on the default value
        return super.getDefaultValue();
    }
    
    /**
     * @see StyleParameterFilter#setDefaultValue()
     * @param s
     */
    public void setRealDefault(String s) {
        // Just use the regular set method
        setDefaultValue(s);
    }
    
    @Override
    public StyleParameterFilter clone() {
        StyleParameterFilter clone = new StyleParameterFilter();
        clone.setDefaultValue(super.getDefaultValue()); // Want to get the configured value so use super
        clone.setKey(getKey());
        clone.allowedStyles = getStyles();
        clone.availableStyles = availableStyles;
        clone.defaultStyle = defaultStyle;
        return clone;
    }
    
    /**
     * Get the names of all the styles supported by the layer
     * @return
     */
    public Set<String> getLayerStyles() {
        checkInitialized();
        return availableStyles;
    }
    
    @Override
    public List<String> getLegalValues() {
        checkInitialized();
        Set<String> layerStyles = getLayerStyles();
        if (allowedStyles==null) {
            // Values is null so allow any of the backing layer's styles
            return new ArrayList<String>(layerStyles);
        } else {
            // Values is set so only allow the intersection of the specified styles and those of the backing layer.
            return new ArrayList<String>(Sets.intersection(layerStyles, allowedStyles));
        }
    }
    
    /**
     * Set/update the availableStyles and defaultStyle based on the given GeoServer layer.
     * 
     * @param layer
     */
    public void setLayer(LayerInfo layer) {
        availableStyles = new TreeSet<String>();
        
        for(StyleInfo style: layer.getStyles()) {
            availableStyles.add(style.prefixedName());
        }
        if(layer.getDefaultStyle() !=null) {
            defaultStyle = layer.getDefaultStyle().prefixedName();
        } else {
            defaultStyle = null;
        }
    }
    
    /**
     * Get the styles.
     * @return The set of specified styles, or {@literal null} if all styles are allowed.
     */
    @Nullable public Set<String> getStyles() {
        if(allowedStyles==null) return null;
        return Collections.unmodifiableSet(allowedStyles);
    }
    
    
    /**
     * Set the allowed styles.  {@code null} to allow all styles available on the layer.
     * @param styles
     */
    public void setStyles(@Nullable Set<String> styles) {
        if(styles==null) {
            this.allowedStyles=null;
        } else {
            this.allowedStyles = new TreeSet<String>(styles);
        }
    }
    
    @Override
    protected ParameterFilter readResolve() {
        super.readResolve();
        Preconditions.checkState(this.getKey().equalsIgnoreCase("STYLES"), "StyleParameterFilter must have a key of \"STYLES\"");
        return this;
    }

}
