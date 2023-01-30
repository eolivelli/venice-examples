#bin/bash

set -x

jar=$(realpath target/*venice-admin*.jar)
url=http://venice-controller:5555

# create the store
java -jar $jar --url $url "$@"
