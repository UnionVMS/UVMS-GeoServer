/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;
import javax.media.jai.JAI;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.ppio.ZipArchivePPIO;
import org.geoserver.wps.resource.WPSFileResource;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The main DownloadProcess class.
 * 
 * This class is simply responsible for deciding who is going to take care of the request and then for putting together the final result as a zip file
 * adding the needed styles.
 * 
 * 
 * @author "Alessio Fabiani - alessio.fabiani@geo-solutions.it"
 * @author Simone Giannecchini, GeoSolutions SAS
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Enterprise Download Process", description = "Downloads Layer Stream and provides a ZIP.")
public class DownloadProcess implements GSProcess, ApplicationContextAware {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(DownloadProcess.class);

    /** The estimator. */
    private final DownloadEstimatorProcess estimator;

    /** The catalog. */
    private final Catalog catalog;

    private WPSResourceManager resourceManager;

    private ApplicationContext context;

    /**
     * Instantiates a new download process.
     * 
     * @param geoServer the geo server
     * @param sendMail the send mail
     * @param estimator the estimator
     * @param resourceManager the resourceManager to track resources to be cleaned up
     */
    public DownloadProcess(GeoServer geoServer, DownloadEstimatorProcess estimator,
            WPSResourceManager resourceManager) {
        Utilities.ensureNonNull("geoServer", geoServer);
        this.catalog = geoServer.getCatalog();
        this.estimator = estimator;
        this.resourceManager = resourceManager;
    }

    /**
     * This process returns a zipped file containing the selected layer, cropped if needed.
     * 
     * @param layerName the layer name
     * @param filter the filter
     * @param email the email
     * @param mimeType the output format
     * @param targetCRS the target crs
     * @param roiCRS the roi crs
     * @param roi the roi
     * @param clip the crop to geometry
     * @param interpolation interpolation method to use when reprojecting / scaling
     * @param targetSizeX the size of the target image along the X axis
     * @param targetSizeY the size of the target image along the Y axis
     * @param progressListener the progress listener
     * @return the file
     * @throws ProcessException the process exception
     */
    @DescribeResult(name = "result", description = "Zipped output files to download")
    public File execute(
            @DescribeParameter(name = "layerName", min = 1, description = "Original layer to download") String layerName,
            @DescribeParameter(name = "filter", min = 0, description = "Optional Vector Filter") Filter filter,
            @DescribeParameter(name = "outputFormat", min = 1, description = "Output Format Mime-Type") String mimeType,
            @DescribeParameter(name = "targetCRS", min = 0, description = "Optional Target CRS") CoordinateReferenceSystem targetCRS,
            @DescribeParameter(name = "RoiCRS", min = 0, description = "Optional Region Of Interest CRS") CoordinateReferenceSystem roiCRS,
            @DescribeParameter(name = "ROI", min = 0, description = "Optional Region Of Interest (Polygon)") Geometry roi,
            @DescribeParameter(name = "cropToROI", min = 0, description = "Crop to ROI") Boolean clip,
            @DescribeParameter(name = "interpolation", description = "Interpolation function to use when reprojecting / scaling raster data.  Values are NEAREST (default), BILINEAR, BICUBIC2, BICUBIC", min = 0) Interpolation interpolation,
            @DescribeParameter(name = "targetSizeX", min = 0, minValue = 1, description = "X Size of the Target Image (applies to raster data only)") Integer targetSizeX,
            @DescribeParameter(name = "targetSizeY", min = 0, minValue = 1, description = "Y Size of the Target Image (applies to raster data only)") Integer targetSizeY,
            final ProgressListener progressListener) throws ProcessException {

        try {

            //
            // initial checks on mandatory params
            //
            // layer name
            if (layerName == null || layerName.length() <= 0) {
                throw new IllegalArgumentException("Empty or null layerName provided!");
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Download process called on resource: " + layerName);
            }
            // Default behavior is intersection
            if (clip == null) {
                clip = false;
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Clipping disabled");
                }
            }
            // Checking the validity of the input ROI
            if (roi != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "ROI check");
                }
                DownloadUtilities.checkPolygonROI(roi);
                if (roiCRS == null) {
                    throw new IllegalArgumentException("ROI without a CRS is not usable!");
                }
                roi.setUserData(roiCRS);
            }

            // set default interpolation value
            if (interpolation == null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                            "Interpolation parameter not specified, using default (Nearest Neighbor)");
                }
                interpolation = (Interpolation) ImageUtilities.NN_INTERPOLATION_HINT
                        .get(JAI.KEY_INTERPOLATION);
            }

            //
            // do we respect limits?
            //
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Running the estimator");
            }
            if (!estimator.execute(layerName, filter, targetCRS, roiCRS, roi, clip, targetSizeX,
                    targetSizeY, progressListener)) {
                throw new IllegalArgumentException("Download Limits Exceeded. Unable to proceed!");
            }

            //
            // Move on with the real code
            //
            // checking for the resources on the GeoServer catalog
            LayerInfo layerInfo = catalog.getLayerByName(layerName);
            if (layerInfo == null) {
                // could not find any layer ... abruptly interrupt the process
                throw new IllegalArgumentException("Unable to locate layer: " + layerName);

            }
            ResourceInfo resourceInfo = layerInfo.getResource();
            if (resourceInfo == null) {
                // could not find any data store associated to the specified layer ... abruptly interrupt the process
                throw new IllegalArgumentException("Unable to locate ResourceInfo for layer:"
                        + layerName);

            }

            //
            // Limits
            //
            DownloadServiceConfiguration limits = estimator.getDownloadServiceConfiguration();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Employing limits " + limits);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "The resource to work on is " + resourceInfo.getName());
            }

            // CORE CODE
            File internalOutput = null;
            if (resourceInfo instanceof FeatureTypeInfo) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "The resource to work on is a vector layer");
                }
                //
                // VECTOR
                //
                // perform the actual download of vectorial data accordingly to the request inputs
                internalOutput = new VectorDownload(limits, resourceManager, context).execute(
                        (FeatureTypeInfo) resourceInfo, mimeType, roi, clip, filter, targetCRS,
                        progressListener);

            } else if (resourceInfo instanceof CoverageInfo) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "The resource to work on is a raster layer");
                }
                //
                // RASTER
                //
                CoverageInfo cInfo = (CoverageInfo) resourceInfo;
                // convert/reproject/crop if needed the coverage
                internalOutput = new RasterDownload(limits, resourceManager, context).execute(
                        mimeType, progressListener, cInfo, roi, targetCRS, clip, filter,
                        interpolation, targetSizeX, targetSizeY);
            } else {

                // wrong type
                throw new IllegalArgumentException(
                        "Could not complete the Download Process, requested layer was of wrong type-->"
                                + resourceInfo.getClass());

            }

            //
            // Work on result
            //
            // checks
            if (internalOutput == null) {
                // wrong type
                throw new IllegalStateException(
                        "Could not complete the Download Process, output file is null");
            }
            if (!internalOutput.exists() || !internalOutput.canRead()) {
                // wrong type
                throw new IllegalStateException(
                        "Could not complete the Download Process, output file invalid! --> "
                                + internalOutput.getAbsolutePath());

            }

            // adding the style and zipping
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Preparing the result");
            }
            // build output zip
            final File result = resourceManager.getOutputResource(
                    resourceManager.getExecutionId(true), resourceInfo.getName() + ".zip").file();

            FileOutputStream os1 = null;
            try {
                os1 = new FileOutputStream(result);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Listing files");
                }
                // output
                List<File> filesToDownload = new ArrayList<File>();
                filesToDownload.add(internalOutput);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Collecting styles");
                }
                // add all SLD to zip
                List<File> styles = DownloadUtilities.collectStyles(layerInfo);
                filesToDownload.addAll(styles);

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Zipping files");
                }
                // zip them all
                new ZipArchivePPIO(estimator.getDownloadServiceConfiguration()
                        .getCompressionLevel()).encode(filesToDownload, os1);

            } finally {
                if (os1 != null) {
                    IOUtils.closeQuietly(os1);
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Prepare the result for deletion");
                }
                // mark original output for deletion
                resourceManager.addResource(new WPSFileResource(internalOutput));
            }

            //
            // finishing
            //
            // Completed!
            if (progressListener != null) {
                progressListener.complete();
            }

            // return
            return result;
        } catch (Throwable e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Download failed");
            }
            // FAILED

            // catch and rethrow but warn the listener
            final ProcessException processException = new ProcessException(e);
            if (progressListener != null) {
                progressListener.exceptionOccurred(processException);
            }
            throw processException;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;

    }
}