## GETTING STARTED

## STARTUP THE INFRA

`
    cd docker/infra
    docker-compose up
`

## MODULE: API-SERVER

#### To run Spring Boot API

    cd api-server
    mvn clean spring-boot:run

#### Test the Kafka Producer Endpoint

    curl -X POST http://localhost:8084/kafka/send \
     -H "Content-Type: text/plain" \
     -d "Hello from Spring Boot Kafka!"

#### Check Kafka topics exist:
    docker exec -it <kafka-container-name> kafka-topics --bootstrap-server localhost:9092 --list

in this case it will be

    docker exec -it  infra-kafka-1 kafka-topics --bootstrap-server localhost:9092 --list

#### Consume from output.topic to verify Flink processed the message:

    docker exec -it <kafka-container-name> kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic output.topic \
        --from-beginning

in this case it will be

    docker exec -it infra-kafka-1 kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic output.topic \
        --from-beginning


## MODULE: FLINK JOB

#### To run the Flink job, package the JAR using Maven

    cd flink-job
    mvn clean package

You should get:

    target/flink-job-0.0.1-SNAPSHOT-jar-with-dependencies.jar

#### Submit the Flink Job

    docker exec -it <flink-jobmanager-container-id> bash
    flink run /opt/flink/jars/flink-job-0.0.1-SNAPSHOT-jar-with-dependencies.jar

## NB
- Confirm Kafka is running: `docker ps` should show the kafka container
- Use Kafka CLI tools inside the Kafka container to check topic existence:

      docker exec -it <kafka-container> kafka-topics --bootstrap-server kafka:9092 --list
- to stop the docker containers `docker-compose down`