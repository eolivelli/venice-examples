package org.example;

import com.linkedin.venice.meta.Version;
import com.linkedin.venice.samza.VeniceSystemFactory;
import com.linkedin.venice.samza.VeniceSystemProducer;
import com.linkedin.venice.utils.Utils;
import org.apache.samza.config.MapConfig;

import java.util.HashMap;
import java.util.Map;

import static com.linkedin.venice.CommonConfigKeys.SSL_ENABLED;
import static com.linkedin.venice.samza.VeniceSystemFactory.DEPLOYMENT_ID;
import static com.linkedin.venice.samza.VeniceSystemFactory.DOT;
import static com.linkedin.venice.samza.VeniceSystemFactory.SYSTEMS_PREFIX;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_AGGREGATE;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_CHILD_CONTROLLER_D2_SERVICE;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_CHILD_D2_ZK_HOSTS;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_PARENT_CONTROLLER_D2_SERVICE;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_PARENT_D2_ZK_HOSTS;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_PUSH_TYPE;
import static com.linkedin.venice.samza.VeniceSystemFactory.VENICE_STORE;


public class MainWriter {
    static {
        System.setProperty("log4j.configurationFile", "src/main/resources/log4j2.xml");
    }

    public static void main(String[] args) {
        try {
            String storeName = "venice-store";
            String systemName = "venice";

            VeniceSystemFactory factory = new VeniceSystemFactory();
            VeniceSystemProducer producer = factory
                    .getClosableProducer(systemName, new MapConfig(getSamzaConfig(storeName, systemName)), null);

            producer.start();

            producer.put("17", "sdfsf").get();

            producer.stop();
            producer.close();

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }

    public static final String D2_SERVICE_NAME = "ChildController";

    public static final String PARENT_D2_SERVICE_NAME = "ParentController";

    private static Map<String, String> getSamzaConfig(String storeName, String systemName) {
        Map<String, String> samzaConfig = new HashMap<>();
        String configPrefix = SYSTEMS_PREFIX + systemName + DOT;
        samzaConfig.put(configPrefix + VENICE_PUSH_TYPE, Version.PushType.STREAM.toString());
        samzaConfig.put(configPrefix + VENICE_STORE, storeName);
        samzaConfig.put(configPrefix + VENICE_AGGREGATE, "false");
        samzaConfig.put(VENICE_CHILD_D2_ZK_HOSTS, "localhost:2181");
        samzaConfig.put(VENICE_CHILD_CONTROLLER_D2_SERVICE, D2_SERVICE_NAME);
        samzaConfig.put("venice.discover.urls", "http://venice-router:7777");

        samzaConfig.put(VENICE_PARENT_D2_ZK_HOSTS, "localhost:2181"); // parentController.getKafkaZkAddress());
        samzaConfig.put(VENICE_PARENT_CONTROLLER_D2_SERVICE, PARENT_D2_SERVICE_NAME);
        samzaConfig.put(DEPLOYMENT_ID, Utils.getUniqueString("venice-push-id"));
        samzaConfig.put(SSL_ENABLED, "false");
        return samzaConfig;
    }
}