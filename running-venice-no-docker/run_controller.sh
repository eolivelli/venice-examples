#/bin/bash

CONFIGDIR=${1:-config-no-tls}
echo "Using configuration in $CONFIGDIR"
java -Djavax.net.debug=all  -Djava.net.preferIPv4Stack=true -jar bin/venice-controller-all.jar $CONFIGDIR/cluster.properties $CONFIGDIR/controller.properties
