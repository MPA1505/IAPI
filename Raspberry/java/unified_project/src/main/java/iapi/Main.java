package iapi;

import iapi.convert_data.HadoopConfig;
import iapi.merge_data.FileMerger;
import org.apache.hadoop.conf.Configuration;

public class Main {

    private static volatile boolean isRunning = true; // Flag to control the loop

    public static void main(String[] args) {
        // Default values
        final String DEFAULT_INPUT_FOLDER = "./src/main/resources/datasets_20hz_1_robot_1_minute";
        final String DEFAULT_OUTPUT_PARQUET_FILE = "./src/main/resources/merged_datasets/merged_dataset.parquet";
        final int DEFAULT_MAX_FILE_SIZE_MB = 300;

        // Parse command-line arguments
        String inputFolder = (args.length > 0) ? args[0] : DEFAULT_INPUT_FOLDER;
        String outputParquetFile = (args.length > 1) ? args[1] : DEFAULT_OUTPUT_PARQUET_FILE;
        int maxFileSizeMB = parseMaxFileSize(args, DEFAULT_MAX_FILE_SIZE_MB);

        System.out.printf("Starting the data merging and conversion process.%n");
        System.out.printf("Input folder: %s%n", inputFolder);
        System.out.printf("Output Parquet file: %s%n", outputParquetFile);
        System.out.printf("Max file size: %d MB%n", maxFileSizeMB);

        // Initialize Hadoop configuration
        Configuration conf = HadoopConfig.getHadoopConfiguration();

        try {
            // Initialize FileMerger
            FileMerger merger = new FileMerger(inputFolder, maxFileSizeMB * 1024L * 1024L, outputParquetFile, conf);

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
                System.out.println("Checking for new files...");
                merger.mergeFilesConcurrently(); // Process files
                System.out.println("Waiting for new files...");
                try {
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
}
