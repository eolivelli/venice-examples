#/bin/bash

echo "Using VENICE_HOME=$VENICE_HOME"
if [ -z "$VENICE_HOME" ];
then
   echo "Please set VENICE_HOME to a venice repository"
   exit 
fi

set -e -x

pushd $VENICE_HOME
./gradlew shadowJar -x :services:venice-standalone:shadowJar
popd

rm -Rf bin
mkdir bin

cp $VENICE_HOME/clients/venice-push-job/build/libs/venice-push-job-all.jar bin/
cp $VENICE_HOME/clients/venice-thin-client/build/libs/venice-thin-client-all.jar bin/
cp $VENICE_HOME/clients/venice-admin-tool/build/libs/venice-admin-tool-all.jar bin/
cp $VENICE_HOME/services/venice-server/build/libs/venice-server-all.jar bin/
cp $VENICE_HOME/services/venice-controller/build/libs/venice-controller-all.jar bin/
cp $VENICE_HOME/services/venice-router/build/libs/venice-router-all.jar bin/
