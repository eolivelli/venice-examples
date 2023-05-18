set -x -e
HERE=$(realpath $(dirname $0))
java -jar -Djava.net.preferIPv4Stack=true $HERE/venice/venice-thin-client-all.jar store1 ${1} http://localhost:7777 false ""
