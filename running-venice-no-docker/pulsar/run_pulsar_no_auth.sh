set -x
export PULSAR_STANDALONE_CONF=$(realpath pulsar/standalone.conf)
$PULSAR_HOME/bin/pulsar standalone -nfw -nss 
