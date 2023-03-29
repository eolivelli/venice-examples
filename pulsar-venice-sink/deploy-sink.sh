set -x -e

ARCHIVE=$(realpath target/*nar)
CONFIG=$(cat *sink*config*json)
INPUT=t1/n1/input
$PULSAR_HOME/bin/pulsar-admin sinks create --tenant t1 --namespace n1 --name venice --archive $ARCHIVE --sink-config "$CONFIG" -i $INPUT
