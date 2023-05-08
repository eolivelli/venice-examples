set -x -e
HERE=$(realpath $(dirname $0))
java -jar -Djava.net.preferIPv4Stack=true $HERE/venice/venice-standalone-all.jar $HERE/venice/config
