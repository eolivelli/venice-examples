set -x -e
HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
$PULSAR_HOME/bin/pulsar-admin topics create-partitioned-topic -p 1 persistent://public/default/venice_admin_venice-cluster0
