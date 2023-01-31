#/bin/bash

set -x

jar=$(realpath ../pulsar-venice-sink/target/*venice-admin*.jar)
url=https://PUT-HERE-THE-VALID-SERVER-NAME:5556

SSLCONFIG="--ssl-config-path config-tls/client.tls.properties"

# create the store
java -Djavax.net.debug=all -jar $jar --url $url $SSLCONFIG "$@"
