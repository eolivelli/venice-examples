#bin/bash

set -x

jar=$(realpath binaries/*venice-admin*.jar)
url=http://localhost:5555
clusterName=venice-cluster0
storeName=store1


key=foo1

java -jar $jar  --url $url --cluster $clusterName  --store $storeName --query --key $key

