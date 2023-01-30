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

Start Pulsar Standalone on localhost

```
PULSARHOME=WHERE-YOU-HAVE-PULSAR
$PULSARHOME/bin/pulsar standalone -nss
```

## Start the cluster using docker

```
git clone https://github.com/eolivelli/venice-examples
docker compose up
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


### Running Venice locally with DataStax Starlight for Kafka

You can also run Venice without docker.

- Start Pulsar Standalone locally, with the [Starlight-for-Kafka](https://github.com/datastax/starlight-for-kafka) plugin
- Run Venice using the scripts inside [running-venice-no-docker](running-venice-no-docker) directory
- Use the "prepare.sh" to get the binaries from your Venice code directory
- Run all the services one at a time
- After then you can run all of the other examples on your Venice cluster
