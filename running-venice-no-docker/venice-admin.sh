#/bin/bash

set -x

jar=$(realpath ../pulsar-venice-sink/target/*venice-admin*.jar)
url=http://venice-controller:5555

# create the store
java -jar $jar --url $url $SSLCONFIG "$@"
