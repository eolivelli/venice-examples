set -x -e
HERE=$(realpath $(dirname $0))


cd $HERE
rm -Rf pulsar
mkdir pulsar
pushd pulsar
tar zxvf ../binaries/lunastreaming*.tar.gz --strip-components 1
cp ../standalone.conf conf/standalone.conf
mkdir protocols
cp ../binaries/*protocol* protocols/
popd

rm -Rf venice
mkdir venice
pushd venice
cp ../binaries/venice* .
cp -r ../config .
popd
