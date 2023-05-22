set -x -e

HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
$PULSAR_HOME/bin/pulsar-admin sinks delete --tenant public --namespace default --name venice
