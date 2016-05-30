/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geowebcache.s3.S3BlobStoreConfig;

/**
 *
 * Panel for S3BlobStore
 *
 * @author Niels Charlier
 */
public class S3BlobStorePanel extends Panel {

    private static final long serialVersionUID = -8237328668463257329L;

    public S3BlobStorePanel(String id, final IModel<S3BlobStoreConfig> configModel) {
        super(id, configModel);

        add(new TextField<String>("bucket").setRequired(true).add(
                new AttributeModifier("title", true, new ResourceModel("bucket.title"))));
        add(new TextField<String>("awsAccessKey").setRequired(true).add(
                new AttributeModifier("title", true, new ResourceModel("awsAccessKey.title"))));
        add(new TextField<String>("awsSecretKey").setRequired(true).add(
                new AttributeModifier("title", true, new ResourceModel("awsSecretKey.title"))));
        add(new TextField<String>("prefix").add(new AttributeModifier("title", true,
                new ResourceModel("prefix.title"))));
        add(new TextField<Integer>("maxConnections").setRequired(true).add(
                new AttributeModifier("title", true, new ResourceModel("maxConnections.title"))));
        add(new CheckBox("useHTTPS").add(new AttributeModifier("title", true, new ResourceModel(
                "useHTTPS.title"))));
        add(new TextField<String>("proxyDomain").add(new AttributeModifier("title", true,
                new ResourceModel("proxyDomain.title"))));
        add(new TextField<String>("proxyWorkstation").add(new AttributeModifier("title", true,
                new ResourceModel("proxyWorkstation.title"))));
        add(new TextField<String>("proxyHost").add(new AttributeModifier("title", true,
                new ResourceModel("proxyHost.title"))));
        add(new TextField<Integer>("proxyPort").add(new AttributeModifier("title", true,
                new ResourceModel("proxyPort.title"))));
        add(new TextField<String>("proxyUsername").add(new AttributeModifier("title", true,
                new ResourceModel("proxyUsername.title"))));
        add(new TextField<String>("proxyPassword").add(new AttributeModifier("title", true,
                new ResourceModel("proxyPassword.title"))));
        add(new CheckBox("useGzip").add(new AttributeModifier("title", true, new ResourceModel(
                "useGzip.title"))));
    }

}
