package org.geoserver.geopkg;

import org.geoserver.data.DataStoreFactoryInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;
import org.vfny.geoserver.util.DataStoreUtils;

import java.io.File;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.classextension.EasyMock.*;
import static org.junit.Assert.assertNotNull;

public class GeoPkgDataStoreFactoryInitializerTest {

    @Test
    public void testInitializer() {
        GeoServerResourceLoader resourceLoader = createMock(GeoServerResourceLoader.class);
        expect(resourceLoader.getBaseDirectory()).andReturn(new File("target")).once();
        replay(resourceLoader);

        GeoPkgDataStoreFactoryInitializer initializer = new GeoPkgDataStoreFactoryInitializer();
        initializer.setResourceLoader(resourceLoader);

        WebApplicationContext appContext = createNiceMock(WebApplicationContext.class);
        expect(appContext.getBeanNamesForType(DataStoreFactoryInitializer.class))
            .andReturn(new String[]{"geopkgDataStoreFactoryInitializer"}).anyTimes();
        expect(appContext.getBean("geopkgDataStoreFactoryInitializer")).andReturn(initializer).anyTimes();
        replay(appContext);

        new GeoServerExtensions().setApplicationContext(appContext);
        assertNotNull(DataStoreUtils.aquireFactory(new GeoPkgDataStoreFactory().getDisplayName()));

        verify(resourceLoader);
    }


}
