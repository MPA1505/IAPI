package iapi;

import iapi.convert_data.HadoopConfig;
import iapi.merge_data.FileMerger;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.nio.file.*;

public class Main {

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
                System.out.println("Shutdown detected. Cleaning up resources...");
                merger.shutdown();
                System.exit(0);
            }));

            // Monitor directory for new files
            //monitorDirectory(merger, inputFolder);
            // Continuously monitor and process files
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("New files detected");
                merger.mergeFilesConcurrently();
                System.out.println("Waiting for new files...");
                Thread.sleep(5000); // Pause before checking again
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

    /**
     * Monitors the input folder for new files and triggers the merge process.
     */
    private static void monitorDirectory(FileMerger merger, String inputFolder) throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get(inputFolder);
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        System.out.println("Monitoring directory for new files: " + inputFolder);

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    System.out.println("New file detected: " + event.context());
                    merger.mergeFilesConcurrently();
                }
            }
            if (!key.reset()) {
                break;
            }
        }
        watchService.close();
    }
}
