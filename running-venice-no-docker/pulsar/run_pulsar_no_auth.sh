set -x
HERE=$(dirname $0)
PULSAR_STANDALONE_CONF=$(realpath $HERE/standalone.conf) $PULSAR_HOME/bin/pulsar standalone -nss  --wipe-data
