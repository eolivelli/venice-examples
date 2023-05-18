set -x -e
HERE=$(realpath $(dirname $0))
PULSAR_HOME=$HERE/pulsar
PULSARTOPIC=public/default/input
SCHEMA=$(cat value.avsc)
JSONFILE=$HERE/tmp/person.json
AVROFILE=$HERE/tmp/person.avro
MESSAGEKEY=${1}
echo '{"name":"foo1-name","age":15}' > $JSONFILE
cat $JSONFILE &&
java -jar $HERE/binaries/*avro-tools*.jar jsontofrag "$SCHEMA" $JSONFILE > $AVROFILE
$PULSAR_HOME/bin/pulsar-client produce --key-value-encoding-type separated -k $MESSAGEKEY --key-schema string --value-schema "avro:$SCHEMA" -f $AVROFILE $PULSARTOPIC

