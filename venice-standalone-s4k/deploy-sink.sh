set -x -e

HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
ARCHIVE=$(realpath $HERE/binaries/*sink*nar)
CONFIG=$(cat sink.config.json)
INPUT=public/default/input
$PULSAR_HOME/bin/pulsar-admin sinks create --tenant public --namespace default --name venice --archive $ARCHIVE --sink-config "$CONFIG" -i $INPUT
