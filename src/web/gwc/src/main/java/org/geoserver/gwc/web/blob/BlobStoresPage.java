/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.blob;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.geoserver.gwc.GWC;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geowebcache.config.BlobStoreConfig;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.layer.TileLayer;

/**
 * 
 * Panel with table of all blobstores.
 *
 * @author Niels Charlier
 *
 */
public class BlobStoresPage extends GeoServerSecuredPage {

    private AjaxLink<Object> remove;

    private GeoServerTablePanel<BlobStoreConfig> blobStoresPanel;

    private GeoServerDialog dialog;

    public BlobStoresPage() {

        add(dialog = new GeoServerDialog("confirmDeleteDialog"));
        dialog.setTitle(new ParamResourceModel("confirmDeleteDialog.title", getPage()));
        dialog.setInitialHeight(200);

        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new AjaxLink<Object>("addNew") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new BlobStorePage());
            }
        });

        // the removal button
        header.add(remove = new AjaxLink<Object>("removeSelected") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {

                final Set<String> ids = new HashSet<String>();
                final List<String> assignedLayers = new ArrayList<String>();

                for (BlobStoreConfig config : blobStoresPanel.getSelection()) {
                    if (config.isDefault()) {
                        error(new ParamResourceModel("deleteError", getPage()).getString());
                        target.addComponent(feedbackPanel);
                        return;
                    }
                    ids.add(config.getId());
                }

                for (TileLayer layer : GWC.get().getTileLayers()) {
                    if (layer.getBlobStoreId() != null) {
                        if (ids.contains(layer.getBlobStoreId())) {
                            assignedLayers.add(layer.getName());
                        }
                    }
                }
                if (assignedLayers.size() > 0) {
                    dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                        private static final long serialVersionUID = 5257987095800108993L;

                        private String error = null;

                        @Override
                        protected Component getContents(String id) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(new ParamResourceModel("confirmDeleteDialog.content",
                                    getPage()).getString());
                            for (String layerName : assignedLayers) {
                                sb.append("\n&nbsp;&nbsp;");
                                sb.append(escapeHtml(layerName));
                            }
                            return new MultiLineLabel("userPanel", sb.toString())
                                    .setEscapeModelStrings(false);
                        }

                        @Override
                        protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                            try {
                                GWC.get().removeBlobStores(ids);
                                for (String layerName : assignedLayers) {
                                    TileLayer layer = GWC.get().getTileLayerByName(layerName);
                                    layer.setBlobStoreId(null);
                                    GWC.get().save(layer);
                                }
                            } catch (ConfigurationException e) {
                                error = e.getMessage();
                            }
                            return true;
                        }

                        @Override
                        public void onClose(AjaxRequestTarget target) {
                            if (error != null) {
                                error(error);
                                target.addComponent(feedbackPanel);
                            } else {
                                target.addComponent(blobStoresPanel);
                            }
                        }

                    });
                } else {
                    try {
                        GWC.get().removeBlobStores(ids);
                    } catch (ConfigurationException e) {
                        error(e.toString());
                        target.addComponent(feedbackPanel);
                    }
                    target.addComponent(blobStoresPanel);
                }
            }
        });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        setHeaderPanel(header);

        // the panel
        add(blobStoresPanel = new GeoServerTablePanel<BlobStoreConfig>("storesPanel",
                new BlobStoresProvider(), true) {
            private static final long serialVersionUID = -5380703588873422601L;

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<BlobStoreConfig> property) {
                final BlobStoreConfig blobStore = (BlobStoreConfig) itemModel.getObject();
                if (property == BlobStoresProvider.ID) {
                    return new SimpleAjaxLink(id, itemModel, property.getModel(itemModel)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            setResponsePage(new BlobStorePage(blobStore));
                        }
                    };
                } else if (property == BlobStoresProvider.DEFAULT) {
                    if (blobStore.isDefault()) {
                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                    } else {
                        return new Label(id, "");
                    }
                } else if (property == BlobStoresProvider.ENABLED) {
                    if (blobStore.isEnabled()) {
                        return new Icon(id, CatalogIconFactory.ENABLED_ICON);
                    } else {
                        return new Label(id, "");
                    }
                } else if (property == BlobStoresProvider.TYPE) {
                    return new Label(id, BlobStoreTypes.getFromClass(blobStore.getClass()).toString());
                }
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(blobStoresPanel.getSelection().size() > 0);
                target.addComponent(remove);
            }
        });
        blobStoresPanel.setOutputMarkupId(true);
    }

}
