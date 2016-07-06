# Builds a docker image for this EEG Kafka Streams app.
#
# Usage:
#   docker build -t jeffsvds/eeg-kafka-streams .
#   docker push jeffsvds/eeg-kafka-streams

FROM confluent/platform

COPY *.jar /usr/local/lib/

ENTRYPOINT ["java", "-jar", "/usr/local/lib/eeg-kafka-streams-assembly-0.0.1.jar", "--bootstrap-servers", "kafka:9092", "--zookeeper", "zookeeper:2181", "--schema-registry", "http://schema-registry:8081", "--source", "sensors", "--sink", "eeg"]
