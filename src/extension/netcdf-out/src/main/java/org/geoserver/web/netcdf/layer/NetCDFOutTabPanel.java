/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf.layer;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.data.resource.LayerEditTabPanel;
import org.geoserver.web.netcdf.NetCDFSettingsContainer;
import org.geoserver.web.util.MetadataMapModel;

/**
 * {@link LayerEditTabPanel} implementation for configuring NetCDF output settings
 */
public class NetCDFOutTabPanel extends LayerEditTabPanel {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public NetCDFOutTabPanel(String id, IModel<LayerInfo> model, IModel<ResourceInfo> resourceModel) {
        super(id, model);

        if (resourceModel.getObject() != null) {
            // Selection of the IModel associated to the metadata map
            final PropertyModel<MetadataMap> metadata = new PropertyModel<MetadataMap>(
                    resourceModel, "metadata");
            // Selection of the CoverageInfo model
            IModel<CoverageInfo> cmodel = null;
            if (resourceModel.getObject() instanceof CoverageInfo) {
                CoverageInfo cinfo = (CoverageInfo) resourceModel.getObject();
                cmodel = new Model<CoverageInfo>(cinfo);
            }

            // Getting the NetcdfSettingsContainer model from MetadataMap
            IModel<NetCDFLayerSettingsContainer> netcdfModel = new MetadataMapModel(metadata,
                    NetCDFSettingsContainer.NETCDFOUT_KEY, NetCDFLayerSettingsContainer.class);
            NetCDFOutSettingsEditor editor = new NetCDFOutSettingsEditor("netcdfeditor",
                    netcdfModel, cmodel);
            add(editor);
            setVisible(true);
        } else {
            add(new Label("netcdfeditor", new ResourceModel("NetCDFOutTabPanel.invalid")));
            setVisible(false);
        }
    }
}
