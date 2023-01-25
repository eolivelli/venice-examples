package org.example;

import com.linkedin.venice.client.factory.CachingVeniceStoreClientFactory;
import com.linkedin.venice.client.factory.VeniceStoreClientFactory;
import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;

public class Main {
    public static void main(String[] args) {
        try {
            ClientConfig clientConfig = new ClientConfig();
            clientConfig.setVeniceURL("http://venice-router:7777");
            VeniceStoreClientFactory factory
                    = new CachingVeniceStoreClientFactory(clientConfig);
            AvroGenericStoreClient<Object, Object> store = factory.getAndStartAvroGenericStoreClient("venice-store");
        } catch (Throwable t) {
            t.printStackTrace();
        }

    }
}