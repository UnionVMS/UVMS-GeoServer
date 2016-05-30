/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

public class Util {
    private Util() {
        
    }
    
    public static <T> T coalesce(T... val) {
        for(T t : val) {
            if(t != null) {
                return t;
            }
        }

        return null;
    }
}
