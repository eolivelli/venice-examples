import pulsar
import pulsar.schema
from pulsar.schema import AvroSchema
import avro.schema
from avro.io import BinaryEncoder
from avro.io import DatumWriter
import random
import string
import io

# Define the Avro schema for the value
class Person:
    def __init__(self, age, name):
        self.age = age
        self.name = name

    @staticmethod
    def schema():
        #return avro.schema.parse(
        return '''
            {
                "type": "record",
                "name": "Person",
                "namespace": "org.example.Person",
                "fields": [
                    {"name": "age", "type": "int"},
                    {"name": "name", "type": "string"}
                ]
            }
        '''

    @staticmethod
    def random_name():
        name = ''.join(random.choice(string.ascii_lowercase) for i in range(10))
        return name

    def to_avro(self):
        schema = self.avro_schema()
        bytes_writer = io.BytesIO()
        encoder = BinaryEncoder(bytes_writer)
        writer = DatumWriter(schema)
        writer.write({"age": self.age, "name": self.name}, encoder)
        raw_bytes = bytes_writer.getvalue()
        return raw_bytes

schema = AvroSchema(avro.schema.parse(Person.schema()))

from pulsar import Client, Producer

# Create a Pulsar client
client = Client('pulsar://localhost:6650')

# Create a producer
producer = client.create_producer(
    topic='public/default/input',
    schema=schema,
    properties={
        'key.schema': '{"type": "string"}'
    }
)

# Produce 10 messages with random name and age
for i in range(100):
    name = Person.random_name()
    age = random.randint(18, 65)
    person = Person(age, name)
    avro_bytes = person.to_avro()
    key = "key-{}".format(i)
    print(key, avro_bytes)
    producer.send(key=key, value=avro_bytes)

# Close the producer and client
producer.close()
client.close()
