#bin/bash

set -x

jar=$(realpath binaries/*venice-admin*.jar)
url=http://localhost:5555
clusterName=venice-cluster0
storeName=store4
keySchema=key.avsc
valueSchema=value.avsc
token=

# create the store
java -jar $jar --new-store --url $url --cluster $clusterName  --store $storeName --key-schema-file $keySchema --value-schema-file $valueSchema

# enable incremental push
java -jar $jar --update-store --url $url --cluster $clusterName  --store $storeName --storage-quota -1 --incremental-push-enabled true

# disable read quota
java -jar $jar --update-store --url $url --cluster $clusterName  --store $storeName --read-quota 1000000

# create the first version of the store
java -jar $jar --empty-push --url $url --cluster $clusterName --store $storeName --push-id init --store-size 1000
