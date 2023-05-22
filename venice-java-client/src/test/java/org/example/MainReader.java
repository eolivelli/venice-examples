package org.example;

import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;
import com.linkedin.venice.utils.EncodingUtils;

import java.nio.charset.StandardCharsets;
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
            String storeName = "public.default.store1";
            String[] keys = {"foo1"};

            String encoded = EncodingUtils.base64EncodeToString(keys[0].getBytes(StandardCharsets.UTF_8));
            System.out.println("Encoded: " + encoded);

            ClientConfig clientConfig = ClientConfig.defaultGenericClientConfig(storeName);
            clientConfig.setVeniceURL("http://localhost:7777");
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