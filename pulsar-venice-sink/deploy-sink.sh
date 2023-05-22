set -x -e

ARCHIVE=$(realpath target/*nar)
CONFIG=$(cat *sink*config*json)
INPUT=public/default/input
$PULSAR_HOME/bin/pulsar-admin sinks create --tenant public --namespace default --name venice --archive $ARCHIVE --sink-config "$CONFIG" -i $INPUT
