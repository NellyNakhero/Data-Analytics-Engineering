# Kafka-based data hub

## Components
- Kafka (with ZooKeeper)
- Schema Registry
- Kafka Connect (with Aiven SMT support)
- MinIO (S3-compatible for Iceberg/Parquet)
- Trino (for querying Iceberg tables)
- dbt (for transformations)
- Apache Airflow (for orchestration)
- Volumes + Plugin

This example uses the Confluent S3 Sink Connector with Aiven’s HeaderToValue SMT
to inject all Kafka headers into the message value, then writes it as Parquet 
to MinIO (S3-compatible).

Assumptions:
- Kafka connect is up  and running on `localhost:8083`
- Minio on `http://minio:9000`
- Aiven SMT is mounted and working
- topic called `input-topic` with header data
- Data is serialized in JSON
- You have jq installed



## Run Script

    chmod +x download-aiven-smt.sh
    ./download-aiven-smt.sh


## STEPS

1. Run the plugin install script

- Run `./download-aiven-smt.sh` to fetch the Aiven SMT plugin
  - Verify plugin JARs are in `./connect-plugins/aiven-smts/`
        - ensure your directory looks like this
            connect-plugins/aiven-smts/
            ├── kotlin-compiler-1.9.10.jar
            ├── kotlin-reflect-1.9.10.jar
            ├── kotlin-stdlib-1.9.10.jar
            ├── kotlin-stdlib-common-1.9.10.jar
            ├── transforms-for-apache-kafka-connect-1.6.0.jar
        - if it doesnt look like that, do
            <code>
                # Move into the directory
                cd connect-plugins/aiven-smts/transforms-for-apache-kafka-connect-1.6.0
                # Move the jar to the parent directory
                mv transforms-for-apache-kafka-connect-1.6.0.jar ../
                # Go back up and clean up
                cd ..
                rm -rf transforms-for-apache-kafka-connect-1.6.0
            </code>
  
- Ensure `CONNECT_PLUGIN_PATH` includes `aiven-smts`
- Apply the connector JSON via POST to `http://localhost:8083/connectors`
- Monitor logs and inspect Parquet files on MinIO

2. start all containers

`docker-compose up -d`

check that kafka connect connectivity is working well

`curl http://localhost:8083/ | jq`

3. check kafka topics exists

<code>

docker exec -it $(docker ps -qf "name=kafka") bash #e.g  docker exec -it datahub-components-kafka-1 bash
kafka-topics --bootstrap-server kafka:9092 --create --topic test-topic --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server localhost:9092 --list

</code>

ensure kafka connect sees our plugin
`curl -s http://localhost:8083/connector-plugins | jq`


4. Send sample events with headers

<code>

docker exec -it kafka bash

kafka-console-producer \
--broker-list kafka:9092 \
--topic test-topic \
--property "parse.key=true" \
--property "key.separator=:" \
--property "headers=source=api,eventType=create"

# Then paste this
123:{"id": "abc", "value": 42}


</code>


or alternatively, you can use kcat

- send events without headers

`$ echo "Hello Kafka" | docker run --rm --network datahub-components_default -i edenhill/kcat:1.7.0 -P -b kafka:9092 -t input-topic`

- Send event with headers

`$ echo "Hello Kafka with headers" | docker run --rm --network datahub-components_default -i edenhill/kcat:1.7.0 -P -b kafka:9092 -t input-topic -H "headerKey1=headerValue1" -H "headerKey2=headerValue2"`

-  consume the messages to confirm headers are present

`docker run --rm --network datahub-components_default edenhill/kcat:1.7.0 -C -b kafka:9092 -t input-topic -f 'Headers: %h\nMessage: %s\n'`

press `cntrl + c` to exit

Your output should be something like this

`
Headers: 
% Reached end of topic input-topic [0] at offset 2
% Reached end of topic input-topic [0] at offset 3
% Reached end of topic input-topic [0] at offset 4
Message: Hello Kafka
Headers: headerKey1=headerValue1,headerKey2=headerValue2
Message: Hello Kafka with headers
Headers: headerKey1=headerValue1,headerKey2=headerValue2
Message: Hello Kafka with headers
Headers:
Message: Hello Kafka
`



5. Post the Kafka Connect Sink config:

<code>

curl -X POST -H "Content-Type: application/json" \
--data @minio-s3-sink-with-headers.json \
http://localhost:8083/connectors

</code>

6. Check the data in MinIO

Visit: http://localhost:9001

Login: minioadmin / minioadmin

Look inside bucket data-lake

Check if Parquet files are there under topics/test-topic/...

7. Query parquet files with trino

Inside trino

`docker exec -it trino trino`

- Query the topics

<code> 
SHOW SCHEMAS FROM iceberg;
SHOW TABLES FROM iceberg."default";

SELECT * FROM iceberg."default"."test-topic" LIMIT 10;

</code>


## DEBUG

#### check connector status

`curl http://localhost:8083/connectors/s3-sink-with-headers/status
`


#### trino logs

`docker logs trino`

#### Kafka connect logs

`docker logs kafka-connect`


## Success Criteria

- Kafka messages with headers are written into MinIO as Parquet
-  Header fields appear inside the JSON/Parquet data
- You can query the result using Trino