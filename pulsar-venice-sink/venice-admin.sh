#bin/bash

set -x

jar=$(realpath target/*venice-admin*.jar)
url=http://localhost:5555

# create the store
java -jar $jar --url $url "$@"
