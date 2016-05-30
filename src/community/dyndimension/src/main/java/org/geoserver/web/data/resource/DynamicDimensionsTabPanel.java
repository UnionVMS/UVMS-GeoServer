/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.web.wicket.EnumChoiceRenderer;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.ReorderableTablePanel;
import org.geoserver.wms.dimension.DefaultValueConfiguration;
import org.geoserver.wms.dimension.DefaultValueConfiguration.DefaultValuePolicy;
import org.geoserver.wms.dimension.DefaultValueConfigurations;

/**
 * Adds a tab to the layer editor to allow editing the dynamic dimension default values
 * 
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings({"serial", "rawtypes"})
public class DynamicDimensionsTabPanel extends LayerEditTabPanel {

    Property<DefaultValueConfiguration> DIMENSION = new BeanProperty<DefaultValueConfiguration>(
            "dimension", "dimension");

    Property<DefaultValueConfiguration> POLICY = new BeanProperty<DefaultValueConfiguration>(
            "policy", "policy");

    Property<DefaultValueConfiguration> DEFAULT_VALUE_EXPRESSION = new BeanProperty<DefaultValueConfiguration>(
            "defaultValueExpression", "defaultValueExpression");

    ReorderableTablePanel<DefaultValueConfiguration> table;

    List<DefaultValueConfiguration> configurations;

    IModel model;

    public DynamicDimensionsTabPanel(String id, IModel model) {
        super(id, model);

        MetadataMapModel configsModel = new MetadataMapModel(new PropertyModel<MetadataMap>(model,
                "resource.metadata"), DefaultValueConfigurations.KEY,
                DefaultValueConfigurations.class);
        final LayerInfo layer = (LayerInfo) model.getObject();
        configurations = getConfigurations(model, configsModel);
        Editor editor = new Editor("editor", getEnabledDimensionNames(layer), configsModel);
        add(editor);
    }

    private List<DefaultValueConfiguration> getConfigurations(IModel<LayerInfo> layerModel,
            IModel<DefaultValueConfigurations> configModel) {
        ArrayList<DefaultValueConfiguration> result = new ArrayList<DefaultValueConfiguration>();

        // see if we have configs already
        DefaultValueConfigurations configs = configModel.getObject();
        if (configs != null) {
            result.addAll(configs.getConfigurations());
        } else {
            configs = new DefaultValueConfigurations(new ArrayList<DefaultValueConfiguration>());
        }

        // add missing dimension configs
        Set<String> dimensionNames = getEnabledDimensionNames(layerModel.getObject());
        for (String dimensionName : dimensionNames) {
            addIfMissing(dimensionName, result);
        }

        // remove unknown ones
        for (Iterator it = result.iterator(); it.hasNext();) {
            DefaultValueConfiguration config = (DefaultValueConfiguration) it.next();
            if (!dimensionNames.contains(config.getDimension())) {
                it.remove();
            }

        }

        configs.getConfigurations().clear();
        configs.getConfigurations().addAll(result);
        return result;
    }

    Set<String> getEnabledDimensionNames(LayerInfo layer) {
        Set<String> dimensionNames = new HashSet<String>();
        for (Map.Entry<String, Serializable> entry : layer.getResource().getMetadata().entrySet()) {
            String key = entry.getKey();
            Serializable md = entry.getValue();
            if (md instanceof DimensionInfo) {
                // skip disabled dimensions
                DimensionInfo di = (DimensionInfo) md;
                if (!di.isEnabled()) {
                    continue;
                }

                // get the dimension name
                String dimensionName;
                if (key.startsWith(ResourceInfo.CUSTOM_DIMENSION_PREFIX)) {
                    dimensionName = key.substring(ResourceInfo.CUSTOM_DIMENSION_PREFIX.length());
                } else {
                    dimensionName = key;
                }
                dimensionNames.add(dimensionName);
            }
        }

        return dimensionNames;
    }

    private void addIfMissing(String dimension, ArrayList<DefaultValueConfiguration> configs) {
        for (DefaultValueConfiguration config : configs) {
            if (dimension.equals(config.getDimension())) {
                return;
            }
        }

        configs.add(new DefaultValueConfiguration(dimension, DefaultValuePolicy.STANDARD));

    }

    class Editor extends FormComponentPanel<DefaultValueConfigurations> {

        public Editor(String id, final Collection<String> enabledDimensionNames,
                IModel<DefaultValueConfigurations> model) {
            super(id, model);
            List<Property<DefaultValueConfiguration>> properties = Arrays.asList(DIMENSION, POLICY,
                    DEFAULT_VALUE_EXPRESSION);

            table = new ReorderableTablePanel<DefaultValueConfiguration>("defaultConfigs",
                    configurations, properties) {

                @Override
                protected Component getComponentForProperty(String id, final IModel itemModel,
                        Property<DefaultValueConfiguration> property) {
                    if (DEFAULT_VALUE_EXPRESSION.equals(property)) {
                        Fragment f = new Fragment(id, "ecqlEditor", DynamicDimensionsTabPanel.this);
                        TextArea<String> ta = new TextArea<String>("editor");
                        // a dimension expression cannot refer to itself
                        List<String> otherDimensions = new ArrayList<String>(enabledDimensionNames);
                        String currentDimension = ((DefaultValueConfiguration) itemModel
                                .getObject()).getDimension();
                        otherDimensions.remove(currentDimension);
                        ta.add(new ECQLValidator().setValidAttributes(otherDimensions));
                        ta.setModel(property.getModel(itemModel));
                        ta.setOutputMarkupId(true);
                        Object currentPolicy = POLICY.getModel(itemModel).getObject();
                        ta.setVisible(DefaultValuePolicy.EXPRESSION.equals(currentPolicy));
                        f.add(ta);
                        return f;
                    } else if (POLICY.equals(property)) {
                        Fragment f = new Fragment(id, "policyChoice",
                                DynamicDimensionsTabPanel.this);
                        final DropDownChoice<DefaultValuePolicy> dd = new DropDownChoice<DefaultValueConfiguration.DefaultValuePolicy>(
                                "choice", Arrays.asList(DefaultValuePolicy.values()));
                        dd.setChoiceRenderer(new EnumChoiceRenderer(DynamicDimensionsTabPanel.this));
                        dd.setModel(property.getModel(itemModel));
                        f.add(dd);
                        return f;
                    }

                    return null;
                }

                @Override
                protected void onPopulateItem(final Property<DefaultValueConfiguration> property,
                        final ListItem item) {
                    super.onPopulateItem(property, item);

                    // assuming that if we got here, everything before it has been populated
                    if (property == DEFAULT_VALUE_EXPRESSION) {
                        MarkupContainer parent = item.getParent();
                        // drill down into the containers to get the actual form components we want
                        final DropDownChoice dd = (DropDownChoice) ((Fragment) ((ListItem) parent
                                .get(3))
                                .get(0)).get(0);
                        final TextArea ta = (TextArea) ((Fragment) ((ListItem) parent.get(4))
                                .get(0)).get(0);
                        dd.add(new OnChangeAjaxBehavior() {

                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                DefaultValuePolicy currentPolicy = (DefaultValuePolicy) dd
                                        .getConvertedInput();
                                ta.setVisible(DefaultValuePolicy.EXPRESSION.equals(currentPolicy));
                                target.addComponent(ta);
                                target.addComponent(table);
                            }
                        });
                    }

                    item.add(new AbstractBehavior() {

                        public void onComponentTag(Component component, ComponentTag tag) {
                            if (property == DEFAULT_VALUE_EXPRESSION) {
                                tag.put("style", "width:99%");
                            } else {
                                tag.put("style", "width:1%");
                            }

                        }
                    });
                }

            };
            table.setFilterable(false);
            table.setSortable(false);
            table.setPageable(false);
            table.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
            table.setOutputMarkupId(true);

            add(table);

            add(new IValidator<DefaultValueConfigurations>() {

                @Override
                public void validate(IValidatable<DefaultValueConfigurations> validatable) {
                    DefaultValueConfigurations configurations = validatable.getValue();
                    for (DefaultValueConfiguration config : configurations.getConfigurations()) {
                        if (config.getPolicy() == DefaultValuePolicy.EXPRESSION
                                && config.getDefaultValueExpression() == null) {
                            error(new ParamResourceModel("expressionRequired",
                                    DynamicDimensionsTabPanel.this).getString());
                        }
                    }

                }

            });
        }

        @Override
        protected void convertInput() {
            visitChildren(TextArea.class, new org.apache.wicket.Component.IVisitor() {
                public Object component(Component component) {
                    ((FormComponent) component).updateModel();
                    return null;
                }
            });
            setConvertedInput(new DefaultValueConfigurations(configurations));
        }

    };

}
