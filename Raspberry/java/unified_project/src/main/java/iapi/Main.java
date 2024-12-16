package iapi;

import iapi.convert_data.HadoopConfig;
import iapi.merge_data.FileMerger;
import iapi.send_data.KafkaFileProducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.File;

public class Main {

    private static volatile boolean isRunning = true; // Flag to control the loop

    public static void main(String[] args) {
        // Default values
        final String DEFAULT_INPUT_FOLDER = "./src/main/resources/datasets_20hz_1_robot_1_minute";
        final String DEFAULT_OUTPUT_FOLDER = "./src/main/resources/merged_datasets";
        final int DEFAULT_MAX_FILE_SIZE_MB = 300;

        // Parse command-line arguments
        String inputFolder = (args.length > 0) ? args[0] : DEFAULT_INPUT_FOLDER;
        String outputFolder = (args.length > 1) ? args[1] : DEFAULT_OUTPUT_FOLDER;
        int maxFileSizeMB = parseMaxFileSize(args, DEFAULT_MAX_FILE_SIZE_MB);

        System.out.printf("Starting the data merging and conversion process.%n");
        System.out.printf("Input folder: %s%n", inputFolder);
        System.out.printf("Output folder: %s%n", outputFolder);
        System.out.printf("Max file size: %d MB%n", maxFileSizeMB);

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
                //merger.mergeFilesConcurrently(); // Process files
                System.out.println("Waiting for new files...");
                try {
                    sendFiles(outputFolder);
                    Thread.sleep(5000); // Pause before checking again
                } catch (InterruptedException e) {
                    System.out.println("Thread interrupted. Exiting...");
                    isRunning = false;
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to initialize or execute FileMerger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Parses the max file size from command-line arguments.
     */
    private static int parseMaxFileSize(String[] args, int defaultMaxFileSize) {
        if (args.length > 2) {
            try {
                int maxFileSizeMB = Integer.parseInt(args[2]);
                if (maxFileSizeMB <= 0) {
                    throw new IllegalArgumentException("Max file size must be greater than 0.");
                }
                return maxFileSizeMB;
            } catch (NumberFormatException e) {
                System.err.printf("Invalid max file size specified: %s. Using default max file size: %d MB%n", args[2], defaultMaxFileSize);
            } catch (IllegalArgumentException e) {
                System.err.printf("%s Using default max file size: %d MB%n", e.getMessage(), defaultMaxFileSize);
            }
        }
        return defaultMaxFileSize;
    }

    public static void sendFiles(String outputPath) {
        // Kafka configuration
        String bootstrapServers = "pkc-lq8v7.eu-central-1.aws.confluent.cloud:9092"; // Replace with actual Kafka server
        String topic = "kafka-test";

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
