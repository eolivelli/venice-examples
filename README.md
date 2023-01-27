# LinkedIn Venice Playground

This repository contains a sample project to access LinkedIn Venice using a Java program.

Key points about the writer side:
- it uses a twaked version of the VeniceSystemProducer that is the Apache Samza writer
- it uses the INCREMENTAL push mode

Key points about the reader:
- it uses the standard venice-thin-client API, that is based on HTTP

This repository also contains a very simple [Pulsar IO Sink](pulsar-venice-sink) that allows Pulsar users to ingest data to Venice.



## Build Venice from source

```
git clone https://github.com/linkedin/venice
cd venice
./gradlew publishToMavenLocal -Pversion=1.1-SNAPSHOT
cd docker
./build-venice-docker-images.sh
```

## Start the cluster

```
git clone https://github.com/eolivelli/venice-examples
docker compose up
```

## Setting up the cluster

Create the "store" and push some data (this will create the first version of the store):

```
docker exec -it venice-client bash

./create-store.sh http://venice-controller:5555 venice-cluster0 test-store sample-data/schema/keySchema.avsc sample-data/schema/valueSchema.avsc 

./run-vpj.sh sample-data/single-dc-configs/batch-push-job.properties

```


## Run the examples

- MainWriter: writes a key to the test-store
- MainReader: read the same key to the test-store


## Setting up the Pulsar Sink with an AVRO payload

### Init the store

First of all you have to create an empty store, the key is a STRING and the value is a record of type "Person"
```
docker exec -it venice-client bash

STORENAME=test-store-persons
CONTROLLERURL=http://venice-controller:5555
CLUSTER=venice-cluster0

echo '{"name": "key","type": "string"}' > key.avsc
echo '{"type":"record","name":"Person","namespace":"org.example.WriteKeyValue","fields":[{"name":"age","type":"int"},{"name":"name","type":["null","string"],"default":null}]}' > persons.avsc

./create-store.sh $CONTROLLERURL $CLUSTER $STORENAME key.avsc persons.avsc
java -jar /opt/venice/bin/venice-admin-tool-all.jar --empty-push --url $CONTROLLERURL --cluster $CLUSTER --store test-store-persons  --push-id init --store-size 1000
```

### Start Pulsar standalone

Start Pulsar Standalone on localhost

```
bin/pulsar standalone -nss
```

### Start the Pulsar Sink

```
cd pulsar-venice-sink
./deploy_pulsar_standalone.sh
```

The script deploys the Sink on localhost

You can check the logs

```
tail -f  logs/functions/public/default/venice/venice-0.log 
```

### Generate Data To Pulsar

Use the WriteKeyValue java file to generate data to the `people`
Follow the logs on the Sink

### Read the data from Venice

You can use MainReader and set

```
String storeName = "test-store-persons";
String[] keys = {"name0"};
```



