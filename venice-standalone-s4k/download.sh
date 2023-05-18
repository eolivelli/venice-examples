set -x -e
HERE=$(realpath $(dirname $0))
PULSARURL=https://github.com/datastax/pulsar/releases/download/ls210_4.5/lunastreaming-2.10.4.5-bin.tar.gz
S4KURL=https://github.com/datastax/starlight-for-kafka/releases/download/v2.10.3.9/pulsar-protocol-handler-kafka-2.10.3.9.nar
VENICESINKURL=https://github.com/datastax/venice/releases/download/ds-0.4.17-alpha-10/pulsar-venice-sink.nar
VENICESTANDALONEURL=https://github.com/datastax/venice/releases/download/0.4.17-alpha-10/venice-standalone-all.jar
VENICETOOLSURL=https://github.com/datastax/venice/releases/download/0.4.17-alpha-10/venice-admin-tool-all.jar
VENICECLIENTURL=https://github.com/datastax/venice/releases/download/0.4.17-alpha-10/venice-thin-client-all.jar
AVROTOOLSURL=https://dlcdn.apache.org/avro/avro-1.11.1/java/avro-tools-1.11.1.jar


BINDIR=$HERE/binaries
rm -Rf $BINDIR
mkdir $BINDIR
pushd $BINDIR
cd $BINDIR
curl -L -O $PULSARURL
curl -L -O $S4KURL
curl -L -O $VENICESINKURL
curl -L -O $VENICESTANDALONEURL
curl -L -O $VENICETOOLSURL
curl -L -O $VENICECLIENTURL
curl -L -O $AVROTOOLSURL
popd





