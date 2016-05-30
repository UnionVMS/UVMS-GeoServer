/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.*;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.RoleFilterConfig;
import org.geoserver.security.filter.GeoServerRoleFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

@Category(SystemTest.class)
public class GeoServerRoleFilterTest extends GeoServerSecurityTestSupport {

    @Test
    public void testFilterChainWithEnabled() throws Exception {
        
        GeoServerSecurityManager secMgr = getSecurityManager();
        RoleFilterConfig config = new RoleFilterConfig();
        config.setName("roleConverter");
        config.setClassName(GeoServerRoleFilter.class.getName());
        config.setRoleConverterName("roleConverter");
        config.setHttpResponseHeaderAttrForIncludedRoles("ROLES");
        secMgr.saveFilter(config);

        
        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        chain.addFilter(getSecurityManager().loadFilter("roleConverter"));
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        assertEquals(GeoServerRole.ANONYMOUS_ROLE.getAuthority(),response.getHeader("ROLES"));        
    }

    @Test
    public void testFilterChainWithDisabled() throws Exception {

        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        assertNull(response.getHeader("ROLES"));
        
    }
}
