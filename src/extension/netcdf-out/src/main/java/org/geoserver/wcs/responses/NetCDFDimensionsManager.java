/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.geoserver.wcs2_0.response.DimensionBean;
import org.geoserver.wcs2_0.response.DimensionBean.DimensionType;
import org.geotools.coverage.io.netcdf.crs.NetCDFCoordinateReferenceSystemType.NetCDFCoordinate;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Dimension;

/**
 * Provides mapping between a Coverage {@link DimensionBean}, a NetCDF {@link Dimension} 
 * as well as the related dimension values (the coordinates).
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
class NetCDFDimensionsManager {
    /**
     * A dimension mapping between dimension names and dimension mapper 
     * instances We use a Linked map to preserve the dimension order
     */
    private Map<String, NetCDFDimensionMapping> netcdfDimensions = new LinkedHashMap<String, NetCDFDimensionMapping>();

    public final int getNumDimensions() {
        return netcdfDimensions.keySet().size();
    }

    public void add(String name, NetCDFDimensionMapping mapper) {
        netcdfDimensions.put(name, mapper);
    }

    public Collection<NetCDFDimensionMapping> getDimensions() {
        return netcdfDimensions.values();
    }

    public void addDimensions (Map<String, NetCDFDimensionMapping> mapping) {
        netcdfDimensions.putAll(mapping);
    }

    public void dispose() {
        if (netcdfDimensions != null) {
            netcdfDimensions.clear();
            netcdfDimensions = null;
        }
    }

    /**
     * A NetCDFDimensionMapping class to associate to a dimension:
     * - the input coverageDimension, 
     * - the output netCDFDimension, 
     * - the available values for the coordinate variable of that dimension
     */
    static class NetCDFDimensionMapping {

        public NetCDFDimensionMapping(String name) {
            super();
            this.name = name;
        }

        /**
         * The available (sorted) values for a Dimension
         */
        private DimensionValues dimensionValues;

        /**
         * The input coverage Dimension (a {@link DimensionBean} instance)
         */
        private DimensionBean coverageDimension;

        /** 
         * The output netCDF dimension (a {@link Dimension} instance)
         */
        private Dimension netCDFDimension;

        /**
         * the name of this dimension manager
         */
        private String name;

        @Override
        public String toString() {
            return "NetCDFDimensionMapping [name=" + name + " coverageDimension=" + coverageDimension
                    + ", netCDFDimension=" + netCDFDimension + "]";
        }

        public DimensionValues getDimensionValues() {
            return dimensionValues;
        }

        public void setDimensionValues(DimensionValues dimensionValues) {
            this.dimensionValues = dimensionValues;
        }

        public DimensionBean getCoverageDimension() {
            return coverageDimension;
        }

        public void setCoverageDimension(DimensionBean coverageDimension) {
            this.coverageDimension = coverageDimension;
        }

        public Dimension getNetCDFDimension() {
            return netCDFDimension;
        }

        public void setNetCDFDimension(Dimension netCDFDimension) {
            this.netCDFDimension = netCDFDimension;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        /**
         * A simple interface to deal with the values of a Dimension
         */
        interface DimensionValues {

            abstract Object getValues();

            abstract void addValue(Object object);

            abstract int getSize();
        }

        /**
         * A DimensionValues based on Set of objects
         * 
         */
        static class DimensionValuesSet implements DimensionValues {
            Set<Object> values;

            public DimensionValuesSet(Set<Object> set) {
                values = set;
            }

            @Override
            public Set getValues() {
                return values;
            }

            @Override
            public void addValue(Object object) {
                values.add(object);
            }

            @Override
            public int getSize() {
                return values.size();
            }
        }

        /**
         * A DimensionValues based on Array
         */
        static class DimensionValuesArray implements DimensionValues {
            Array values;

            public DimensionValuesArray(Array data) {
                values = data;
            }

            @Override
            public Array getValues() {
                return (Array) values;
            }

            @Override
            public void addValue(Object object) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getSize() {
                return (int) values.getSize();
            }
        }

        public void dispose() {
            dimensionValues = null;
            netCDFDimension = null;
        }

        /**
         * Get the values for a Dimension wrapped by its specific DimensionManager and return them as
         * a NetCDF Array object
         * 
         * @param rangeValues specify whether the data should be returned as a 1D array or a 2D array 
         * (the latter for dimensions having ranges)
         * @param netCDFCoordinates used to check whether a dimension is related to a coordinate. In that case, 
         * just return the coordinate values.
         * @return
         */
        public Array getDimensionData(final boolean rangeValues, NetCDFCoordinate[] netCDFCoordinates) {
            final String dimensionName = getName();

            // Special management for latitude and logitude
            boolean is2DCoordinate = false;
            if (netCDFCoordinates != null && netCDFCoordinates.length > 0) {
                for (NetCDFCoordinate coordinate : netCDFCoordinates) {
                    if (dimensionName.equalsIgnoreCase(coordinate.getDimensionName())) {
                        is2DCoordinate = true;
                        break;
                    }
                }
            }
            if (is2DCoordinate) {
                return (Array) getDimensionValues().getValues();
            } else {

                // Get Dimension information
                DimensionBean bean = getCoverageDimension();
                DimensionType type = bean.getDimensionType();
                final String dataType = bean.getDatatype();
                boolean isTime = false;
                if (type == DimensionType.TIME || NetCDFUtilities.isATime(dataType)) {
                    isTime = true;
                }

                // Get Dimension values
                final DimensionValues dimensionValues = getDimensionValues();
                final Set<Object> values = (Set<Object>) dimensionValues.getValues();
                final int numElements = values.size();

                final String dimensionDataType = getCoverageDimension().getDatatype();
                final DataType netCDFDataType = NetCDFUtilities.getNetCDFDataType(dimensionDataType);

                // Get a proper array to contain the dimension values
                final int[] dimensionSize = rangeValues ? new int[] { numElements, 2 } : new int[] {numElements};
                final Array data = NetCDFUtilities.getArray(dimensionSize, netCDFDataType);

                final Index index = data.getIndex();
                final Iterator<Object> valuesIterator = values.iterator();
                final int indexing[] = new int[rangeValues ? 2: 1];

                // Setting array values
                for (int pos = 0; pos < numElements; pos++) {
                    indexing[0] = pos;
                    Object value = valuesIterator.next();
                    data.setObject(index.set(indexing), getValue(value, isTime, false));
                    if (rangeValues) {
                        indexing[1] = 1;
                        data.setObject(index.set(indexing), getValue(value, isTime, rangeValues));
                        indexing[1] = 0;
                    }
                }
                return data;
            }
        }
        
        /**
         * Get the value from the input object. Take care of time elements since they need
         * to be referred to the time origin
         * 
         * @param input 
         * @param isTime does this object represents a temporal entity?
         * @param endValue specify whether it needs to return the second value of a range
         * @return
         */
        private Object getValue(Object input, boolean isTime, boolean endValue) {
            if (isTime) {
                return getTime(input, endValue);
            } else if (input instanceof NumberRange) {
                NumberRange range = (NumberRange) input;
                return endValue ? range.getMaxValue() : range.getMinValue();
            } 
            //Simply return back the value
            return input;
        }

        /**
         * Return the time value for this object. Note that times are referred with respect to
         * an origin {@link NetCDFUtilities#START_TIME}.
         * @param input
         * @param endTime specify whether it needs to return the second value of a time range
         * @return
         */
        private Double getTime(Object input, boolean endTime) {
            long time = 0;
            if (input instanceof Timestamp) {
                time = ((Timestamp) input).getTime();
            } else if (input instanceof DateRange) {
                if (!endTime) {
                    time = ((DateRange) input).getMinValue().getTime();
                } else {
                    time = ((DateRange) input).getMaxValue().getTime();
                }
            } else if (input instanceof Date){
                time = ((Date)input).getTime();
            } else {
                throw new IllegalArgumentException("Unsupported time");
            }
            // Convert to seconds since START_TIME
            return ((double)(time - NetCDFUtilities.START_TIME))/1000d;
        }
    }
}
