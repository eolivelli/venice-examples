package org.example.sink;

import com.linkedin.venice.meta.Version;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.writer.VeniceWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.pulsar.client.api.schema.GenericObject;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.functions.api.Record;
import org.apache.pulsar.io.core.Sink;
import org.apache.pulsar.io.core.SinkContext;
import org.apache.samza.config.MapConfig;
import org.checkerframework.checker.units.qual.A;
import org.example.simplewriter.VeniceSystemFactory;
import org.example.simplewriter.VeniceSystemProducer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    AtomicInteger count = new AtomicInteger();

    @Override
    public void open(Map<String, Object> config, SinkContext sinkContext) throws Exception {
        this.config = VeniceSinkConfig.load(config, sinkContext);
        VeniceSystemFactory factory = new VeniceSystemFactory();
        final String systemName = "venice";
        this.producer = factory
                .getClosableProducer(systemName, new MapConfig(getConfig(this.config.getStoreName(),
                        systemName)), null);
        this.producer.start();
        String kafkaBootstrapServers = this.producer.getKafkaBootstrapServers();
        log.info("Connected to Kafka {}", kafkaBootstrapServers);

        VeniceWriter<byte[], byte[], byte[]> veniceWriter = this.producer.getInternalProducer();
        String topicName = veniceWriter.getTopicName();
        log.info("Kafka topic name is {}", topicName);
    }

    @Override
    public void write(Record<GenericObject> record) throws Exception {
        Object nativeObject = record.getValue().getNativeObject();
        Object key;
        Object value;
        if (nativeObject instanceof KeyValue) {
            KeyValue keyValue = (KeyValue) nativeObject;
            key = extract(keyValue.getKey());
            value = extract(keyValue.getValue());
        } else {
            // this is a string
            key = record.getKey();
            value = extract(record.getValue());
        }
        //dumpSchema("key", key);
        //dumpSchema("value", value);
        if (value == null) {
            // here we are making it explicit, but "put(key, null) means DELETE in the API"
            //log.info("Deleting key: {}", key);
            producer.delete(key).whenComplete((___, error) -> {
                if (error != null) {
                    record.fail();
                } else {
                    record.ack();
                }
            });
        } else {
            //log.info("Writing key: {} value {}", key, value);
            producer.put(key, value).whenComplete((___, error) -> {
                if (count.incrementAndGet() % 1000 == 0) {
                    log.info("written {} records", count);
                }
                if (error != null) {
                    log.error("error", error);
                    record.fail();
                } else {
                    record.ack();
                }
            });
        }
    }

    private static void dumpSchema(String prefix, Object key) {
        if (key instanceof GenericRecord) {
            GenericRecord record = (GenericRecord) key;
            Schema schema = record.getSchema();
            log.info("Schema for {}: {}", prefix, schema.toString());
        }
    }

    private static Object extract(Object o) {
        if (o instanceof GenericRecord) {
            return (GenericRecord) o;
        }
        // Pulsar GenericRecord is a wrapper over AVRO GenericRecord
        if (o instanceof org.apache.pulsar.client.api.schema.GenericRecord) {
            return ((org.apache.pulsar.client.api.schema.GenericRecord) o).getNativeObject();
        }

        return o;
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
        //samzaConfig.put("venice.parent.d2.zk.hosts", config.getVeniceZookeeper());
        //samzaConfig.put("venice.child.d2.zk.hosts", config.getVeniceZookeeper());

        samzaConfig.put("venice.discover.urls", this.config.getVeniceRouterUrl());

        samzaConfig.put(DEPLOYMENT_ID, Utils.getUniqueString("venice-push-id-pulsar-sink"));
        samzaConfig.put(SSL_ENABLED, "false");
        return samzaConfig;
    }
}
