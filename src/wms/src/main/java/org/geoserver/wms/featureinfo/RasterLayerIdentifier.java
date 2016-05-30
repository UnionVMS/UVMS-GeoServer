/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.TransformedDirectPosition;
import org.geotools.parameter.Parameter;
import org.geotools.resources.geometry.XRectangle2D;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.styling.Style;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.CannotEvaluateException;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;




/**
 * Layer identifier specifialized for raster layers
 * @author Andrea Aime - GeoSolutions
 *
 */
public class RasterLayerIdentifier implements LayerIdentifier {
    
    static final Logger LOGGER = Logging.getLogger(RasterLayerIdentifier.class);

    private WMS wms;
    
    public RasterLayerIdentifier(final WMS wms) {
        this.wms = wms;
    }
    
    public boolean canHandle(MapLayerInfo layer) {
        int type = layer.getType();
        return type == MapLayerInfo.TYPE_RASTER;
    }

    public List<FeatureCollection> identify(FeatureInfoRequestParameters params, int maxFeatures) throws Exception {
        final MapLayerInfo layer = params.getLayer();
        final Filter filter = params.getFilter(); 
        final CoverageInfo cinfo = layer.getCoverage();
        final GridCoverage2DReader reader = (GridCoverage2DReader) cinfo
                .getGridCoverageReader(new NullProgressListener(),
                        GeoTools.getDefaultHints());
        
        
        // set the requested position in model space for this request
        final Coordinate middle = WMS.pixelToWorld(params.getX(), params.getY(), params.getRequestedBounds(), params.getWidth(), params.getHeight());
        CoordinateReferenceSystem requestedCRS = params.getRequestedCRS();
        DirectPosition position = new DirectPosition2D(requestedCRS, middle.x, middle.y);

        // change from request crs to coverage crs in order to compute a minimal request
        // area,
        // TODO this code need to be made much more robust
        if (requestedCRS != null) {
            final CoordinateReferenceSystem targetCRS;
            if(cinfo.getProjectionPolicy() == ProjectionPolicy.NONE) {
                targetCRS = cinfo.getNativeCRS();
            } else {
                targetCRS = cinfo.getCRS();
            }
            final TransformedDirectPosition arbitraryToInternal = new TransformedDirectPosition(
                    requestedCRS, targetCRS, new Hints(Hints.LENIENT_DATUM_SHIFT,
                            Boolean.TRUE));
            try {
                arbitraryToInternal.transform(position);
            } catch (TransformException exception) {
                throw new CannotEvaluateException("Unable to answer the geatfeatureinfo",
                        exception);
            }
            position = arbitraryToInternal;
        }
        // check that the provided point is inside the bbox for this coverage
        if (!reader.getOriginalEnvelope().contains(position)) {
            return null;
        }

        // read from the request
        GetMapRequest getMap = params.getGetMapRequest();
        GeneralParameterValue[] parameters = wms.getWMSReadParameters(getMap, 
                layer, filter, params.getTimes(), params.getElevations(), reader, true);
        
        
        // now get the position in raster space using the world to grid related to
        // corner
        final MathTransform worldToGrid = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER)
                .inverse();
        final DirectPosition rasterMid = worldToGrid.transform(position, null);
        // create a 20X20 rectangle aruond the mid point and then intersect with the
        // original range
        final Rectangle2D.Double rasterArea = new Rectangle2D.Double();
        rasterArea.setFrameFromCenter(rasterMid.getOrdinate(0), rasterMid.getOrdinate(1),
                rasterMid.getOrdinate(0) + 10, rasterMid.getOrdinate(1) + 10);
        final Rectangle integerRasterArea = rasterArea.getBounds();
        final GridEnvelope gridEnvelope = reader.getOriginalGridRange();
        final Rectangle originalArea = (gridEnvelope instanceof GridEnvelope2D) ? (GridEnvelope2D) gridEnvelope
                : new Rectangle();
        XRectangle2D.intersect(integerRasterArea, originalArea, integerRasterArea);
        // paranoiac check, did we fall outside the coverage raster area? This should
        // never really happne if the request is well formed.
        if (integerRasterArea.isEmpty()) {
            return null;
        }
        // now set the grid geometry for this request
        for (int k = 0; k < parameters.length; k++) {
            if (!(parameters[k] instanceof Parameter<?>))
                continue;

            final Parameter<?> parameter = (Parameter<?>) parameters[k];
            if (parameter.getDescriptor().getName()
                    .equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName())) {
                //
                // create a suitable geometry for this request reusing the getmap (we
                // could probably optimize)
                //
                parameter.setValue(new GridGeometry2D(new GridEnvelope2D(integerRasterArea), reader
                        .getOriginalGridToWorld(PixelInCell.CELL_CENTER), reader.getCoordinateReferenceSystem()));
            }

        }

        final GridCoverage2D coverage = (GridCoverage2D) reader.read(parameters);
        if (coverage == null) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Unable to load raster data for this request.");
            return null;
        }

        FeatureCollection pixel = null;
        try {
            final double[] pixelValues = coverage.evaluate(position, (double[]) null);
            pixel = wrapPixelInFeatureCollection(coverage, pixelValues, cinfo.getQualifiedName());
        } catch (PointOutsideCoverageException e) {
            // it's fine, users might legitimately query point outside, we just don't
            // return anything
        } finally {
            RenderedImage ri = coverage.getRenderedImage();
            coverage.dispose(true);
            if(ri instanceof PlanarImage) {
                ImageUtilities.disposePlanarImageChain((PlanarImage) ri);
            }
        }
        return Collections.singletonList(pixel);
    }
    
    private SimpleFeatureCollection wrapPixelInFeatureCollection(GridCoverage2D coverage,
            double[] pixelValues, Name coverageName) throws SchemaException {

        GridSampleDimension[] sampleDimensions = coverage.getSampleDimensions();

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(coverageName);
        final Set<String> bandNames = new HashSet<String>();
        for (int i = 0; i < sampleDimensions.length; i++) {
            String name = sampleDimensions[i].getDescription().toString();
            // GEOS-2518
            if (bandNames.contains(name))
                // it might happen again that the name already exists but it pretty difficult I'd
                // say
                name = new StringBuilder(name).append("_Band").append(i).toString();
            bandNames.add(name);
            builder.add(name, Double.class);
        }
        SimpleFeatureType gridType = builder.buildFeatureType();

        Double[] values = new Double[pixelValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = new Double(pixelValues[i]);
        }
        return DataUtilities.collection(SimpleFeatureBuilder.build(gridType, values, ""));
    }

}
