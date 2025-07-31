1. Start Kafka cluster:

docker-compose up -d

2. Create topics:

docker exec -it kafka kafka-topics --create --topic input.topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
docker exec -it kafka kafka-topics --create --topic output.topic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

3. Start Spring Boot app:

mvn spring-boot:run

4. Send a message:

curl -X POST http://localhost:8080/kafka/send -d "hello"

5. Run Flink job:

mvn exec:java -Dexec.mainClass="com.example.flink.FlinkKafkaProcessor"

mvn exec:java -Dexec.mainClass="com.example.flinkkafka.flink.FlinkKafkaProcessor"

mvn exec:java -Dexec.mainClass="com.data.engineering.data_engineering.flink.FlinkKafkaProcessor"



6. Read output:

kafka-console-consumer --bootstrap-server localhost:9092 --topic output.topic --from-beginning
