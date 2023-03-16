package org.example;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.UniformReservoir;
import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;

import java.util.Random;
import java.util.concurrent.ExecutionException;

public class SimpleReadsBenchmark {

    static {
        System.setProperty("log4j.configurationFile", "src/main/resources/log4j2.xml");
    }
    public static void main(String[] args) {
        try {

            //String storeName = "test-store";
            //String[] keys = {"one"};

            // use this values for the Pulsar Sink demo
            String storeName = "test-store-persons";


            ClientConfig clientConfig = ClientConfig.defaultGenericClientConfig(storeName);
            clientConfig.setVeniceURL("http://venice-router:7777");
            clientConfig.setForceClusterDiscoveryAtStartTime(true);
            AvroGenericStoreClient<Object, Object> store = ClientFactory.getAndStartGenericAvroClient(clientConfig);
            System.out.println("Store: " + store);

            Reservoir reservoir = new UniformReservoir();
            Histogram latency = new Histogram(reservoir);

            int numReads = 5000;
            Random r = new Random();
            for (int i = 0; i < numReads; i++) {
                long now = System.nanoTime();
                int n = r.nextInt(100);
                String key = "name"+n;
                try {
                    Object o = store.get(key).get();
                //    System.out.println("Result Key " + key+" = " + o);
                } catch (ExecutionException err) {
                    System.out.println("Result Key " + key+" > " + err.getCause());
                }
                latency.update(System.nanoTime() - now);
                System.out.println("Read "+i+" recodrds");
            }

            System.out.println("STATS:");
            Snapshot snapshot = latency.getSnapshot();
            System.out.println(snapshot.getMedian());
            System.out.println(snapshot.get75thPercentile());
            System.out.println(snapshot.get99thPercentile());
            System.out.println(snapshot.getMax());

            store.close();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }

    }
}