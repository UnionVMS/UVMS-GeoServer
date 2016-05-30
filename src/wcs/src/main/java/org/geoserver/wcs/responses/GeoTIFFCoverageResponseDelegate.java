/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFLZWCompressor;

import java.awt.Dimension;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.OWS20Exception;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;

/**
 * Coverage writer for the geotiff format.
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GeoTIFFCoverageResponseDelegate extends BaseCoverageResponseDelegate implements CoverageResponseDelegate {

    private final static Logger LOGGER= Logging.getLogger(GeoTIFFCoverageResponseDelegate.class.toString());
    
    /** DEFAULT_JPEG_COMPRESSION_QUALITY */
    private static final float DEFAULT_JPEG_COMPRESSION_QUALITY = 0.75f;

	private static final GeoTiffFormat GEOTIF_FORMAT = new GeoTiffFormat();
	
        public static final String GEOTIFF_CONTENT_TYPE = "image/tiff";


    @SuppressWarnings("serial")
    public GeoTIFFCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("tif","tiff","geotiff","TIFF", "GEOTIFF", "GeoTIFF","image/geotiff"), //output formats
                new HashMap<String, String>(){ // file extensions
                    {
                        put("tiff", "tif");
                        put("tiff", "tif");
                        put("geotiff", "tif");
                        put("TIFF", "tif");
                        put("GEOTIFF", "tif");
                        put("GeoTIFF", "tif");
                        put("image/geotiff", "tif");    
                        put("image/tiff", "tif"); 
                    }
                },
                new HashMap<String, String>(){ //mime types
                    {
                        put("tiff", "image/tiff");
                        put("tif", "image/tiff");
                        put("geotiff", "image/tiff");
                        put("TIFF", "image/tiff");
                        put("GEOTIFF", "image/tiff");
                        put("GeoTIFF", "image/tiff");
                        put("image/geotiff", "image/tiff");                        
                    }
                });        
    }

    public void encode(GridCoverage2D sourceCoverage, String outputFormat, Map<String,String> econdingParameters, OutputStream output) throws IOException {
        Utilities.ensureNonNull("sourceCoverage", sourceCoverage);
        Utilities.ensureNonNull("econdingParameters", econdingParameters);

        
        // imposing encoding parameters
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        
        // compression
        handleCompression(econdingParameters, wp);
        
        // tiling
        handleTiling(econdingParameters, wp, sourceCoverage);
        
        // interleaving
        handleInterleaving(econdingParameters, wp, sourceCoverage);

        final ParameterValueGroup writerParams = GEOTIF_FORMAT.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        
        if(geoserver.getService(WCSInfo.class).isLatLon()){
            writerParams.parameter(GeoTiffFormat.RETAIN_AXES_ORDER.getName().toString()).setValue(true);
        }

        // write down
        GeoTiffWriter writer = (GeoTiffWriter) GEOTIF_FORMAT.getWriter(output);
        try {
            if (writer != null)
                writer.write(sourceCoverage, (GeneralParameterValue[]) writerParams.values()
                        .toArray(new GeneralParameterValue[1]));
        } finally {
            try {
                if (writer != null)
                    writer.dispose();
            } catch (Throwable e) {
                // eating exception
            }
            sourceCoverage.dispose(false);
        }
    }

    /**
     * Handle interleaving encoding parameters for WCS.
     * 
     * <p>
     * Notice that the Tiff ImageWriter supports only pixel interleaving.
     * 
     * @param econdingParameters a {@link Map} of {@link String} keys with {@link String} values to hold the encoding parameters.
     * @param wp an instance of {@link GeoTiffWriteParams} to be massaged as per the provided encoding parameters.
     * 
     * @throws WcsException in case there are invalid or unsupported options.
     */
    private void handleInterleaving(Map<String, String> encondingParameters, GeoTiffWriteParams wp, GridCoverage2D sourceCoverage) throws WcsException{

        // interleaving is optional
        if(encondingParameters.containsKey("interleave")){
            
            // ok, the interleaving has been specified, let's see what we got
            final String interleavingS= encondingParameters.get("interleave");
            if(interleavingS.equals("pixel")){
                // ok we want pixel interleaving, TIFF ImageWriter always writes
                // with pixel interleaving hence, we are good!
            } else if(interleavingS.equals("band")){
                // TODO implement this in TIFF Writer, as it is not supported right now
                throw new OWS20Exception("Banded Interleaving not supported", ows20Code(WcsExceptionCode.InterleavingNotSupported), interleavingS);
            } else {
                throw new OWS20Exception("Invalid Interleaving type provided", ows20Code(WcsExceptionCode.InterleavingInvalid), interleavingS);
            }
        }
        
    }

    /**
     * All OWS 2.0 exceptions for the geotiff extension come with a 404 error code
     * @param code
     * @return
     */
    private OWS20Exception.OWSExceptionCode ows20Code(WcsExceptionCode code) {
        return new OWS20Exception.OWSExceptionCode(code.toString(), 404);
    }

    /**
     * Handle tiling encoding parameters for WCS.
     * 
     * <p>
     * Notice that tile width and height must be positive and multiple of 16.
     * 
     * 
     * @param econdingParameters a {@link Map} of {@link String} keys with {@link String} values to hold the encoding parameters.
     * @param wp an instance of {@link GeoTiffWriteParams} to be massaged as per the provided encoding parameters.
     * @param sourceCoverage the source {@link GridCoverage2D} to encode.
     * 
     * @throws WcsException in case there are invalid or unsupported options.
     */
    private void handleTiling(Map<String, String> econdingParameters, final GeoTiffWriteParams wp, GridCoverage2D sourceCoverage)
            throws WcsException {

        // start with default dimension, since tileW and tileH are optional
        final RenderedImage sourceImage=sourceCoverage.getRenderedImage();
        final SampleModel sampleModel = sourceImage.getSampleModel();
        final int sourceTileW=sampleModel.getWidth();
        final int sourceTileH=sampleModel.getHeight();        
        final Dimension tileDimensions= new Dimension(sourceTileW,sourceTileH);
        LOGGER.fine("Source tiling:"+tileDimensions.width+"x"+tileDimensions.height);
        // if the tile size exceeds the image dimension, let's retile to save space on output image
        final GridEnvelope gr = sourceCoverage.getGridGeometry().getGridRange();
        if(gr.getSpan(0) < tileDimensions.width) {
            tileDimensions.width = gr.getSpan(0);
        }
        if(gr.getSpan(1) < tileDimensions.height) {
            tileDimensions.height = gr.getSpan(1);
        }
        LOGGER.fine("Source tiling reviewed to save space:"+tileDimensions.width+"x"+tileDimensions.height);        

        //
        // tiling
        //
        if(econdingParameters.containsKey("tiling")){
            
            final String tilingS= econdingParameters.get("tiling");
            if(tilingS!=null&&Boolean.valueOf(tilingS)){  
                
                
                // tileW
                if(econdingParameters.containsKey("tilewidth")){
                    final String tileW_= econdingParameters.get("tilewidth");
                    if(tileW_!=null){  
                        try{
                            final int tileW=Integer.valueOf(tileW_);
                            if(tileW>0&& (tileW%16==0)){
                                tileDimensions.width=tileW;
                            } else {
                                // tile width not supported
                                throw new OWS20Exception(
                                        "Provided tile width is invalid",
                                        ows20Code(WcsExceptionCode.TilingInvalid),
                                        Integer.toString(tileW));                            
                            } 
                        }catch (Exception e) {
                            // tile width not supported
                            throw new OWS20Exception(
                                    "Provided tile width is invalid",
                                    ows20Code(WcsExceptionCode.TilingInvalid),
                                    tileW_);    
                        }

                    }
                    
                }
                // tileH    
                if(econdingParameters.containsKey("tileheight")){
                    final String tileH_= econdingParameters.get("tileheight");
                    if(tileH_!=null){  
                        try{
                            final int tileH=Integer.valueOf(tileH_);
                            if(tileH>0&& (tileH%16==0)){
                                tileDimensions.height=tileH;
                            } else {
                                // tile height not supported
                                throw new OWS20Exception(
                                        "Provided tile height is invalid",
                                        ows20Code(WcsExceptionCode.TilingInvalid),
                                        Integer.toString(tileH));
                            }
                        }catch (Exception e) {
                            // tile height not supported
                            throw new OWS20Exception(
                                    "Provided tile height is invalid",
                                    ows20Code(WcsExceptionCode.TilingInvalid),
                                    tileH_);
                        }
                    }
                }
            }
        }

        // set tile dimensions
        if(tileDimensions.width!=sourceTileW||tileDimensions.height!=sourceTileH){
            LOGGER.fine("Final tiling:"+tileDimensions.width+"x"+tileDimensions.height);
            wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
            wp.setTiling(tileDimensions.width, tileDimensions.height);
        } else {
            LOGGER.fine("Mantaining original tiling");
        }
    }

    /**
     * Handle compression encoding parameters for WCS.
     * 
     * <p>
     * Notice that not all the encoding params are supported by the underlying Tiff ImageWriter
     * <ol>
     * <li>Floating Point predictor is not supported  for LZW</li>
     * <li>Huffman is supported only for 1 bit images</li>
     * </ol> 
     * 
     * @param econdingParameters a {@link Map} of {@link String} keys with {@link String} values to hold the encoding parameters.
     * @param wp an instance of {@link GeoTiffWriteParams} to be massaged as per the provided encoding parameters.
     * 
     * @throws WcsException in case there are invalid or unsupported options.
     */
    private void handleCompression(Map<String, String> econdingParameters,
            final GeoTiffWriteParams wp) throws WcsException {
        // compression
        if(econdingParameters.containsKey("compression")){ 
            String compressionS= econdingParameters.get("compression");
            if(compressionS!=null&&!compressionS.equalsIgnoreCase("none")){ 
                if(compressionS.equals("LZW")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("LZW"); 
                    
                    // look for a predictor
                    String predictorS= econdingParameters.get("predictor");
                    if(predictorS!=null){
                        if(predictorS.equals("None")){
                            
                        } else if(predictorS.equals("Horizontal")){
                            wp.setTIFFCompressor(new TIFFLZWCompressor(BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING));
                        } else if(predictorS.equals("Floatingpoint")){
                            // NOT SUPPORTED YET
                            throw new OWS20Exception(
                                    "Floating Point predictor is not supported",
                                    ows20Code(WcsExceptionCode.PredictorNotSupported),
                                    predictorS);
                        } else {
                            // invalid predictor
                            throw new OWS20Exception(
                                    "Invalid Predictor provided",
                                    ows20Code(WcsExceptionCode.PredictorInvalid),
                                    predictorS);
                        }
                    }
                } else if(compressionS.equals("JPEG")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("JPEG"); 
                    // start with the default one, visually lossless
                    wp.setCompressionQuality(DEFAULT_JPEG_COMPRESSION_QUALITY);
                    
                    // check quality 
                    if(econdingParameters.containsKey("jpeg_quality")){ 
                        final String quality_= econdingParameters.get("jpeg_quality");
                        if(quality_!=null){  
                            try{
                                final int quality=Integer.valueOf(quality_);
                                if(quality>0&&quality<=100){
                                    wp.setCompressionQuality(quality/100.f);
                                } else {
                                    // invalid quality
                                    throw new OWS20Exception(
                                            "Provided quality value for the jpeg compression in invalid",
                                            ows20Code(WcsExceptionCode.JpegQualityInvalid),
                                            quality_);
                                }  
                            } catch (Exception e) {
                                // invalid quality
                                throw new OWS20Exception(
                                        "Provided quality value for the jpeg compression in invalid",
                                        ows20Code(WcsExceptionCode.JpegQualityInvalid),
                                        quality_);
                            }
                        }  
                    }
                } else if(compressionS.equals("PackBits")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("PackBits");      
                } else if(compressionS.equals("DEFLATE")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("Deflate");      
                } else if(compressionS.equals("Huffman")){
                    wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
                    wp.setCompressionType("CCITT RLE");  
                } else {
                    // compression not supported
                    throw new OWS20Exception("Provided compression does not seem supported", ows20Code(WcsExceptionCode.CompressionInvalid), compressionS);
                }
            }
        }
    }

    
    @Override
    public String getConformanceClass(String format) {
        return "http://www.opengis.net/spec/GMLCOV_geotiff-coverages/1.0/conf/geotiff-coverage";
    }
}
