set -x -e
HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
$PULSAR_HOME/bin/pulsar standalone -nss --wipe-data --zookeeper-port 2222

