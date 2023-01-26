package org.example.sink;

import com.linkedin.venice.meta.Version;
import com.linkedin.venice.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.schema.GenericObject;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.Sink;
import org.apache.pulsar.io.core.SinkContext;
import org.apache.samza.config.MapConfig;
import org.example.simplewriter.VeniceSystemFactory;
import org.example.simplewriter.VeniceSystemProducer;

import java.util.HashMap;
import java.util.Map;

import static com.linkedin.venice.CommonConfigKeys.SSL_ENABLED;
import static org.example.simplewriter.VeniceSystemFactory.DEPLOYMENT_ID;
import static org.example.simplewriter.VeniceSystemFactory.DOT;
import static org.example.simplewriter.VeniceSystemFactory.SYSTEMS_PREFIX;
import static org.example.simplewriter.VeniceSystemFactory.VENICE_AGGREGATE;
import static org.example.simplewriter.VeniceSystemFactory.VENICE_PUSH_TYPE;
import static org.example.simplewriter.VeniceSystemFactory.VENICE_STORE;

@Slf4j
public class VeniceSink implements Sink<GenericObject> {

    VeniceSinkConfig config;
    VeniceSystemProducer producer;

    @Override
    public void open(Map<String, Object> config, SinkContext sinkContext) throws Exception {
        this.config = VeniceSinkConfig.load(config, sinkContext);
        VeniceSystemFactory factory = new VeniceSystemFactory();
        final String systemName = "venice";
        this.producer = factory
                .getClosableProducer(systemName, new MapConfig(getConfig(this.config.getStoreName(),
                        systemName)), null);
        this.producer.start();
    }

    @Override
    public void write(Record<GenericObject> record) throws Exception {
        Object nativeObject = record.getValue().getNativeObject();
        Object key = record.getKey();
        Object value = record.getValue();
        if (nativeObject instanceof KeyValue) {
            KeyValue keyValue = (KeyValue) nativeObject;
            key = keyValue.getKey();
            value = keyValue.getValue();
        }
        log.info("Writing key: {} value {}", key, value);
        producer.put(key, value).get();
    }

    @Override
    public void close() throws Exception {
        if (producer != null) {
            producer.close();
        }
    }

    private Map<String, String> getConfig(String storeName, String systemName) {
        Map<String, String> samzaConfig = new HashMap<>();
        String configPrefix = SYSTEMS_PREFIX + systemName + DOT;
        samzaConfig.put(configPrefix + VENICE_PUSH_TYPE, Version.PushType.INCREMENTAL.toString());
        samzaConfig.put(configPrefix + VENICE_STORE, storeName);
        samzaConfig.put(configPrefix + VENICE_AGGREGATE, "false");
        samzaConfig.put("venice.parent.d2.zk.hosts", "NOT-CONFIGURED");
        samzaConfig.put("venice.child.d2.zk.hosts", "NOT-CONFIGURED");

        samzaConfig.put("venice.discover.urls", this.config.getVeniceRouterUrl());

        samzaConfig.put(DEPLOYMENT_ID, Utils.getUniqueString("venice-push-id-pulsar-sink"));
        samzaConfig.put(SSL_ENABLED, "false");
        return samzaConfig;
    }
}
