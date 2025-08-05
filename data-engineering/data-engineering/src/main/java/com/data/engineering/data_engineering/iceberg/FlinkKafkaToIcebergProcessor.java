package com.data.engineering.data_engineering.iceberg;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.types.Row;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.flink.CatalogLoader;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.iceberg.types.Types;
import org.apache.flink.table.api.DataTypes;

import java.util.HashMap;
import java.util.Map;

public class FlinkKafkaToIcebergProcessor {

    public static void main(String[] args) throws Exception {
        // Initialize Flink environment
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka source setup
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setTopics("input.topic")
                .setGroupId("flink-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> stringStream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source"
        );

        stringStream.print(); // See what is coming from Kafka


        // Convert to Flink Row (single column "data")
        DataStream<Row> rowStream = stringStream
                .map(value -> Row.of(value))
                .returns(Row.class);

        // Define Iceberg schema
        Schema icebergSchema = new Schema(
                Types.NestedField.required(1, "data", Types.StringType.get())
        );

        // Set up Iceberg catalog properties for MinIO/S3A
        Map<String, String> catalogProps = new HashMap<>();
        catalogProps.put("type", "hadoop");
        catalogProps.put("warehouse", "s3a://warehouse/iceberg");
        catalogProps.put("fs.s3a.access.key", "minioadmin");
        catalogProps.put("fs.s3a.secret.key", "minioadmin");
        catalogProps.put("s3.endpoint", "http://minio:9000");
        catalogProps.put("fs.s3a.path.style.access", "true");
        catalogProps.put("fs.s3a.connection.ssl.enabled", "false");
        catalogProps.put("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        catalogProps.put("io-impl", "org.apache.iceberg.aws.s3.S3FileIO");

        // Hadoop Configuration
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        for (Map.Entry<String, String> entry : catalogProps.entrySet()) {
            hadoopConf.set(entry.getKey(), entry.getValue());
        }

        // Load catalog
        CatalogLoader catalogLoader = CatalogLoader.hadoop("minio_catalog", hadoopConf, catalogProps);
        Catalog catalog = catalogLoader.loadCatalog();

        // Define table identifier
        TableIdentifier tableIdentifier = TableIdentifier.of("default", "kafka_iceberg_table");

        // Create the table if it doesn't exist
        if (!catalog.tableExists(tableIdentifier)) {
            catalog.createTable(tableIdentifier, icebergSchema);
        }

        // Table loader
        TableLoader tableLoader = TableLoader.fromCatalog(catalogLoader, tableIdentifier);

        // Write stream to Iceberg sink
        FlinkSink.forRow(rowStream,
                        org.apache.flink.table.api.TableSchema.builder()
                                .field("data", DataTypes.STRING())
                                .build())
                .tableLoader(tableLoader)
                .overwrite(false)
                .append();

        // Execute the pipeline
        env.execute("Kafka to Iceberg Flink Processor");
    }
}
