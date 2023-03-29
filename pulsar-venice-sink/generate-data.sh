set -x

JAR=$(realpath ../pulsar-data-generator/target/*dependenc*.jar)

java -jar $JAR http://localhost:8080 t1/n1/input  
