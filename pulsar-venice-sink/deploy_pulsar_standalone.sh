#/bin/bash


INPUTTOPIC=people
NARPATH=$(realpath target/*.nar)
SINKCONFIG="$(cat sink.config.json)"

set -x

pulsar-admin sinks delete --name venice
pulsar-admin sinks create --name venice -a $NARPATH --sink-config "$SINKCONFIG" -i $INPUTTOPIC
