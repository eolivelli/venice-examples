# Running Venice Standalone with Pulsar

This directory contains a bunch of scripts to easily start Venice Standalone and Pulsar Standalone
in order to have a fully working environment to test Venice.


With this set of scripts you will setup:
- a Pulsar Standalone with Starlight for Kafka
- a Venice Standalone cluster with 1 controller, 1 server and 1 router
- a Pulsar Sink to Venice

## Download the binaries and prepare

```
./download.sh
./install.sh
```

### Start Pulsar standalone

```
./run_pulsar.sh
```

## Start the Venice Cluster

```
# open a new terminal
./setup.sh
./run_venice.sh
```

## Create the Store and the Pulsar Sink

```
# open a new terminal
./create-store.sh
./deploy-sink.sh
```

You can see the logs of the Sink heres

```
tail -f -n20000 pulsar/logs/functions/public/default/venice/venice-0.log 
```

### Generate some data to Pulsar

The scripts write a KeyValue message to Pulsar, with key=foo1
```
./generate-data.sh
```


### Read the data from Venice using the HTTP API

The script reads from the Venice Router using the HTTP API and fetches the record with key=foo1 
```
./query_using_http.sh
```
