#bin/bash

set -x

jar=$(realpath binaries/*venice-admin*.jar)
url=http://localhost:5555
clusterName=venice-cluster0

# create the store
java -jar $jar --list-stores --url $url --cluster $clusterName 
