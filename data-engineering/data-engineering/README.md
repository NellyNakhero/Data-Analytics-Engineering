#  GETTING STARTED GUIDE

##  Prerequisites

- Docker & Docker Compose installed
- Java 17+ and Maven installed
- Ports `8084`, `9092`, etc., not in use

---

##  STEP 1: Start Infrastructure

Start Kafka, Zookeeper, Flink, and MinIO using Docker Compose:

```bash
cd docker/infra
docker-compose up -d
```

###  Check containers are running:
```bash
docker ps
```

You should see containers like:
- `infra-kafka-1`
- `infra-zookeeper-1`
- `infra-jobmanager-1`
- `infra-taskmanager-1`
- `infra-minio-1`

---

##  Docker Services Overview

###  Kafka + Zookeeper

| Service    | Port(s) | Purpose |
|------------|---------|---------|
| `zookeeper` | - | Kafka coordination |
| `kafka`     | `9092` (internal)<br>`29092` (external/host) | Kafka broker with dual listeners |

>  Spring Boot apps connect using `localhost:29092`  
>  Flink jobs (inside Docker) connect using `kafka:9092`

###  MinIO (S3-compatible Storage)

| Port(s)     | Purpose                   |
|-------------|---------------------------|
| `9000`      | S3 API endpoint           |
| `9001`      | Admin Console             |

Credentials:
- Access Key: `minioadmin`
- Secret Key: `minioadmin`

> 🔧 Used by Flink for checkpointing/savepoints if needed

###  Flink

| Service           | Port(s) | Description |
|-------------------|---------|-------------|
| `flink-jobmanager` | `8081` | Flink dashboard UI |
| `flink-taskmanager` | - | Executes tasks from jobmanager |
| `flink-sql-client` | - | Interactive SQL shell for Flink |

---

##  STEP 2: Run the Spring Boot API Server

### Run the API:

```bash
cd api-server
mvn clean spring-boot:run
```

By default, it runs on port `8084`.

### Test Kafka Producer Endpoint:

```bash
curl -X POST http://localhost:8084/kafka/send \
  -H "Content-Type: text/plain" \
  -d "Hello from Spring Boot Kafka!"
```

---

##  STEP 3: Verify Kafka Topics

### List Kafka Topics:

```bash
docker exec -it infra-kafka-1 kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

You should see:

```
input.topic
output.topic
```

If not, ensure that your Spring Boot or Flink job has produced/consumed from them.

---

##  STEP 4: Build and Run the Flink Job

### Build the Flink Job:

```bash
cd flink-job
mvn clean package
```

Expected output:

```
target/flink-job-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

### Submit the Flink Job:

```bash
docker exec -it infra-jobmanager-1 bash
flink run /opt/flink/jars/flink-job-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

>  Make sure the JAR is correctly mounted via the volume in `docker-compose.yml`

### Optional: Monitor Flink UI
Visit [http://localhost:8081](http://localhost:8081)

---

##  STEP 5: Consume from `output.topic`

To verify that the message was processed by Flink:

```bash
docker exec -it infra-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic output.topic \
  --from-beginning
```

Expected output:

```
Java xoxo processed: Hello from Spring Boot Kafka!
```

---

##  Notes & Tips

- Confirm `input.topic` and `output.topic` exist **before** running the Flink job.
- If messages are not flowing:
  - Check Kafka container is running (`docker ps`)
  - Ensure API server sends messages to `localhost:29092`
  - Verify Flink is consuming from `kafka:9092`

### Stop All Services:

```bash
cd docker/infra
docker-compose down
```

---

## PROJECT STRUCTURE
data-engineering/                   # Root project
│
├── api-server/                    # Spring Boot application (entry point, controllers)
│   └── src/main/java/...
│
├── core/                          # Shared logic (Kafka config, POJOs, utilities)
│   └── src/main/java/...
│
├── flink-job/                     # Apache Flink job module
│   └── src/main/java/...
│
└── pom.xml         # Parent POM settings

### flink

data-engineering/
├── flink-job/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/
│       │   │       └── data/
│       │   │           └── engineering/
│       │   │               └── flinkjob/
│       │   │                   └── FlinkKafkaProcessor.java
│       │   └── resources/
│       │       └── application.properties (optional)
│       └── test/
│           └── java/...


## RESPONSIBILITY BREAKDOWN

| Module        | Responsibilities                                                               |
| ------------- | ------------------------------------------------------------------------------ |
| `core/`       | - Common Kafka configs<br>- Data model POJOs<br>- Utils shared across modules  |
| `api-server/` | - REST endpoints (controller)<br>- Spring Boot app<br>-  publish to Kafka |
| `flink-job/`  | - Flink stream processing logic<br>- Kafka consumer via FlinkKafkaConsumer     |
