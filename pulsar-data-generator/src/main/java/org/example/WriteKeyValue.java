package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.schema.KeyValue;

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

        String destTopic = "people";

        PulsarClient client = PulsarClient.builder()
                .serviceUrl("http://localhost:8080")
                .build();

        Schema<Person> personSchema = Schema.AVRO(Person.class);
        String schema = personSchema.getSchemaInfo().getSchemaDefinition();
        log.info("Schema: {}", schema);

        Producer<KeyValue<String, Person>> destTopicProducer = client
                .newProducer(Schema.KeyValue(Schema.STRING, personSchema))
                .topic(destTopic)
                .blockIfQueueFull(true)
                .create();

        for (int i = 0; i < 10; i++) {
            Person person = new Person("name" + i, 20 + i * 2 );
            destTopicProducer.send(new KeyValue<>(person.getName(), person));
        }

        destTopicProducer.flush();
        destTopicProducer.close();

        client.close();

    }
}
