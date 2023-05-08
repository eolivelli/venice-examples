# Running Venice Standalone with Pulsar

This directory contains a bunch of scripts to easily start Venice Standalone and Pulsar Standalone
in order to have a fully working environment to test Venice.


With this set of scripts you will setup:
- a Pulsar Standalone with Starlight for Kafka
- a Venice Standalone cluster with 1 controller, 1 server and 1 router
- a Pulsar Sink to Venice

## 

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
