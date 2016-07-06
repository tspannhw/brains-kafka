Brains Kafka
============
Kafka Streams demo application that performs real-time processing of brain EEG data from an [Emotiv](http://emotiv.com) headset -- converting them into [OpenTSDB](http://opentsdb.org) time series records visualized with [Grafana](http://grafana.org) .

Detailed instructions found at Silicon Valley Data Science blog:

### 1. Build & Run the Demo ###
    ./build_and_run_demo.sh

### 2. Go to http://${DOCKER_MACHINE_IP}:3000/dashboard/db/eeg and wait a few minutes for data to start appearing... ###

### 3. (Optional) In a new terminal, you may Inspect EEG topic for debugging purposes ###
    eval $(docker-machine env)
    docker exec docker_kafka-connect-opentsdb_1 /usr/bin/kafka-avro-console-consumer --topic eeg --bootstrap-server kafka:9092 --property value.schema='{"type":"record","name":"Point","namespace":"com.svds.kafka.connect.opentsdb","fields":[{"name":"metric","type":"string"},{"name":"timestamp","type":{"type":"long","logicalType":"timestamp-millis"}},{"name":"value","type":"double"},{"name":"tags","type":{"type":"map","values":"string"}}]}' --property schema.registry.url=http://schema-registry:8081 --zookeeper zookeeper:2181
