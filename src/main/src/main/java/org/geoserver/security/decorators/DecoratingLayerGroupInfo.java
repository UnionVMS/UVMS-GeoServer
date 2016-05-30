/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AbstractDecorator;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Delegates every method to the wrapped {@link LayerGroupInfo}. Subclasses will
 * override selected methods to perform their "decoration" job
 * 
 * @author Andrea Aime
 */
public class DecoratingLayerGroupInfo extends AbstractDecorator<LayerGroupInfo> implements LayerGroupInfo {

    public DecoratingLayerGroupInfo(LayerGroupInfo delegate) {
        super(delegate);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return delegate.getBounds();
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public LayerInfo getRootLayer() {
        return delegate.getRootLayer();
    }
    
    @Override
    public StyleInfo getRootLayerStyle() {
        return delegate.getRootLayerStyle();
    }   
    
    @Override
    public List<PublishedInfo> getLayers() {
        return delegate.getLayers();
    }
    
    @Override
    public List<LayerInfo> layers() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allLayersForRendering();
    }

    @Override
    public List<StyleInfo> styles() {
        LayerGroupHelper helper = new LayerGroupHelper(this);
        return helper.allStylesForRendering();
    }  
    
    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Mode getMode() {
        return delegate.getMode();
    }
    
    @Override
    public WorkspaceInfo getWorkspace() {
        return delegate.getWorkspace();
    }

    @Override
    public String prefixedName() {
        return delegate.prefixedName();
    }

    @Override
    public List<StyleInfo> getStyles() {
        return delegate.getStyles();
    }

    @Override
    public void setRootLayer(LayerInfo rootLayer) {
        delegate.setRootLayer(rootLayer);
    }

    @Override
    public void setRootLayerStyle(StyleInfo style) {
        delegate.setRootLayerStyle(style);
    }    
    
    @Override
    public void setBounds(ReferencedEnvelope bounds) {
        delegate.setBounds(bounds);
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setMode(Mode mode) {
        delegate.setMode(mode);
    }
    
    @Override
    public void setWorkspace(WorkspaceInfo workspace) {
        delegate.setWorkspace(workspace);
    }

    @Override
    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }
    
    @Override
    public String getTitle() {
        return delegate.getTitle();
    }
    
    @Override
    public void setTitle(String title) {
        delegate.setTitle(title);
    }
    
    @Override
    public String getAbstract() {
        return delegate.getAbstract();
    }
    
    @Override
    public void setAbstract(String abstractTxt) {
        delegate.setAbstract(abstractTxt);
    }    
    
    @Override
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }
    
    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return delegate.getAuthorityURLs();
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return delegate.getIdentifiers();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(delegate).append(
                ']').toString();
    }

    @Override
    public String getPrefixedName() {
        return delegate.prefixedName();
    }

    @Override
    public PublishedType getType() {
        return delegate.getType();
    }
}
