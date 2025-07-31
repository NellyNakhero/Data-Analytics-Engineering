###  run the docker compose

`
cd docker
docker-compose up
`

### create kafka topics

open shell

`docker exec -it infra-kafka-1 bash`
g
Inside the container, create your input.topic and output.topic:

<code>

# Create input.topic
kafka-topics --create \
--topic input.topic \
--bootstrap-server localhost:9092 \
--partitions 1 \
--replication-factor 1

# Create output.topic
kafka-topics --create \
--topic output.topic \
--bootstrap-server localhost:9092 \
--partitions 1 \
--replication-factor 1

</code>

confirm  the topics exist

`kafka-topics --list --bootstrap-server localhost:9092`

You should see

`input.topic
output.topic
`

### Build the app

`mvn clean compile assembly:single`

## Confirm the new fat JAR is created

` ls target/data-engineering-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

## Run the app

`java -jar target/data-engineering-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

You should see this in the logs

`Seeking to earliest offset of partition input.topic-0
Resetting offset for partition input.topic-0 to position FetchPosition{offset=0, ...}
`

## To run the API
`curl -X POST http://localhost:8084/kafka/send -d "hello from java 21"`

Alternatively, run the kafka producer

`docker exec -it infra-kafka-1 kafka-console-producer --broker-list localhost:9092 --topic input.topic`

Additionally, when you run

` docker exec -it infra-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic output.topic \
  --from-beginning`

You should see the messages you published