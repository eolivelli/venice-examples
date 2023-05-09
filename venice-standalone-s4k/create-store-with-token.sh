#bin/bash

set -x

jar=$(realpath binaries/*venice-admin*.jar)
url=http://localhost:5555
clusterName=venice-cluster0
storeName=store4
keySchema=key.avsc
valueSchema=value.avsc
token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.DBjI5MJuVyCa6oncrP5eEP329Pmixk6SX4UG-HS0P7g

# create the store
java -jar $jar --new-store --url $url --cluster $clusterName  --store $storeName --key-schema-file $keySchema --value-schema-file $valueSchema --token $token

# enable incremental push
java -jar $jar --update-store --url $url --cluster $clusterName  --store $storeName --storage-quota -1 --incremental-push-enabled true --token $token

# disable read quota
java -jar $jar --update-store --url $url --cluster $clusterName  --store $storeName --read-quota 1000000 --token $token

# create the first version of the store
java -jar $jar --empty-push --url $url --cluster $clusterName --store $storeName --push-id init --store-size 1000 --token $token
