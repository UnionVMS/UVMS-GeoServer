/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.data.store.panel.DirectoryParamPanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geowebcache.config.FileBlobStoreConfig;

/**
 *
 * Panel for FileBlobStore
 *
 * @author Niels Charlier
 */
public class FileBlobStorePanel extends Panel {

    private static final long serialVersionUID = -8237328668463257329L;

    public FileBlobStorePanel(String id, final IModel<FileBlobStoreConfig> configModel) {
        super(id, configModel);

        DirectoryParamPanel paramPanel;
        add(paramPanel = new DirectoryParamPanel("baseDirectory", new PropertyModel<String>(
                configModel.getObject(), "baseDirectory"), new ParamResourceModel("baseDirectory",
                this), true));
        paramPanel.add(new AttributeModifier("title", true,
                new ResourceModel("baseDirectory.title")));
        paramPanel.getFormComponent().setModel(paramPanel.getDefaultModel()); // disable filemodel
        paramPanel.setFileFilter(new Model<DirectoryFileFilter>(
                (DirectoryFileFilter) DirectoryFileFilter.INSTANCE));
        add(new TextField<Integer>("fileSystemBlockSize").setRequired(true)
                .add(new AttributeModifier("title", true, new ResourceModel(
                        "fileSystemBlockSize.title"))));
    }

}
