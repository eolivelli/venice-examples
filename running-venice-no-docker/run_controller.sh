#/bin/bash

CONFIGDIR=${1:-config-no-tls}
echo "Using configuration in $CONFIGDIR"
java -javaagent:./jmx_prometheus_javaagent-0.17.2.jar=9999:prometheus_config.yaml -Djava.net.preferIPv4Stack=true -jar bin/venice-controller-all.jar $CONFIGDIR/cluster.properties $CONFIGDIR/controller.properties

