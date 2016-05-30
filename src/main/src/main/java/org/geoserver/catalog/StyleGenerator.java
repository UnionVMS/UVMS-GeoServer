/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.StyleGenerator.ColorRamp.Entry;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Generates pseudo random styles using a specified color ramp
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
public class StyleGenerator {

    private ColorRamp ramp;

    private Catalog catalog;

    /**
     * Workspace to create styles relative to
     */
    private WorkspaceInfo workspace;

    /**
     * Builds a style generator with the default color ramp
     * @param catalog
     */
    public StyleGenerator(Catalog catalog) {
        this.catalog = catalog;
        ramp = new ColorRamp();
        ramp.add("red", Color.decode("0xFF3300"));
        ramp.add("orange", Color.decode("0xFF6600"));
        ramp.add("dark orange", Color.decode("0xFF9900"));
        ramp.add("gold", Color.decode("0xFFCC00"));
        ramp.add("yellow", Color.decode("0xFFFF00"));
        ramp.add("dark yellow", Color.decode("0x99CC00"));
        ramp.add("teal", Color.decode("0x00CC33"));
        ramp.add("cyan", Color.decode("0x0099CC"));
        ramp.add("azure", Color.decode("0x0033CC"));
        ramp.add("violet", Color.decode("0x3300FF"));
        randomizeRamp();
    }

    protected void randomizeRamp() {
        ramp.initRandom();
    }

    public StyleGenerator(Catalog catalog, ColorRamp ramp) {
        if (ramp == null)
            throw new NullPointerException("The color ramp cannot be null");

        this.ramp = ramp;
        this.catalog = catalog;
    }

    /**
     * Set the workspace to generate styles in.
     * @param workspace
     */
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    /**
     * Generate a style for a resource in the catalog, and add the created style to the catalog.
     * 
     * @param handler: The StyleHandler used to generate the style. Determines the style format.
     * @param featureType: The FeatureType to generate the style for. Determines the style type and 
     * style name
     * @return The StyleInfo referencing the generated style
     * @throws IOException
     */
    public StyleInfo createStyle(StyleHandler handler, FeatureTypeInfo featureType) throws IOException {
        return createStyle(handler, featureType, featureType.getFeatureType());
    }
    
    /**
     * Generate a style for a resource in the catalog, and add the created style to the catalog.
     * 
     * @param handler: The StyleHandler used to generate the style. Determines the style format.
     * @param featureType: The FeatureType to generate the style for. Determines the style type and 
     * style name
     * @param nativeFeatureType: The geotools feature type, required in cases where featureType is 
     * missing content
     * @return The StyleInfo referencing the generated style
     * @throws IOException
     */
    public StyleInfo createStyle(StyleHandler handler, FeatureTypeInfo featureType, FeatureType nativeFeatureType) 
        throws IOException {

        // geometryless, style it randomly
        GeometryDescriptor gd = nativeFeatureType.getGeometryDescriptor();
        if (gd == null)
            return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);

        Class gtype = gd.getType().getBinding();
        StyleType st;
        if (LineString.class.isAssignableFrom(gtype)
                || MultiLineString.class.isAssignableFrom(gtype)) {
            st = StyleType.LINE;
        } else if (Polygon.class.isAssignableFrom(gtype)
                || MultiPolygon.class.isAssignableFrom(gtype)) {
            st = StyleType.POLYGON;
        } else if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
            st = StyleType.POINT;
        } else {
            st = StyleType.GENERIC;
        }

        return doCreateStyle(handler, st, featureType);
    }

    /**
     * Generate a style for a resource in the catalog, and add the created style to the catalog.
     * 
     * @param handler: The StyleHandler used to generate the style. Determines the style format.
     * @param coverage: The CoverageInfo to generate the style for. Determines the style type and 
     * style name
     * @return The StyleInfo referencing the generated style
     * @throws IOException
     */
    public StyleInfo createStyle(StyleHandler handler, CoverageInfo coverage) throws IOException {
        return doCreateStyle(handler, StyleType.RASTER, coverage);
    }
    
    /**
     * Generate a style of the give style type. 
     * 
     * @param handler: The StyleHandler used to generate the style. Determines the style format.
     * @param styleType: The type of template, see {@link org.geoserver.catalog.StyleType}.
     * @param layerName: The name of the style/layer; used in comments.
     * @return The generated style, as a String.
     * @throws IOException
     */
    public String generateStyle(StyleHandler handler, StyleType styleType, String layerName) throws IOException {
        Entry color = ramp.next();
        try {
            return handler.getStyle(styleType, color.color, color.name, layerName);
        } catch (UnsupportedOperationException e) {
            //Handler does not support loading from template; load SLD template and convert
            SLDHandler sldHandler = new SLDHandler();
            String sldTemplate =  sldHandler.getStyle(styleType, color.color, color.name, layerName);
            
            StyledLayerDescriptor sld = sldHandler.parse(sldTemplate, null, null, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            handler.encode(sld, null, true, out);
            return out.toString();
        }
    }

    StyleInfo doCreateStyle(StyleHandler handler, StyleType styleType, ResourceInfo resource) throws IOException {
        // find a new style name
        String styleName = workspace != null ?
            findUniqueStyleName(resource, workspace) : findUniqueStyleName(resource);

        // variable replacement
        String styleData = generateStyle(handler, styleType, styleName);

        // let's store it
        StyleInfo style = catalog.getFactory().createStyle();
        style.setName(styleName);
        style.setFilename(styleName + "." + handler.getFileExtension());
        style.setFormat(handler.getFormat());
        if (workspace != null) {
            style.setWorkspace(workspace);
        }

        catalog.getResourcePool().writeStyle(style, new ByteArrayInputStream(styleData.getBytes()));
        
        return style;
    }

    String findUniqueStyleName(ResourceInfo resource) {
        String styleName = resource.getStore().getWorkspace().getName() + "_" + resource.getName();
        StyleInfo style = catalog.getStyleByName(styleName);
        int i = 1;
        while(style != null) {
            styleName = resource.getStore().getWorkspace().getName() + "_" + resource.getName() + i;
            style = catalog.getStyleByName(styleName);
            i++;
        }
        return styleName;
    }

    String findUniqueStyleName(ResourceInfo resource, WorkspaceInfo workspace) {
        String styleName = resource.getName();
        StyleInfo style = catalog.getStyleByName(workspace, styleName);
        int i = 1;
        while(style != null) {
            styleName = resource.getName() + i;
            style = catalog.getStyleByName(workspace, styleName);
            i++;
        }
        return styleName;
    }

    /**
     * A rolling color ramp with color names
     */
    public static class ColorRamp {
        static class Entry {
            String name;
            Color color;

            Entry(String name, Color color) {
                this.name = name;
                this.color = color;
            }
        }

        List<Entry> entries = new ArrayList<Entry>();
        int position;
        
        /**
         * Builds an empty ramp. Mind, you need to call {@link #add(String, Color)} at least
         * once to make the ramp usable.
         */
        public ColorRamp() {
            
        }
        
        /**
         * Adds a name/color combination
         * @param name
         * @param color
         */
        public void add(String name, Color color) {
            entries.add(new Entry(name, color));
        }
        
        /**
         * Moves to the next color in the ramp
         */
        public Entry next() {
            position = (position+1) % entries.size();
            return entries.get(position);
        }

        /**
         * Sets the current ramp position to a random index
         */
        public void initRandom() {
            position = (int) (entries.size() * Math.random());
        }

    }
}
