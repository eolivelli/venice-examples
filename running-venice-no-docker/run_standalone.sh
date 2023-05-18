DIR=$(realpath $1)
java -Djava.net.preferIPv4Stack=true -jar $VENICE_HOME/services/venice-standalone/build/libs/venice-standalone-all.jar $DIR
