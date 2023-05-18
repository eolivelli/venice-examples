set -x -e
HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
$PULSAR_HOME/bin/pulsar-admin namespaces set-retention public/default -t -1 -s 100M
$PULSAR_HOME/bin/pulsar-admin topics create-partitioned-topic -p 1 persistent://public/default/venice_admin_venice-cluster0
$PULSAR_HOME/bin/pulsar-admin topics create-partitioned-topic -p 1 persistent://public/default/venice_system_store_meta_store_store1_rt
$PULSAR_HOME/bin/pulsar-admin topics create-partitioned-topic -p 1 persistent://public/default/venice_system_store_METADATA_SYSTEM_SCHEMA_STORE_rt
