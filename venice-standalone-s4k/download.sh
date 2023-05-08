set -x -e
HERE=$(realpath $(dirname $0))
PULSARURL=https://github.com/datastax/pulsar/releases/download/ls210_3.1/lunastreaming-2.10.3.1-bin.tar.gz
S4KURL=https://github.com/datastax/starlight-for-kafka/releases/download/v2.10.3.6/pulsar-protocol-handler-kafka-2.10.3.6.nar
VENICESINKURL=https://github.com/eolivelli/venice-examples/releases/download/v1/pulsar-venice-sink-1.0-SNAPSHOT.nar
VENICESTANDALONEURL=https://github.com/datastax/venice/releases/download/0.4.17-alpha-0/venice-standalone-all.jar

BINDIR=$HERE/binaries
rm -Rf $BINDIR
mkdir $BINDIR
pushd $BINDIR
cd $BINDIR
curl -L -O $PULSARURL
curl -L -O $S4KURL
curl -L -O $VENICESINKURL
curl -L -O $VENICESTANDALONEURL
popd





