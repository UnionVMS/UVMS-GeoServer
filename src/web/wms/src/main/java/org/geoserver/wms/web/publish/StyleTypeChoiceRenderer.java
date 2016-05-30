/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.publish;

import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.geoserver.catalog.StyleType;

@SuppressWarnings("serial")
public class StyleTypeChoiceRenderer implements IChoiceRenderer {

    public Object getDisplayValue(Object object) {
        return object.toString();
    }

    public String getIdValue(Object object, int index) {
        return object.toString();
    }

}
