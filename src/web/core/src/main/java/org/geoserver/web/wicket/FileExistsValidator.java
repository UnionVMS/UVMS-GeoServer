/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geotools.util.Converters;

/**
 * Checks the specified file exists on the file system, including checks in the data directory
 */
@SuppressWarnings("serial")
public class FileExistsValidator extends AbstractValidator {
    
    private UrlValidator delegate;
    
    /** Optional base directory for use during testing */
    File baseDirectory = null;
    
    /**
     * Checks the file exists on the local file system
     */
    public FileExistsValidator() {
        this(true);
    }

    /**
     * If <code>allowRemoveUrl</code> is true this validator allows the file to be either
     * local (no URI scheme, or file URI scheme) or a remote 
     * @param allowRemoteUrl
     */
    public FileExistsValidator(boolean allowRemoteUrl) {
        if(allowRemoteUrl) {
            this.delegate = new UrlValidator();
        } 
    }
    
    @Override
    protected void onValidate(IValidatable validatable) {
        String uriSpec = Converters.convert(validatable.getValue(), String.class);
        
        // Make sure we are dealing with a local path
        try {
            URI uri = new URI(uriSpec);
            if(uri.getScheme() != null && !"file".equals(uri.getScheme())) {
                if(delegate != null) {
                    delegate.validate(validatable);
                    InputStream is = null;
                    try {
                        URLConnection connection = uri.toURL().openConnection();
                        connection.setConnectTimeout(10000);
                        is = connection.getInputStream();
                    } catch(Exception e) {
                        error(validatable, "FileExistsValidator.unreachable", 
                                Collections.singletonMap("file", uriSpec));
                    } finally {
                        IOUtils.closeQuietly(is);
                    }
                }
                return;
            } else {
                // ok, strip away the scheme and just get to the path
                String path = uri.getPath();
                if(path != null && new File(path).exists()) {
                    return;
                }
            }
        } catch(URISyntaxException e) {
            // may be a windows path, move on               
        }
        
        File relFile = null;

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        if (baseDirectory != null ){
            // local to provided baseDirectory
            relFile = Files.url(baseDirectory, uriSpec);
        }
        else if( loader != null ){
            // local to data directory?
            relFile = loader.url(uriSpec);
        }

        if (relFile == null || !relFile.exists()) {
            error(validatable, "FileExistsValidator.fileNotFoundError",
                    Collections.singletonMap("file", uriSpec));
        }
    }

}
