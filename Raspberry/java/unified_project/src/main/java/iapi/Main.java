package iapi;

import iapi.convert_data.HadoopConfig;
import iapi.merge_data.FileMerger;
import iapi.send_data.KafkaFileProducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static volatile boolean isRunning = true; // Flag to control the loop

    public static void main(String[] args) {
        // Default values
        final String DEFAULT_INPUT_FOLDER = "./src/main/resources/datasets_20hz_1_robot_1_minute";
        final String DEFAULT_OUTPUT_FOLDER = "./src/main/resources/merged_datasets";
        final int DEFAULT_MAX_FILE_SIZE_MB = 300;
        final String DEFAULT_BOOTSTRAP_SERVER = "pkc-lq8v7.eu-central-1.aws.confluent.cloud:9092";
        final String DEFAULT_TOPIC = "kafka-test";

        // Parse command-line arguments
        Map<String, String> argMap = parseArguments(args);

        String inputFolder = argMap.getOrDefault("input", DEFAULT_INPUT_FOLDER);
        String outputFolder = argMap.getOrDefault("output", DEFAULT_OUTPUT_FOLDER);
        String bootstrapServer = argMap.getOrDefault("server", DEFAULT_BOOTSTRAP_SERVER);
        String topic = argMap.getOrDefault("topic", DEFAULT_TOPIC);
        int maxFileSizeMB = parseMaxFileSize(argMap.getOrDefault("size", String.valueOf(DEFAULT_MAX_FILE_SIZE_MB)));

        // Print configuration
        System.out.println("Starting the data merging and conversion process.");
        System.out.printf("Input folder: %s%n", inputFolder);
        System.out.printf("Output folder: %s%n", outputFolder);
        System.out.printf("Max file size: %d MB%n", maxFileSizeMB);
        System.out.printf("Kafka Bootstrap Server: %s%n", bootstrapServer);
        System.out.printf("Kafka Topic: %s%n", topic);

        // Initialize Hadoop configuration
        Configuration conf = HadoopConfig.getHadoopConfiguration();

        try {
            // Initialize FileMerger
            FileMerger merger = new FileMerger(inputFolder, maxFileSizeMB * 1024L * 1024L, outputFolder, conf);

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown signal received. Stopping application...");
                isRunning = false; // Stop the loop
                try {
                    merger.stop(); // Gracefully stop the FileMerger
                    System.out.println("Application stopped gracefully.");
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                    e.printStackTrace();
                }
            }));

            // Continuously monitor and process files
            while (isRunning) {
                merger.mergeFilesConcurrently(); // Process files
                System.out.println("Waiting for new files...");
                try {
                    sendFiles(outputFolder, bootstrapServer, topic);
                    Thread.sleep(5000); // Pause before checking again
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted. Exiting...");
                    isRunning = false;
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to initialize or execute: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses command-line arguments into a key-value map.
     * Example input: --inputFolder=/path/to/input --outputFolder=/path/to/output --maxFileSizeMB=200
     */
    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] split = arg.substring(2).split("=", 2);
                if (split.length == 2) {
                    argMap.put(split[0], split[1]);
                }
            }
        }
        return argMap;
    }

    /**
     * Parses the max file size from a string.
     */
    private static int parseMaxFileSize(String value) {
        try {
            int maxFileSizeMB = Integer.parseInt(value);
            if (maxFileSizeMB <= 0) {
                throw new IllegalArgumentException("Max file size must be greater than 0.");
            }
            return maxFileSizeMB;
        } catch (NumberFormatException e) {
            System.err.printf("Invalid max file size specified: %s. Using default value.%n", value);
            return 300; // Default max file size
        }
    }

    public static void sendFiles(String outputPath, String bootstrapServers, String topic) {
        // Initialize Kafka producer
        KafkaFileProducer kafkaProducer = new KafkaFileProducer(bootstrapServers, topic);

        // Process files in a folder and send to Kafka
        File outputFolder = new File(outputPath);

        if (!outputFolder.exists() || !outputFolder.isDirectory()) {
            System.err.printf("Output path '%s' does not exist or is not a directory.%n", outputPath);
            return;
        }

        System.out.printf("Scanning folder: %s for .parquet files...%n", outputPath);
        File[] files = outputFolder.listFiles((dir, name) -> name.endsWith(".parquet"));

        if (files == null || files.length == 0) {
            System.out.println("No .parquet files found to process.");
            return;
        }

        System.out.printf("Found %d .parquet file(s) to send.%n", files.length);

        for (File file : files) {
            System.out.printf("Preparing to send file: %s (size: %d bytes)%n", file.getName(), file.length());
            try {
                kafkaProducer.sendFile(file.toPath()); // Send each file to Kafka
                System.out.printf("Successfully sent file: %s%n", file.getName());
            } catch (Exception e) {
                System.err.printf("Error sending file %s: %s%n", file.getName(), e.getMessage());
                e.printStackTrace();
            }
        }

        // Close Kafka producer
        kafkaProducer.close();
        System.out.println("Kafka producer closed. All files processed.");
    }

}
