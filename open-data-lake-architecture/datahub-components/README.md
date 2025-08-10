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



## Run Script

    chmod +x download-aiven-smt.sh
    ./download-aiven-smt.sh


## STEPS

1. Run the plugin install script

- Run `./download-aiven-smt.sh` to fetch the Aiven SMT plugin
- Verify plugin JARs are in `./connect-plugins/aiven-smts/`
- Ensure `CONNECT_PLUGIN_PATH` includes `aiven-smts`
- Apply the connector JSON via POST to `http://localhost:8083/connectors`
- Monitor logs and inspect Parquet files on MinIO

2. start all containers

`docker-compose up -d`

3. Post the Kafka Connect Sink config:

<code>

curl -X POST -H "Content-Type: application/json" \
--data @minio-s3-sink-with-headers.json \
http://localhost:8083/connectors

</code>