#/bin/bash
CONFIGDIR=${1:-config-no-tls}
echo "Using configuration in $CONFIGDIR"
rm -Rf rocksdb
java -javaagent:./jmx_prometheus_javaagent-0.17.2.jar=9998:prometheus_config.yaml  -Djava.net.preferIPv4Stack=true -jar bin/venice-server-all.jar $CONFIGDIR
