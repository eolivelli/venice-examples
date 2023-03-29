set -x -e

ARCHIVE=$(realpath target/*nar)
CONFIG=$(cat *sink*config*json)
INPUT=t1/n1/input
$PULSAR_HOME/bin/pulsar-admin sinks delete --tenant t1 --namespace n1 --name venice 
