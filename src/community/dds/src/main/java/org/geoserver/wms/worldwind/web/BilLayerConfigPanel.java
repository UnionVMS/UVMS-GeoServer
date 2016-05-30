/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.worldwind.web;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.resource.loader.IStringResourceLoader;
import org.geoserver.web.GeoServerStringResourceLoader;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.wms.worldwind.BilConfig;
import org.geotools.util.logging.Logging;

/**
 * UI panel to configure a the BIL output format for a layer. This panel appears
 * in the Publishing tab of the layer configuration for a raster layer.
 * 
 * @author Parker Abercrombie
 */
@SuppressWarnings("serial")
public class BilLayerConfigPanel extends LayerConfigurationPanel
{
    private static final Logger LOGGER = Logging.getLogger(BilLayerConfigPanel.class);

    @SuppressWarnings("unchecked")
    public BilLayerConfigPanel(String id, IModel<org.geoserver.catalog.LayerInfo> model)
    {
        super(id, model);

        PropertyModel<Object> metadata = new PropertyModel<Object>(model,
                "resource.metadata");

        add(new DropDownChoice<String>(BilConfig.DEFAULT_DATA_TYPE,
                new MapModel(metadata, BilConfig.DEFAULT_DATA_TYPE),
                new ListModel<>(Arrays.asList(
                        "application/bil8",
                        "application/bil16",
                        "application/bil32"))));

        add(new DropDownChoice<String>(BilConfig.BYTE_ORDER,
                new MapModel(metadata, BilConfig.BYTE_ORDER),
                new ListModel<>(Arrays.asList(
                        ByteOrder.BIG_ENDIAN.toString(),
                        ByteOrder.LITTLE_ENDIAN.toString())),
                new ByteOrderRenderer()));

        add(new TextField<Double>(BilConfig.NO_DATA_OUTPUT,
                new MapModel(metadata, BilConfig.NO_DATA_OUTPUT), Double.class));
    }

    /**
     * Renderer to display a localized string for the Byte Order drop down.
     */
    private class ByteOrderRenderer implements IChoiceRenderer<String>
    {

    	public Object getDisplayValue(String str)
    	{
    	    IStringResourceLoader loader = new GeoServerStringResourceLoader();
    	    if (ByteOrder.LITTLE_ENDIAN.toString().equals(str))
        	{
        		return loader.loadStringResource(BilLayerConfigPanel.this,
        		        "BilLayerConfigPanel.byteOrderLittleEndian");
        	}
        	else if (ByteOrder.BIG_ENDIAN.toString().equals(str))
        	{
                return loader.loadStringResource(BilLayerConfigPanel.this,
                        "BilLayerConfigPanel.byteOrderBigEndian");
        	}

    	    LOGGER.warning("Unknown byte order: " + str);
        	return str;
        }

        public String getIdValue(String str, int index)
        {
        	return str;
        }
    }
}
