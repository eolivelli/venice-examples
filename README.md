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


### Start Pulsar standalone

Start Pulsar Standalone on localhost with Starlight for Kafka

```
export PULSARHOME=WHERE-YOU-HAVE-PULSAR
git clone https://github.com/eolivelli/venice-examples
cp running-venice-no-docker/pulsar
./run_pulsar_no_auth.sh
```

## Start the cluster without using Docker

```
cd running-venice-no-docker
## copy the binaries from your Venice code directory
./prepare.sh

# open 3 terminals and run these commands
./run_controller.sh
./run_server.sh
./run_router.sh

```

## Init the store

Create the "store" and push some data (this will create the first version of the store):

```
cd pulsar-venice-sink
./create-store.sh

```

### Start the Pulsar Sink

```
./deploy_pulsar_standalone.sh
```

The script deploys the Sink on localhost

You can check the logs

```
tail -f  $PULSARHOME/logs/functions/public/default/venice/venice-0.log 
```

### Generate Data To Pulsar

Use the WriteKeyValue java file to generate data to the `people`
Follow the logs on the Sink

### Read the data from Venice

You can use MainReader and set these values

```
String storeName = "test-store-persons";
String[] keys = {"name0"};
```
