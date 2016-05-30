/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import it.geosolutions.imageio.maskband.DatasetLayout;
import it.geosolutions.jaiext.range.NoDataContainer;

import java.awt.image.ColorModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.unit.Unit;
import javax.media.jai.ImageLayout;
import javax.media.jai.PropertySource;
import javax.media.jai.PropertySourceImpl;

import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.HarvestedSource;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.resources.Classes;
import org.geotools.resources.coverage.CoverageUtilities;
import org.geotools.util.NumberRange;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.ColorInterpretation;
import org.opengis.coverage.PaletteInterpretation;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.InternationalString;

/**
 * A {@link GridCoverage2DReader} wrapper to customize the {@link CoverageDimensionInfo} associated
 * with a coverage by exposing configured values such as null values, band name, and 
 * data ranges instead of the ones associated with the underlying coverage. 
 *  
 * @author Daniele Romagnoli - GeoSolutions SAS
 */
public class CoverageDimensionCustomizerReader implements GridCoverage2DReader {

    private static Logger LOGGER = Logging.getLogger(CoverageDimensionCustomizerReader.class);

    final static String LINE_SEPARATOR = System.getProperty("line.separator", "\n");

    private CoverageInfo info;

    private GridCoverage2DReader delegate;

    private String coverageName;

    static class CoverageDimensionCustomizerStructuredReader extends CoverageDimensionCustomizerReader implements StructuredGridCoverage2DReader{

        private StructuredGridCoverage2DReader structuredDelegate;

        public CoverageDimensionCustomizerStructuredReader(StructuredGridCoverage2DReader delegate,
                String coverageName, CoverageInfo info) {
            super(delegate, coverageName, info);
            this.structuredDelegate = delegate;
        }

        public CoverageDimensionCustomizerStructuredReader(StructuredGridCoverage2DReader delegate,
                String coverageName, CoverageStoreInfo storeInfo) {
            super(delegate, coverageName, storeInfo);
            this.structuredDelegate = delegate;
        }

        @Override
        public GranuleSource getGranules(String coverageName, boolean readOnly) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.getGranules(coverageName, readOnly);
        }

        @Override
        public boolean isReadOnly() {
            return structuredDelegate.isReadOnly();
        }

        @Override
        public void createCoverage(String coverageName, SimpleFeatureType schema)
                throws IOException, UnsupportedOperationException {
            structuredDelegate.createCoverage(coverageName, schema);
        }

        @Override
        public boolean removeCoverage(String coverageName) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.removeCoverage(coverageName);
        }

        @Override
        public boolean removeCoverage(String coverageName, boolean delete) throws IOException,
                UnsupportedOperationException {
            return structuredDelegate.removeCoverage(coverageName, delete);
        }

        @Override
        public void delete(boolean deleteData) throws IOException {
            structuredDelegate.delete(deleteData);
        }

        @Override
        public List<HarvestedSource> harvest(String defaultTargetCoverage, Object source,
                Hints hints) throws IOException, UnsupportedOperationException {
            return structuredDelegate.harvest(defaultTargetCoverage, source, hints);
        }

        @Override
        public List<DimensionDescriptor> getDimensionDescriptors(String coverageName)
                throws IOException {
            return structuredDelegate.getDimensionDescriptors(coverageName);
        }
    }

    /**
     * Wrap a {@link GridCoverage2DReader} into a {@link CoverageDimensionCustomizerReader}.
     * @param delegate the reader to be wrapped.
     * @param coverageName the specified coverageName. It may be null in case of {@link GridCoverage2DReader}s 
     * with a single coverage, coming from an old catalog where no coverageName has been stored. 
     * @param info the {@link CoverageStoreInfo} instance used to look for {@link CoverageInfo} instances.
     * @return 
     */
    public static GridCoverageReader wrap(GridCoverage2DReader delegate, String coverageName,
            CoverageStoreInfo info) {
        GridCoverage2DReader reader = delegate;
        if (coverageName != null) {
            reader = SingleGridCoverage2DReader.wrap(delegate, coverageName);
        }
        if (reader instanceof StructuredGridCoverage2DReader) {
            return new CoverageDimensionCustomizerStructuredReader((StructuredGridCoverage2DReader) reader, coverageName, info);
        } else {
            return new CoverageDimensionCustomizerReader(reader, coverageName, info);
        }
    }

    public CoverageDimensionCustomizerReader(GridCoverage2DReader delegate,
            String coverageName, CoverageStoreInfo storeInfo) {
        this.delegate = delegate; 
        this.coverageName = coverageName;
        this.info = getCoverageInfo(storeInfo);
    }

    public CoverageDimensionCustomizerReader(GridCoverage2DReader delegate, String coverageName, CoverageInfo info) {
        this.delegate = delegate; 
        this.coverageName = coverageName;
        this.info = info;
    }

    /**
     * Retrieve the proper {@link CoverageInfo} object from the specified {@link CoverageStoreInfo} 
     * using the specified coverageName (which may be the native one in some cases).
     * In case of null coverageName being specified, we assume we are dealing with a 
     * single coverageStore <-> single coverage relation so we will take the first coverage available
     * on that store.
     * 
     * @param storeInfo the storeInfo to be used to access the catalog
     * @return
     */
    private CoverageInfo getCoverageInfo(CoverageStoreInfo storeInfo) {
        Utilities.ensureNonNull("storeInfo", storeInfo);
        final Catalog catalog = storeInfo.getCatalog();
        if (coverageName != null) {
            info = catalog.getCoverageByName(coverageName);
        }
        if (info == null) {
            final List<CoverageInfo> coverages = catalog.getCoveragesByStore(storeInfo);
            if (coverageName != null) {
                for (CoverageInfo coverage: coverages) {
                    if (coverage.getNativeName().equalsIgnoreCase(coverageName)) {
                        info = coverage;
                        break;
                    }
                }
            }
            if (info == null && coverages != null && coverages.size() == 1) {
                // Last resort
                info = coverages.get(0);
            }
        }
        return info;
    }

    public String getCoverageName() {
        return coverageName;
    }

    public CoverageInfo getInfo() {
        return info;
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IllegalArgumentException,
            IOException {
        return read(this.coverageName, parameters);
    }

    @Override
    /**
     * This specific read operation is a customized one since we need to wrap the coverage properties 
     * (null values, ranges, sampleDimensions...)
     */
    public GridCoverage2D read(String coverageName, GeneralParameterValue[] parameters)
            throws IllegalArgumentException, IOException {
        final GridCoverage2D coverage = coverageName != null ? delegate.read(coverageName, parameters) : delegate.read(parameters);
        if (coverage == null) {
            return coverage;
        }
        final Map<String, Object> properties = coverage.getProperties();
        final SampleDimension[] dims = coverage.getSampleDimensions();
        GridSampleDimension[] wrappedDims = wrapDimensions(dims);
        // Wrapping sample dimensions
        NoDataContainer noDataProperty = CoverageUtilities.getNoDataProperty(coverage);
        if (wrappedDims == null) {
            wrappedDims = (GridSampleDimension[]) dims;
        } else if (properties != null && noDataProperty != null) {
            // update the GC_NODATA property (if any) with the latest value, if we have any
            double[] wrappedNoDataValues = wrappedDims[0].getNoDataValues();
            if (wrappedNoDataValues != null && wrappedNoDataValues.length > 0) {
                CoverageUtilities.setNoDataProperty(properties, wrappedNoDataValues[0]);
            }
        }

        // Wrap the coverage into a coverageWrapper to change its name and sampleDimensions
        return new GridCoverageWrapper(coverageName, coverage, wrappedDims, properties);
    }

    protected GridSampleDimension[] wrapDimensions(SampleDimension[] dims) {
        GridSampleDimension[] wrappedDims = null;
        if (info != null) {
            List<CoverageDimensionInfo> storedDimensions = info.getDimensions();
            MetadataMap map = info.getMetadata();
            if (storedDimensions != null && storedDimensions.size() > 0) {
                    int i = 0;
                    final int inputDims = storedDimensions.size();
                    final int outputDims = dims.length;
                    wrappedDims = new GridSampleDimension[outputDims];
                    for (SampleDimension dim: dims) {
                        wrappedDims[i] = new WrappedSampleDimension((GridSampleDimension) dim, 
                                storedDimensions.get(outputDims != inputDims ? (i > (inputDims - 1 ) ? inputDims - 1 : i) : i));
                        i++;
                    }
                }
        }
        return wrappedDims;
    }

    public Format getFormat() {
        return delegate.getFormat();
    }

    public Object getSource() {
        return delegate.getSource();
    }

    public String[] getMetadataNames() throws IOException {
        return delegate.getMetadataNames();
    }

    public String[] getMetadataNames(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataNames(coverageName);
    }

    public String getMetadataValue(String name) throws IOException {
        return delegate.getMetadataValue(name);
    }

    public String getMetadataValue(String coverageName, String name) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getMetadataValue(coverageName, name);
    }

    public String[] listSubNames() throws IOException {
        return delegate.listSubNames();
    }

    public String getCurrentSubname() throws IOException {
        return delegate.getCurrentSubname();
    }

    public boolean hasMoreGridCoverages() throws IOException {
        return delegate.hasMoreGridCoverages();
    }

    public void skip() throws IOException {
        delegate.skip();
    }

    public void dispose() throws IOException {
        delegate.dispose();
    }

    public GeneralEnvelope getOriginalEnvelope() {
        return delegate.getOriginalEnvelope();
    }

    public GeneralEnvelope getOriginalEnvelope(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalEnvelope(coverageName);
    }

    public GridEnvelope getOriginalGridRange() {
        return delegate.getOriginalGridRange();
    }

    public GridEnvelope getOriginalGridRange(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridRange(coverageName);
    }

    public MathTransform getOriginalGridToWorld(PixelInCell pixInCell) {
        return delegate.getOriginalGridToWorld(pixInCell);
    }

    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        checkCoverageName(coverageName);
        return delegate.getOriginalGridToWorld(coverageName, pixInCell);
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return delegate.getCoordinateReferenceSystem();
    }

    public CoordinateReferenceSystem getCoordinateReferenceSystem(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getCoordinateReferenceSystem(coverageName);
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters() throws IOException {
        return delegate.getDynamicParameters();
    }

    public Set<ParameterDescriptor<List>> getDynamicParameters(String coverageName)
            throws IOException {
        checkCoverageName(coverageName);
        return delegate.getDynamicParameters(coverageName);
    }

    public double[] getReadingResolutions(OverviewPolicy policy, double[] requestedResolution)
            throws IOException {
        return delegate.getReadingResolutions(policy, requestedResolution);
    }

    public double[] getReadingResolutions(String coverageName, OverviewPolicy policy,
            double[] requestedResolution) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getReadingResolutions(coverageName, policy, requestedResolution);
    }

    public String[] getGridCoverageNames() throws IOException {
        return delegate.getGridCoverageNames();
    }

    public int getGridCoverageCount() throws IOException {
        return delegate.getGridCoverageCount();
    }

    public int getNumOverviews() {
        return delegate.getNumOverviews();
    }

    public int getNumOverviews(String coverageName) {
        checkCoverageName(coverageName);
        return delegate.getNumOverviews(coverageName);
    }

    public ImageLayout getImageLayout() throws IOException {
        return delegate.getImageLayout();
    }

    public ImageLayout getImageLayout(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getImageLayout(coverageName);
    }

    public double[][] getResolutionLevels() throws IOException {
        return delegate.getResolutionLevels();
    }

    public double[][] getResolutionLevels(String coverageName) throws IOException {
        checkCoverageName(coverageName);
        return delegate.getResolutionLevels(coverageName);
    }

    /**
     * Checks the specified name is the one we are expecting
     * @param coverageName
     */
    protected void checkCoverageName(String coverageName) {
        if (this.coverageName != null && !this.coverageName.equals(coverageName)) {
            throw new IllegalArgumentException("Unkonwn coverage named " + coverageName
                    + ", the only valid value is: " + this.coverageName);
        }
    }

    /**
     * Utility class to wrap a GridCoverage by overriding its sampleDimensions and properties 
     */
    public static class GridCoverageWrapper extends GridCoverage2D {

        /** A custom propertySource allowing to redefine properties (since getProperties return a clone) */
        private PropertySourceImpl wrappedPropertySource;

        /** Configured sampleDimensions */
        private GridSampleDimension[] wrappedSampleDimensions;

        public GridCoverageWrapper(String name, GridCoverage2D coverage, GridSampleDimension[] sampleDimensions, Map properties) {
            super(name, coverage);
            this.wrappedSampleDimensions = sampleDimensions;
            wrappedPropertySource = new PropertySourceImpl(properties, 
                    coverage instanceof PropertySource ? (PropertySource) coverage : null);
        }

        @Override
        public GridSampleDimension getSampleDimension(int index) {
            return wrappedSampleDimensions[index];
        }

        @Override
        public GridSampleDimension[] getSampleDimensions() {
            return wrappedSampleDimensions.clone();
        }

        @Override
        public Map getProperties() {
            return wrappedPropertySource.getProperties();
        }

        @Override
        public Object getProperty(String arg0) {
            return wrappedPropertySource.getProperty(arg0);
        }

        public static GridCoverage2D wrapCoverage(GridCoverage2D coverage, GridCoverage2D sourceCoverage, GridSampleDimension[] wrappedDimensions, Map properties, boolean forceWrapping) {
            if (coverage instanceof GridCoverageWrapper || forceWrapping) {
                return new GridCoverageWrapper(coverage.getName().toString(), coverage, 
                        wrappedDimensions == null ? coverage.getSampleDimensions() : wrappedDimensions, 
                        properties == null ? sourceCoverage.getProperties() : properties);
            }
            return coverage;
        }
    }

    /** 
     * Wrap a GridSampleDimension by overriding categories, ranges, null values and name.
     */
    static class WrappedSampleDimension extends GridSampleDimension implements SampleDimension {

        @Override
        public SampleDimensionType getSampleDimensionType() {
            return sampleDim.getSampleDimensionType();
        }
        @Override
        public InternationalString getDescription() {
            return configuredDescription;
        }
        @Override
        public InternationalString[] getCategoryNames() throws IllegalStateException {
            return sampleDim.getCategoryNames();
        }
        @Override
        public List<Category> getCategories() {
            return customCategories;
        }
        @Override
        public Category getCategory(double sample) {
            return sampleDim.getCategory(sample);
        }
        @Override
        public double[] getNoDataValues() throws IllegalStateException {
            return configuredNoDataValues;
        }
        @Override
        public double getMinimumValue() {
            NumberRange<? extends Number> range = getRange();
            // Check if the range exists, otherwise use the sample dimension values
            if (range != null){
                return range.getMinimum();
            } else {
                return sampleDim.getMinimumValue();
            }
        }
        @Override
        public double getMaximumValue() {
            NumberRange<? extends Number> range = getRange();
            // Check if the range exists, otherwise use the sample dimension values
            if (range != null) {
                return range.getMaximum();
            } else {
                return sampleDim.getMaximumValue();
            }
        }
        @Override
        public NumberRange<? extends Number> getRange() {
            return configuredRange;
        }
        @Override
        public String getLabel(double value, Locale locale) {
            return sampleDim.getLabel(value, locale);
        }
        @Override
        public Unit<?> getUnits() {
            return configuredUnit;
        }
        @Override
        public double getOffset() throws IllegalStateException {
            return sampleDim.getOffset();
        }
        @Override
        public double getScale() {
            return sampleDim.getScale();
        }
        @Override
        public int[][] getPalette() {
            return sampleDim.getPalette();
        }
        @Override
        public PaletteInterpretation getPaletteInterpretation() {
            return sampleDim.getPaletteInterpretation();
        }
        @Override
        public ColorInterpretation getColorInterpretation() {
            return sampleDim.getColorInterpretation();
        }
        @Override
        public ColorModel getColorModel() {
            return sampleDim.getColorModel();
        }
        @Override
        public ColorModel getColorModel(int visibleBand, int numBands) {
            return sampleDim.getColorModel(visibleBand, numBands);
        }
        @Override
        public ColorModel getColorModel(int visibleBand, int numBands, int type) {
            return sampleDim.getColorModel(visibleBand, numBands, type);
        }

        @Override
        public int hashCode() {
            return sampleDim.hashCode();
        }
        @Override
        public boolean equals(Object object) {
            return sampleDim.equals(object);
        }

        private StringBuilder formatRange(StringBuilder builder, final Locale locale) {
            final NumberRange range = getRange();
            builder.append('[');
            if (range != null) {
                builder.append(range.getMinimum()).append(" ... ").append(range.getMaximum());
            } else {
                final Unit<?> unit = getUnits();
                if (unit != null) {
                    builder.append(unit);
                }
            }
            builder.append(']');
            return builder;
        }

        @Override
        public String toString() {
            if (customCategories != null) {
                StringBuilder builder = new StringBuilder(Classes.getShortClassName(this));
                builder.append('(');
                builder = formatRange(builder, null);
                builder.append(')').append(LINE_SEPARATOR);
                for (final Category category : customCategories) {
                    builder.append("  ").append(/*category == main ? '\u2023' : */' ').append(' ')
                          .append(category).append(LINE_SEPARATOR);
                }
                return builder.toString();
            } else {
                return sampleDim.toString();
            }
        }

        /** The original sample dimension */
        private GridSampleDimension sampleDim;

        /** The custom categories */
        private List<Category> customCategories;

        /** The custom noDataValues */
        private double[] configuredNoDataValues;

        /** The custom unit */
        private Unit<?> configuredUnit;

        /** The custom range */
        private NumberRange<? extends Number> configuredRange;

        /** The custom name */
        private String name;

        /** The custom description */
        private InternationalString configuredDescription;

        public WrappedSampleDimension(GridSampleDimension sampleDim, CoverageDimensionInfo info) {
            super(sampleDim);
            this.name = info.getName();
            final InternationalString sampleDimDescription = sampleDim.getDescription();
            this.configuredDescription = (sampleDimDescription == null || !sampleDimDescription.toString()
                    .equalsIgnoreCase(name)) ? 
                    new SimpleInternationalString(name) : sampleDimDescription;
            this.sampleDim = sampleDim;
            final List<Category> categories = sampleDim.getCategories();
            this.configuredRange = info.getRange();
            this.customCategories = categories;
            final String uom = info.getUnit();
            Unit defaultUnit = sampleDim.getUnits();
            Unit unit = defaultUnit;
            try {
                if (uom != null) {
                    unit = Unit.valueOf(uom);
                }
            } catch (IllegalArgumentException iae) {
                if (LOGGER.isLoggable(Level.WARNING) && defaultUnit != null) {
                    LOGGER.warning("Unable to parse the specified unit (" + uom
                            + "). Using the previous one: " + defaultUnit.toString());
                }
            }
            this.configuredUnit = unit;

            // custom null values 
            final List<Double> nullValues = info.getNullValues();
            if (nullValues != null && nullValues.size() > 0) {
                final int size = nullValues.size();
                configuredNoDataValues = new double[size];
                for (int i = 0; i < size ; i++) {
                    configuredNoDataValues[i] = nullValues.get(i);
                }
            } else {
                this.configuredNoDataValues = sampleDim.getNoDataValues();
            }

            // Check if the nodata has been configured
            boolean nodataConfigured = configuredNoDataValues != null
                    && configuredNoDataValues.length > 0;
            // custom categories
            if (categories != null) {
                this.customCategories = new ArrayList<Category>(categories.size());
                Category wrapped = null;
                for (Category category : categories) {
                    wrapped = category;
                    if (Category.NODATA.getName().equals(category.getName())) {
                        if (category.isQuantitative()) {
                            // Get minimum and maximum value
                            double minimum = nodataConfigured ? configuredNoDataValues[0]
                                    : category.getRange().getMinimum();
                            double maximum = nodataConfigured ? configuredNoDataValues[0]
                                    : category.getRange().getMaximum();
                            if (Double.isNaN(minimum) && Double.isNaN(maximum)) {
                                // Create a qualitative category
                                wrapped = new Category(Category.NODATA.getName(),
                                        category.getColors()[0], minimum);
                            } else {
                                // Create the wrapped category
                                wrapped = new Category(Category.NODATA.getName(),
                                        category.getColors(), NumberRange.create(minimum, maximum));
                            }
                        }
                    }
                    customCategories.add(wrapped);
                }
            }
        }

        private void parseUOM(StringBuilder label, Unit uom) {
            String uomString = uom.toString();
            uomString = uomString.replaceAll("\u00B2", "^2");
            uomString = uomString.replaceAll("\u00B3", "^3");
            uomString = uomString.replaceAll("\u212B", "A");
            uomString = uomString.replaceAll("�", "");
            label.append(uomString);
        }

        private void buildDescription() {
            StringBuilder label = new StringBuilder("GridSampleDimension".intern());
            final Unit uom = sampleDim.getUnits();

            String uName = name.toUpperCase();
            if (uom != null) {
                label.append("(".intern());
                parseUOM(label, uom);
                label.append(")".intern());
            } 
            
            label.append("[".intern());
            label.append(getMinimumValue());
            label.append(",".intern());
            label.append(getMaximumValue());
            label.append("]".intern());
            configuredDescription = new SimpleInternationalString(label.toString());
        }
    }

    @Override
    public DatasetLayout getDatasetLayout() {
        return delegate.getDatasetLayout();
    }

    @Override
    public DatasetLayout getDatasetLayout(String coverageName) {
        return delegate.getDatasetLayout(coverageName);
    }
}
