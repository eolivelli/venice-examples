set -x -e
HERE=$(realpath $(dirname $0))
store=store4
token=eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.DBjI5MJuVyCa6oncrP5eEP329Pmixk6SX4UG-HS0P7g
echo "\"foo2\"" > $HERE/tmp/key
curl -H "Authorization: Bearer $token" -v http://localhost:7777/storage/$store/$(java -jar $HERE/binaries/*avro-tools*.jar jsontofrag --schema-file key.avsc $HERE/tmp/key | base64)\?f=b64
