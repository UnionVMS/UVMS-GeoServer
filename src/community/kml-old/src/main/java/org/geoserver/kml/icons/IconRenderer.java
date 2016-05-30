/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.icons;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Utility to render a point symbol as a stand alone icon.
 * 
 * @author David Winslow, OpenGeo
 * @author Kevin Smith, OpenGeo
 *
 */
public final class IconRenderer {
    private final static ReferencedEnvelope sampleArea = new ReferencedEnvelope(-1, 1, -1, 1, null);
    private final static SimpleFeatureCollection sampleData;
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.kml.icons");
    
    static {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("example");
        typeBuilder.setNamespaceURI("http://example.com/");
        typeBuilder.setSRS("EPSG:4326");
        typeBuilder.add("the_geom", Point.class);
        GeometryFactory geomFactory = new GeometryFactory();
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("the_geom", geomFactory.createPoint(new Coordinate(0, 0)));
        MemoryFeatureCollection temp = new MemoryFeatureCollection(featureType);
        temp.add(featureBuilder.buildFeature(null));
        sampleData = temp;
    }
    
    /**
     * Render a point icon for the given style. This operation will fail if any
     * style properties in the given style are dynamic. This method is intended
     * to work with styles that have been preprocessed by IconPropertyExtractor
     * and IconPropertyInjector.
     * 
     * @param style
     * @return
     */
    public static BufferedImage renderIcon(Style style) {
        int size = findIconSize(style)+1; // size is an int because icons are always square
        MapContent mapContent = new MapContent();
        mapContent.addLayer(new FeatureLayer(sampleData, style));
        BufferedImage image = new BufferedImage(size * Icons.RENDER_SCALE_FACTOR + 1, 
                size * Icons.RENDER_SCALE_FACTOR + 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.scale(Icons.RENDER_SCALE_FACTOR, Icons.RENDER_SCALE_FACTOR);
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        try {
            try {
                renderer.paint(graphics, new Rectangle(size, size), sampleArea);
            } finally {
                graphics.dispose();
            }
        } finally {
            mapContent.dispose();
        }
        return image;
    }

    private static int findIconSize(Style style) {
        int size = 0;
        if (style.featureTypeStyles().isEmpty()) throw new IllegalArgumentException("Feature type style list was empty");
        for (FeatureTypeStyle ftStyle : style.featureTypeStyles()) {
            if (ftStyle.rules().isEmpty()) throw new IllegalArgumentException("Rule list was empty");
            for (Rule rule : ftStyle.rules()) {
                if (rule.symbolizers().isEmpty()) throw new IllegalArgumentException("Symbolizer list was empty");
                for (Symbolizer symbolizer : rule.symbolizers()) {
                    if (symbolizer instanceof PointSymbolizer) {
                        Graphic g = ((PointSymbolizer) symbolizer).getGraphic();
                        if (g != null) {
                            Double rotation = g.getRotation() != null ? g.getRotation().evaluate(null, Double.class) : null;
                            size = Math.max(size, getGraphicSize(g, rotation));
                        }
                    } else {
                        throw new IllegalArgumentException("IconRenderer only supports PointSymbolizer");
                    }
                }
            }
        }
        return size;
    }
    
    private static int getGraphicSize(Graphic g, Double rotation) {
        Double result = Icons.graphicSize(g, rotation, null);
        if(result==null) {
            return (int) Icons.DEFAULT_SYMBOL_SIZE;
        } else {
            return (int) Math.ceil(result);
        }
    }
}
