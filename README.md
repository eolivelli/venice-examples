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