/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.config.SecurityNamedServiceConfig;


/**
 * Security provider for auth-key authentication
 * 
 * @author mcr
 */

public class GeoServerAuthenticationKeyProvider extends AbstractFilterProvider {
    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("authKeyAuthentication", AuthenticationKeyFilterConfig.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerAuthenticationKeyFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerAuthenticationKeyFilter();
    }
    
    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new AuthenticationKeyFilterConfigValidator(securityManager);
    }



}







