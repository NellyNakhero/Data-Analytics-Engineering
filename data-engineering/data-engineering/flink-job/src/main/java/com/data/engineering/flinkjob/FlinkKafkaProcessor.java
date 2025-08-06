package com.data.engineering.flinkjob;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

public class FlinkKafkaProcessor {

    public static void main(String[] args) throws Exception {
        // 1. Set up environments
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        // 2. Kafka Source
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:9092")
                .setTopics("input.topic")
                .setGroupId("flink-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> input = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 3. Transform into Row: Map incoming data into Row with schema
        DataStream<Row> rowStream = input
                .map(value -> {
                    Row row = Row.withNames();  // named row
                    row.setField("message", value);  // set field by name
                    return row;
                })
                .returns(Types.ROW_NAMED(new String[]{"message"}, Types.STRING));

        // 4. Define Table schema
        Schema schema = Schema.newBuilder()
                .column("message", DataTypes.STRING())
                .build();

        // 5. Create a temporary view over the stream
        tableEnv.createTemporaryView("messages_view", rowStream, schema);

        // 6. Register Iceberg catalog (Hadoop + S3)
        tableEnv.executeSql(
                "CREATE CATALOG iceberg_catalog WITH (" +
                        "  'type' = 'iceberg'," +
                        "  'catalog-type' = 'hadoop'," +
                        "  'warehouse' = 's3://warehouse/iceberg/'," +
                        "  'io-impl' = 'org.apache.iceberg.aws.s3.S3FileIO'," +
                        "  's3.endpoint' = 'http://minio:9000'," +
                        "  's3.path-style-access' = 'true'," +
                        "  's3.access-key' = 'minioadmin'," +
                        "  's3.secret-key' = 'minioadmin'" +
                        ")"
        );

        // 7. Use Iceberg catalog and create table
        tableEnv.executeSql("USE CATALOG iceberg_catalog");

        tableEnv.executeSql(
                "CREATE TABLE IF NOT EXISTS default.messages (" +
                        "  message STRING" +
                        ") " +
                        "PARTITIONED BY () " +
                        "STORED AS PARQUET"
        );

        // 8. Insert into the Iceberg table
        tableEnv.executeSql("INSERT INTO default.messages SELECT * FROM messages_view");

        // No need for env.execute(); Flink SQL handles the execution
    }
}
