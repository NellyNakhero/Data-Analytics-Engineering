FROM confluentinc/cp-kafka-connect:7.5.0

# Install Confluent S3 Sink Connector
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-s3:10.5.0

# Create plugin directory for Aiven SMT
RUN mkdir -p /usr/share/java/aiven-smts

# Download Aiven SMT JAR
RUN curl -L -o /usr/share/java/aiven-smts/kafka-connect-transform-header-to-field-0.2.0.jar \
  https://github.com/aiven/kafka-connect-transform-header-to-field/releases/download/v0.2.0/kafka-connect-transform-header-to-field-0.2.0.jar
