package org.geoserver.wps.hz;

import org.geoserver.wps.AbstractProcessStoreTest;
import org.geoserver.wps.ProcessStatusStore;
import org.junit.After;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.test.TestHazelcastInstanceFactory;

/**
 * Tests the hazelcast based process status store with a single hazelcast instance
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class HazelcastStatusStoreTest extends AbstractProcessStoreTest {

    private TestHazelcastInstanceFactory factory;

    @Override
    protected ProcessStatusStore buildStore() {
        // build a hazelcast instance isolated from the network
        Config config = new Config();
        config.addMapConfig(new MapConfig(HazelcastStatusStore.EXECUTION_STATUS_MAP));
        factory = new TestHazelcastInstanceFactory(1);
        HazelcastInstance instance = factory
                .newHazelcastInstance(config);
        HazelcastLoader loader = new HazelcastLoader(instance);
        return new HazelcastStatusStore(loader);
    }

    @After
    public void shutdown() {
        factory.shutdownAll();
    }

}
