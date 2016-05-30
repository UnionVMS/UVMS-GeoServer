/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.util.List;
import java.util.Map;

/**
 * Defines the common interface of format conversion tools.
 * 
 * @author Stefano Costa, GeoSolutions
 */
public interface FormatConverter {

    /**
     * Returns the tool's executable full path.
     * 
     * @return
     */
    public String getExecutable();

    /**
     * Sets the tool's executable full path.
     * 
     * @param executable
     */
    public void setExecutable(String executable);

    /**
     * Returns the environment variables that are set prior to invoking the tool's executable.
     * 
     * @return
     */
    public Map<String, String> getEnvironment();

    /**
     * Provides the environment variables that are set prior to invoking the tool's executable.
     * 
     * @return
     */
    public void setEnvironment(Map<String, String> environment);

    /**
     * Adds an output format among the supported ones.
     * 
     * @param format
     */
    public void addFormat(Format format);

    /**
     * Get a list of supported output formats.
     *
     * @return
     */
    public List<Format> getFormats();

    /**
     * Programmatically removes all formats.
     */
    public void clearFormats();

    /**
     * Replaces currently supported formats with the provided list.
     *  
     * @param formats
     */
    public void replaceFormats(List<Format> formats);

}
