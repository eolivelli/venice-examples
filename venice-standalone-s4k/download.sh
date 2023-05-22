set -x -e
HERE=$(realpath $(dirname $0))
PULSARURL=https://github.com/datastax/pulsar/releases/download/ls210_3.1/lunastreaming-2.10.3.1-bin.tar.gz
S4KURL=https://github.com/datastax/starlight-for-kafka/releases/download/v2.10.3.6/pulsar-protocol-handler-kafka-2.10.3.6.nar
VENICESINKURL=https://github.com/datastax/venice/releases/download/ds-0.4.17-alpha-9/pulsar-venice-sink.nar
VENICESTANDALONEURL=https://github.com/datastax/venice/releases/download/ds-0.4.17-alpha-9/venice-standalone-all.jar
VENICETOOLSURL=https://github.com/datastax/venice/releases/download/ds-0.4.17-alpha-9/venice-admin-tool-all.jar
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
curl -L -O $AVROTOOLSURL
popd





