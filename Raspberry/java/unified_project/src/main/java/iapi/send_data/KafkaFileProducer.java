package iapi.send_data;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Properties;

public class KafkaFileProducer {

    private final Producer<String, String> producer;
    private final String topic;

    public KafkaFileProducer(String bootstrapServers, String topic) {
        this.topic = topic;

        System.out.printf("Initializing Kafka producer for topic: %s on server: %s%n", topic, bootstrapServers);

        // Test connection to Kafka broker
        if (!testBrokerConnection(bootstrapServers)) {
            throw new KafkaException("Failed to connect to Kafka broker at: " + bootstrapServers);
        }

        // Kafka producer properties
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 200000000);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 200000000);

        this.producer = new KafkaProducer<>(props);
        System.out.println("Kafka producer initialized successfully.");
    }

    /**
     * Tests the connection to the Kafka broker by attempting to fetch the cluster ID.
     *
     * @param bootstrapServers Kafka bootstrap servers.
     * @return true if the connection test is successful, false otherwise.
     */
    private boolean testBrokerConnection(String bootstrapServers) {
        System.out.println("Testing connection to Kafka broker...");
        Properties adminProps = new Properties();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            String clusterId = adminClient.describeCluster().clusterId().get();
            System.out.printf("Successfully connected to Kafka broker. Cluster ID: %s%n", clusterId);
            return true;
        } catch (Exception e) {
            System.err.printf("Failed to connect to Kafka broker at '%s'. Error: %s%n", bootstrapServers, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends the content of a file to the Kafka topic.
     *
     * @param filePath Path of the file to send.
     * @return true if the file was sent successfully, false otherwise.
     */
    public boolean sendFile(Path filePath) {
        File file = filePath.toFile();

        if (file.length() == 0) {
            System.err.printf("Skipping empty file: %s%n", file.getName());
            return false;
        }

        try {
            // Read binary file content
            byte[] content = Files.readAllBytes(filePath);

            // Encode binary content to Base64
            String encodedContent = Base64.getEncoder().encodeToString(content);

            // Create a producer record
            System.out.printf("Creating Kafka record for file: %s%n", file.getName());
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, file.getName(), encodedContent);

            // Send the record to Kafka
            System.out.printf("Sending file: %s to Kafka topic: %s%n", file.getName(), topic);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.printf("Error sending file '%s' to Kafka: %s%n", file.getName(), exception.getMessage());
                }
            }).get(); // Ensures blocking until the send operation completes
            return true;
        } catch (IOException e) {
            System.err.printf("Error reading file '%s': %s%n", file.getName(), e.getMessage());
        } catch (Exception e) {
            System.err.printf("Unexpected error while sending file '%s': %s%n", file.getName(), e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Closes the Kafka producer.
     */
    public void close() {
        System.out.println("Closing Kafka producer...");
        producer.close();
        System.out.println("Kafka producer closed.");
    }
}
