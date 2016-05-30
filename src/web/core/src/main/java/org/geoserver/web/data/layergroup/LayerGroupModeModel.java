/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.LayerGroupInfo;


/**
 * Simple detachable model listing all the available LayerGroup modes.
 */
@SuppressWarnings({ "serial" })
public class LayerGroupModeModel extends LoadableDetachableModel<List<LayerGroupInfo.Mode>> {
    
    @Override
    protected List<LayerGroupInfo.Mode> load() {
        List<LayerGroupInfo.Mode> modes = new ArrayList<LayerGroupInfo.Mode>();
        modes.add(LayerGroupInfo.Mode.SINGLE);
        modes.add(LayerGroupInfo.Mode.NAMED);
        modes.add(LayerGroupInfo.Mode.CONTAINER);
        modes.add(LayerGroupInfo.Mode.EO);        
        return modes;
    }
}
