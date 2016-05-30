/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.sequence;

import java.util.NoSuchElementException;

import org.geoserver.kml.utils.KmlCentroidBuilder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Wraps a generic feature collection and returns a collection whose feature geometries
 * are the centroids of the original features  
 * 
 * @author Andrea Aime - GeoSolutions
 */
class KMLCentroidFeatureCollection extends DecoratingSimpleFeatureCollection {

    

    private SimpleFeatureType schema;

    protected KMLCentroidFeatureCollection(
            FeatureCollection<SimpleFeatureType, SimpleFeature> delegate) {
        super(delegate);

        // build the centroid collection schema
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor ad : delegate.getSchema().getAttributeDescriptors()) {
            if (ad instanceof GeometryDescriptor) {
                GeometryDescriptor gd = (GeometryDescriptor) ad;
                Class<?> binding = ad.getType().getBinding();
                if (Point.class.isAssignableFrom(binding)) {
                    tb.add(ad);
                } else {
                    tb.minOccurs(ad.getMinOccurs());
                    tb.maxOccurs(ad.getMaxOccurs());
                    tb.nillable(ad.isNillable());
                    tb.add(ad.getLocalName(), Point.class, gd.getCoordinateReferenceSystem());
                }
            } else {
                tb.add(ad);
            }
        }
        tb.setName(delegate.getSchema().getName());
        this.schema = tb.buildFeatureType();
    }
    
    @Override
    public SimpleFeatureIterator features() {
        return new KMLCentroidFeatureIterator(delegate.features(), this.schema);
    }
    
    static class KMLCentroidFeatureIterator implements SimpleFeatureIterator {

        private SimpleFeatureIterator delegate;
        private SimpleFeatureBuilder builder;
        private KmlCentroidBuilder centroids;

        public KMLCentroidFeatureIterator(SimpleFeatureIterator features, SimpleFeatureType schema) {
            this.delegate = features;
            this.builder = new SimpleFeatureBuilder(schema);
            this.centroids = new KmlCentroidBuilder();
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature f = delegate.next();
            for (Object attribute : f.getAttributes()) {
                if ((attribute instanceof Geometry) &&
                    !(attribute instanceof Point)) {
                    Geometry geom = (Geometry) attribute;
                    Coordinate point = centroids.geometryCentroid(geom);
                    attribute = geom.getFactory().createPoint(point);
                }
                builder.add(attribute);
            }
            return builder.buildFeature(f.getID());
        }

        @Override
        public void close() {
            delegate.close();
        }

    }

}
