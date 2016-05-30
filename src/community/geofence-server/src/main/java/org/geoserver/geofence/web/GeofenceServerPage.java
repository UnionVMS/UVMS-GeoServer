/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.geofence.services.dto.ShortRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import wicketdnd.*;
import wicketdnd.theme.WebTheme;

/**
 * GeoFence Server wicket administration UI for GeoServer.
 * 
 * @author Niels Charlier
 *
 */
@SuppressWarnings("serial")
public class GeofenceServerPage extends GeoServerSecuredPage { 
    
    private GeofenceRulesModel rulesModel;
    
    private GeoServerTablePanel<ShortRule> rulesPanel;
    
    private AjaxLink<Object> remove;
        
    public GeofenceServerPage() {              
                
        // the add button
        add(new AjaxLink<Object>("addNew") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(new GeofenceRulePage(rulesModel.newRule(), rulesModel));
            }
        });
        
        // the removal button
        add(remove = new AjaxLink<Object>("removeSelected") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                rulesModel.remove(rulesPanel.getSelection());
                target.addComponent(rulesPanel);
            }  
        });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);
         
        //the panel
        add(rulesPanel =  new GeoServerTablePanel<ShortRule>("rulesPanel", 
                rulesModel = new GeofenceRulesModel(), true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel, Property<ShortRule> property) {

                if (property == GeofenceRulesModel.BUTTONS) {
                    return new ButtonPanel(id, (ShortRule) itemModel.getObject());
                } 
                
                return null;
            }

            @Override
            protected void onSelectionUpdate(AjaxRequestTarget target) {
                remove.setEnabled(rulesPanel.getSelection().size() > 0);
                target.addComponent(remove);
            }
        });
        rulesPanel.add(CSSPackageResource.getHeaderContribution(new WebTheme()));
        rulesPanel.add(new DragSource(Operation.MOVE).drag("tr"));
        rulesPanel.add(new DropTarget(Operation.MOVE) {
            public void onDrop(AjaxRequestTarget target, Transfer transfer, Location location) {
                if (location == null || !(location.getComponent().getDefaultModel().getObject() instanceof ShortRule)) {
                    return;
                }
                ShortRule movedRule = transfer.getData();
                ShortRule targetRule = (ShortRule) location.getComponent().getDefaultModel().getObject();
                if (movedRule.getId().equals(targetRule.getId())) {
                return;
                }
                if (movedRule.getPriority() < targetRule.getPriority()) {
                    movedRule.setPriority(targetRule.getPriority() + 1);
                } else {
                    movedRule.setPriority(targetRule.getPriority());
                }
                rulesModel.save(movedRule);
                    doReturn(GeofenceServerPage.class);
                }
            }.dropCenter("tr"));
        rulesPanel.setOutputMarkupId(true);
    }
     
    /**
     * 
     * Panel with buttons up, down and edit
     *
     */
    private class ButtonPanel extends Panel {
        
        private ImageAjaxLink upLink;
        private ImageAjaxLink downLink;      
        
        public ButtonPanel( String id, final ShortRule rule ) {
            super( id );
            this.setOutputMarkupId(true);
            
            upLink = new ImageAjaxLink( "up", new ResourceReference( getClass(), "img/arrow_up.png") ) {                                                                                       
                private static final long serialVersionUID = -8179503447106596760L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    rulesModel.moveUp(rule);
                    target.addComponent(rulesPanel);
                }
                
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if (rulesModel.canUp(rule)) {
                        tag.put("style", "visibility:visible");
                    } else {
                        tag.put("style", "visibility:hidden");
                    }
                }
            };
            upLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("GeofenceServerPage.up", upLink)));
            upLink.setOutputMarkupId(true);
            add(upLink);            

            downLink = new ImageAjaxLink( "down", new ResourceReference( getClass(), "img/arrow_down.png") ) {
                private static final long serialVersionUID = 4640187752303674221L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    rulesModel.moveDown(rule);
                    target.addComponent(rulesPanel);           
                }
                
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if (rulesModel.canDown(rule)) {
                        tag.put("style", "visibility:visible");
                    } else {
                        tag.put("style", "visibility:hidden");
                    }
                }
            };
            downLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("GeofenceServerPage.down", downLink)));
            downLink.setOutputMarkupId(true);
            add(downLink);
            
            ImageAjaxLink editLink = new ImageAjaxLink( "edit", new ResourceReference( getClass(), "img/edit.png") ) {
                private static final long serialVersionUID = 4640187752303674221L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    setResponsePage(new GeofenceRulePage(rule, rulesModel));
                }
            };
            editLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("GeofenceServerPage.edit", editLink)));
            editLink.setOutputMarkupId(true);            
            add(editLink);
        }
    }
    
}
