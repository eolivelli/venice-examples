package org.example;

import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;

import java.util.concurrent.ExecutionException;

public class MainReader {

    static {
        System.setProperty("log4j.configurationFile", "src/main/resources/log4j2.xml");
    }
    public static void main(String[] args) {
        try {

            //String storeName = "test-store";
            //String[] keys = {"one"};

            // use this values for the Pulsar Sink demo
            String storeName = "test-store-persons";
            String[] keys = {"name0"};


            ClientConfig clientConfig = ClientConfig.defaultGenericClientConfig(storeName);
            clientConfig.setVeniceURL("http://venice-router:7777");
            clientConfig.setForceClusterDiscoveryAtStartTime(true);
            AvroGenericStoreClient<Object, Object> store = ClientFactory.getAndStartGenericAvroClient(clientConfig);
            System.out.println("Store: " + store);

            for (String key : keys) {
                try {
                    Object o = store.get(key).get();
                    System.out.println("Result Key " + key+" = " + o);
                } catch (ExecutionException err) {
                    System.out.println("Result Key " + key+" > " + err.getCause());
                }
            }

            store.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}