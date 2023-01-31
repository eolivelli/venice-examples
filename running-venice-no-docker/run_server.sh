#/bin/bash
CONFIGDIR=${1:-config-no-tls}
echo "Using configuration in $CONFIGDIR"
java  -Djava.net.preferIPv4Stack=true -jar bin/venice-server-all.jar $CONFIGDIR
