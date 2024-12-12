package iapi;

import iapi.convert_data.HadoopConfig;
import iapi.merge_data.FileMerger;
import org.apache.hadoop.conf.Configuration;

public class Main {
    public static void main(String[] args) {
        // Default values
        String defaultInputFolder = ".\\src\\main\\resources\\datasets_20hz_1_robot_1_minute";
        String defaultOutputParquetFile = ".\\src\\main\\resources\\merged_datasets\\merged_dataset.parquet";
        int defaultMaxFileSizeMB = 300;

        // Parse arguments
        String inputFolder = args.length > 0 ? args[0] : defaultInputFolder;
        String outputParquetFile = args.length > 1 ? args[1] : defaultOutputParquetFile;
        int maxFileSizeMB;

        try {
            // Parse max file size if provided, otherwise use the default value
            maxFileSizeMB = args.length > 2 ? Integer.parseInt(args[2]) : defaultMaxFileSizeMB;
            if (maxFileSizeMB <= 0) {
                throw new IllegalArgumentException("Max file size must be greater than 0.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid max file size specified: " + (args.length > 2 ? args[2] : "N/A") + ". Using default max file size: " + defaultMaxFileSizeMB + " MB");
            maxFileSizeMB = defaultMaxFileSizeMB;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Using default max file size: " + defaultMaxFileSizeMB + " MB");
            maxFileSizeMB = defaultMaxFileSizeMB;
        }

        System.out.println("Starting the data merging and conversion process.");
        System.out.println("Input folder: " + inputFolder);
        System.out.println("Output Parquet file: " + outputParquetFile);
        System.out.println("Max file size: " + maxFileSizeMB + " MB");

        // Initialize Hadoop configuration
        Configuration conf = HadoopConfig.getHadoopConfiguration();

        try {
            // Initialize FileMerger
            FileMerger merger = new FileMerger(inputFolder, maxFileSizeMB * 1024L * 1024L, outputParquetFile, conf);

            // Continuously monitor and process files
            while (true) {
                merger.mergeFiles();
                System.out.println("Waiting for new files...");
                // Pause before re-checking the folder
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted: " + e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }

            System.out.println("Data merging and conversion process completed.");
        } catch (Exception e) {
            System.err.println("Failed to initialize FileMerger: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
