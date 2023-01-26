package org.example;

import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;

public class MainReader {

    static {
        System.setProperty("log4j.configurationFile", "src/main/resources/log4j2.xml");
    }
    public static void main(String[] args) {
        try {
            String storeName = "test-store";
            ClientConfig clientConfig = ClientConfig.defaultGenericClientConfig(storeName);
            clientConfig.setVeniceURL("http://venice-router:7777");
            clientConfig.setForceClusterDiscoveryAtStartTime(true);
            AvroGenericStoreClient<Object, Object> store = ClientFactory.getAndStartGenericAvroClient(clientConfig);
            System.out.println("Store: "+store);
            Object o = store.get("foo").get();
            System.out.println("Result "+o);

            store.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}