
---

##  Kafka → MinIO (S3) Sink Connector

This project demonstrates a Kafka Connect pipeline that writes data from a Kafka topic into a MinIO (S3-compatible) bucket using the Confluent S3 Sink Connector.

---

###  How to Run

1. **Clone this repository**

   ```bash
   git clone https://github.com/NellyNakhero/Data-Analytics-Engineering.git
   cd kacomia
   ```

2. **Start the stack**

   ```bash
   docker-compose up -d --build
   ```

3. **Generate fernet keys**

    ```bash
   python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
    ```
   
    and replace it on the docker compose for every key `AIRFLOW__CORE__FERNET_KEY`

4. **Access UIs**

    * **Kafka Connect REST API**: `http://localhost:8083`
    * **MinIO Web UI**: `http://localhost:9001`

        * Username: `minioadmin`
        * Password: `minioadmin`

5. **Ensure bucket exists**

   login to minio and create bucket called warehouse

6. **Create kafka topic**

    ```bash
   docker exec -it kacomia-kafka-1 kafka-topics --create \
    --topic events \
    --partitions 1 \
    --replication-factor 1 \
    --if-not-exists \
    --bootstrap-server kafka:9092
    ```


---

###  Connector Configuration

Once the services are up, create the connector:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connector-protobuf-config.json
```

---

###  Test the Setup

1. **Produce test messages to Kafka**

   ```bash
   docker run --rm -it --network kacomia_default \
      confluentinc/cp-schema-registry:7.6.0 \
      kafka-protobuf-console-producer \
      --broker-list kacomia-kafka-1:9092 \
      --topic events \
      --property schema.registry.url=http://schema-registry:8081 \
      --property value.schema='syntax = "proto3"; package myproto; message Event { int32 user_id = 1; string event = 2; string device = 3; }'
      
      
   ```

   Paste a few test messages:

   ```json
      {"user_id": 1, "event": "signup", "device": "mobile"}
      {"user_id": 2, "event": "login", "device": "desktop"}
      {"user_id": 3, "event": "purchase", "device": "tablet"}
   ```

2. **Verify connector status**

   ```bash
   curl http://localhost:8083/connectors/minio-s3-sink/status | jq
   ```

   Look for `state: RUNNING` for both the connector and the task.

3. **Check MinIO bucket**

    * Open your browser: [http://localhost:9001](http://localhost:9001)
    * Login with:

        * Username: `minioadmin`
        * Password: `minioadmin`
    * Navigate to:

      ```
      warehouse/
        └── topics/
            └── events/
                └── partition=0/
                    └── events+0+0000000000.snappy.parquet
      ```
    * Click on a file to preview or download it.

---

###  Tear Down

```bash
docker-compose down -v
```

---

###  Notes

* Make sure the bucket (`warehouse`) exists in MinIO before creating the connector.
* The flush size (`flush.size`) controls how many records are batched before writing to S3.

---

