## OVERVIEW

Protobuf Msgs
↓
Kafka (events)
↓
Kafka Connect (JDBC Sink Connector with ProtobufConverter)
↓
PostgreSQL (data sink for analytics with auto-evolving schema)
↓
Superset (BI tool on top of Postgres)

Sure! Here’s the full guide formatted as a `README.md`:

---

# 📊 Kafka → PostgreSQL → Superset Analytics Pipeline

This guide sets up a simple, production-ready pipeline for streaming events from **Apache Kafka** into **PostgreSQL**, then analyzing them with **Apache Superset** using **Kafka Connect with JDBC Sink Connector** — all via **Docker Compose**.

---

## 📦 What’s Included

* **Kafka** — for ingesting/streaming events
* **Kafka Connect (JDBC Sink)** — for moving data from Kafka to PostgreSQL
* **PostgreSQL** — as the analytics data store
* **Apache Superset** — for dashboards and SQL-based analytics

---

## 🧰 Tech Stack

```text
Kafka → Kafka Connect (JDBC Sink) → PostgreSQL → Superset
```

---

## 🚀 Quick Start

### 1. Clone or Create Project Directory

```bash
cd QuickAnalytics
```


---

---

### 2. Start the Services

```bash
docker-compose up -d
```

---

### 3. Check the connector plugins were added successfully

```bash
curl http://localhost:8083/connector-plugins | jq
```

you should see something similar to

```json
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   502  100   502    0     0   3833      0 --:--:-- --:--:-- --:--:--  3861
[
  {
    "class": "io.confluent.connect.jdbc.JdbcSinkConnector",
    "type": "sink",
    "version": "10.7.0"                                                                                                                          
  },
  {
    "class": "io.confluent.connect.jdbc.JdbcSourceConnector",
    "type": "source",
    "version": "10.7.0"
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorCheckpointConnector",
    "type": "source",
    "version": "7.6.0-ccs"                                                                                                                       
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorHeartbeatConnector",
    "type": "source",
    "version": "7.6.0-ccs"                                                                                                                       
  },
  {
    "class": "org.apache.kafka.connect.mirror.MirrorSourceConnector",
    "type": "source",
    "version": "7.6.0-ccs"                                                                                                                       
  }
]
```


---


### 4. Register JDBC Sink Connector

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
    "name": "postgres-sink-connector",
    "config": {
      "connector.class": "io.confluent.connect.jdbc.JdbcSinkConnector",
      "tasks.max": "1",
      "topics": "events_topic",
      "connection.url": "jdbc:postgresql://postgres:5432/analytics",
      "connection.user": "analytics",
      "connection.password": "analytics",
      "auto.create": "true",
      "auto.evolve": "true",
      "insert.mode": "insert",
      "pk.mode": "none",
      "table.name.format": "events"
    }
  }'

```

---

### 5. Check connector status

```bash
curl http://localhost:8083/connectors/postgres-sink-connector/status | jq

# OR
curl http://localhost:8083/connectors/postgres-sink-connector/status
```

you should see

```json
{
  "name": "postgres-sink-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "kafka-connect:8083"                                                                                                            
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "kafka-connect:8083"                                                                                                          
    }
  ],
  "type": "sink"                                                                                                                                 
}
```

The state should be `RUNNING`

---


### 6. Send test data to kafka

```bash
docker run --rm -it --network quickanalytics_default \
  confluentinc/cp-schema-registry:7.6.0 \
  kafka-protobuf-console-producer \
    --broker-list kafka:9092 \
    --topic events_topic \
    --property schema.registry.url=http://schema-registry:8081 \
    --property value.schema='syntax = "proto3"; package myproto; message Event { int32 user_id = 1; string event = 2; string device = 3; }'

#then type
{"user_id": 103, "event": "signup", "device": "mobile"}
{"user_id": 101, "event": "signup"}
{"user_id": 102, "event": "login"}

```

Now check if if the event was streamed to postgres table

```bash
$ docker exec -it quickanalytics-postgres-1 psql -U analytics -d analytics
psql (15.13 (Debian 15.13-1.pgdg130+1))
Type "help" for help.

analytics=# \dt
          List of relations
 Schema |  Name  | Type  |   Owner
--------+--------+-------+-----------
 public | events | table | analytics
(1 row)

analytics=# 
```


---

##  7. Superset Setup

1. Visit [http://localhost:8088](http://localhost:8088)
2. Login:

    * **Username**: `admin`
    * **Password**: `admin`
3. Go to **Settings → Database Connections**
4. Add connection string:

```bash
Host: postgres

Port: 5432

Database: analytics

Username: analytics

Password: analytics
```

```text
postgresql://analytics:analytics@postgres:5432/analytics
```

5. Explore your `events_topic` table and build charts!

---



---

## ✅ Summary

| Component     | Purpose                       |
| ------------- | ----------------------------- |
| Kafka         | Stream event ingestion        |
| Kafka Connect | Move data to Postgres         |
| PostgreSQL    | Durable storage for analytics |
| Superset      | Visualize and query data      |

---

## 📌 Tips

* Add indexing/partitioning in Postgres for larger datasets.
* Superset supports dashboards, alerts, and row-level access control.

---

