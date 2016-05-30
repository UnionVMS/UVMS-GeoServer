/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;

/**
 * {@link GeoServerAuthenticationKeyFilter} configuration object.
 * 
 * {@link #authKeyParamName} is the name of the URL parameter, default
 * is {@link KeyAuthenticationToken#DEFAULT_URL_PARAM}
 * 
 * {@link #authKeyMapperName} is the bean name of an {@link AuthenticationKeyMapper} implementation.
 *
 * @author mcr
 */
public class AuthenticationKeyFilterConfig extends SecurityFilterConfig 
    implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;
    private String authKeyMapperName;
    private String authKeyParamName = KeyAuthenticationToken.DEFAULT_URL_PARAM;
    private String userGroupServiceName;
    private Map<String, String> mapperParameters;
    
       
    @Override
    public  boolean providesAuthenticationEntryPoint() {
        return true;
    }

    public String getAuthKeyMapperName() {
        return authKeyMapperName;
    }

    public void setAuthKeyMapperName(String authKeyMapperName) {
        this.authKeyMapperName = authKeyMapperName;
    }

    public String getAuthKeyParamName() {
        return authKeyParamName;
    }

    public void setAuthKeyParamName(String authKeyParamName) {
        this.authKeyParamName = authKeyParamName;
    }
    
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    /**
     * Returns the mapper parameters.
     * 
     * @return
     */
    public Map<String, String> getMapperParameters() {
        if(mapperParameters == null) {
            mapperParameters = new HashMap<String, String>();
        }
        return mapperParameters;
    }

    /**
     * Sets the mapper parameters.
     * 
     * @param mapperParameters
     */
    public void setMapperParameters(Map<String, String> mapperParameters) {
        this.mapperParameters = mapperParameters;
    }
    
    
}
