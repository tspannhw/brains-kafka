#!/bin/bash -e

project_dir=`pwd`

jar_filepath="target/scala-2.11/eeg-kafka-streams-assembly-0.0.1.jar"
if [ ! -f ${jar_filepath} ]; then

echo "Building JAR file..."
./test_prep.sh
# skip tests when building uber JAR file
sbt 'set test in assembly := {}' clean assembly
cp ${jar_filepath} docker/

fi

cd docker
eval $(docker-machine env)
export DOCKER_MACHINE_IP=`docker-machine ip`
docker-compose up -d

echo "Creating Topics (wait 45 seconds)..."
sleep 45
docker exec docker_kafka_1 /usr/bin/kafka-topics --list --zookeeper zookeeper:2181

echo "Waiting for Schema Registry to start..."
docker-compose up -d # ensure schema-registry starts

echo "Waiting for OpenTSDB to initialize its tables (wait 45 seconds)..."
sleep 45
read -p "In your web browser, go to http://${DOCKER_MACHINE_IP}:3000/datasources/edit/1 and, if necessary, update the URL to match your Docker machine IP address and click the 'Save' button. Press [enter] once you've updated the URL... "

echo "Reading EEG CSV data into 'sensors' topic. It may take a few minutes to see data at http://${DOCKER_MACHINE_IP}:3000/dashboard/db/eeg ..."
while getopts ":e" opt; do
    case $opt in
        e)
            input_device="emotiv"
            ;;
    esac
done

if [ "$input_device" == "emotiv" ]; then
    echo "Reading data from Emotiv headset..."
    tail -f ~/Library/Developer/Xcode/DerivedData/EEGLogger-*/Build/Products/Debug/EEGDataLogger.csv | docker exec -i docker_kafka_1 /usr/bin/kafka-console-producer --broker-list kafka:9092 --topic sensors
else
    while true; do
        cat ${project_dir}/*.csv | docker exec -i docker_kafka_1 /usr/bin/kafka-console-producer --broker-list kafka:9092 --topic sensors
    done
fi
