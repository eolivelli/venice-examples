set -x -e
HERE=$(realpath $(dirname $0))
store=store4
echo "\"foo2\"" > $HERE/tmp/key
curl -v http://localhost:7777/storage/$store/$(java -jar $HERE/binaries/*avro-tools*.jar jsontofrag --schema-file key.avsc $HERE/tmp/key | base64)\?f=b64
