package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.impl.AuthenticationUtil;
import org.apache.pulsar.client.impl.auth.AuthenticationDisabled;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.common.util.FutureUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class WriteKeyValue
{

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
    }

    public static void main( String[] args ) throws Exception {

        String url = args.length > 0 ? args[0] : "pulsar://localhost:6650";
        String destTopic = args.length > 1 ? args[1] : "public/default/input";
        System.out.println("URL "+url+" destTopic "+destTopic);
        String token = args.length > 2 ? args[2] : null;

        PulsarClient client = PulsarClient.builder()
                .serviceUrl(url)
                .authentication(token != null ? AuthenticationFactory.token(token) : new AuthenticationDisabled())
                .allowTlsInsecureConnection(true)
                .build();

        Schema<Person> personSchema = Schema.AVRO(Person.class);
        String schema = personSchema.getSchemaInfo().getSchemaDefinition();
        log.info("Schema: {}", schema);
        System.out.println("Schema: "+schema);

        Producer<KeyValue<String, Person>> destTopicProducer = client
                .newProducer(Schema.KeyValue(Schema.STRING, personSchema))
                .topic(destTopic)
                .blockIfQueueFull(true)
                .create();

        List<CompletableFuture<?>> handles = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            //System.out.println("Sending " + i);
            Person person = new Person("name" + i, 20 + i * 2 );
            handles.add(destTopicProducer.sendAsync(new KeyValue<>(person.getName(), person)));
            //System.out.println("Sent " + i);
            if (handles.size() == 10000) {
                FutureUtil.waitForAll(handles).join();
                handles.clear();
                System.out.println("sent "+i);
            }
        }
        FutureUtil.waitForAll(handles).join();

        System.out.println("Flush");
        destTopicProducer.flush();
        destTopicProducer.close();

        client.close();

    }
}
