
import iapi.send_data.KafkaFileProducer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KafkaFileProducerTest {

    private static KafkaFileProducer producer;
    private static Consumer<String, String> consumer;
    private static final String TOPIC = "test-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    @BeforeAll
    public static void setup() {
        // Initialize Kafka producer
        producer = new KafkaFileProducer(BOOTSTRAP_SERVERS, TOPIC);

        // Configure Kafka consumer for testing
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterAll
    public static void teardown() {
        producer.close();
        consumer.close();
    }

    @Test
    public void testSendFile() throws Exception {
        // Create a temporary file with some content
        Path tempFile = File.createTempFile("test-file", ".txt").toPath();
        Files.writeString(tempFile, "This is a test message!");

        // Send the file to Kafka
        producer.sendFile(tempFile);

        // Consume the message from Kafka
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
        assertEquals(1, records.count()); // Ensure one message was consumed

        for (ConsumerRecord<String, String> record : records) {
            assertEquals(tempFile.getFileName().toString(), record.key()); // Check the file name as the key
            assertEquals("This is a test message!", record.value()); // Check the content as the value
        }

        // Delete the temporary file
        Files.delete(tempFile);
    }
}
