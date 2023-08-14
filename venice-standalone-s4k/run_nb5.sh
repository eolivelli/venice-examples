set -x -e
HERE=$(realpath $(dirname $0))
java -jar $HERE/binaries/nb5.jar run driver=pulsar  workload=pulsar_venice.yaml config=pulsar_client.conf service_url=pulsar://localhost:6650 web_url=http://localhost:8080 tags=block:msg-produce-block cycles=100 -v
java -jar $HERE/binaries/nb5.jar run driver=venice  workload=venice_reader.yaml store_name=store1 router_url=http://localhost:7777 cycles=100 -v
