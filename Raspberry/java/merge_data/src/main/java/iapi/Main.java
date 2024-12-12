package iapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // Default values
        String defaultInputFolder = ".\\datasets\\datasets_20hz_1_robot_1_minute";
        String defaultOutputFile = ".\\datasets\\merged_datasets\\merged_dataset.csv";
        int defaultMaxFileSizeMB = 300;

// Parse arguments
        String inputFolder = args.length > 0 ? args[0] : defaultInputFolder;
        String outputFile = args.length > 1 ? args[1] : defaultOutputFile;
        int maxFileSizeMB;

        try {
            // Parse max file size if provided, otherwise use the default value
            maxFileSizeMB = args.length > 2 ? Integer.parseInt(args[2]) : defaultMaxFileSizeMB;
            if (maxFileSizeMB <= 0) {
                throw new IllegalArgumentException("Max file size must be greater than 0.");
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid max file size specified: " + (args.length > 2 ? args[2] : "N/A"));
            System.err.println("Using default max file size: " + defaultMaxFileSizeMB + " MB");
            maxFileSizeMB = defaultMaxFileSizeMB;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Using default max file size: " + defaultMaxFileSizeMB + " MB");
            maxFileSizeMB = defaultMaxFileSizeMB;
        }

        System.out.println("Starting the file merging process.");
        System.out.println("Input folder: " + inputFolder);
        System.out.println("Output file: " + outputFile);
        System.out.println("Max file size: " + maxFileSizeMB + " MB");

        // Validate input folder
        File folder = new File(inputFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Error: Input folder does not exist or is not a directory: " + inputFolder);
            System.exit(1);
        }

        // Ensure output file's parent directory exists
        try {
            Files.createDirectories(Paths.get(outputFile).getParent());
        } catch (IOException e) {
            System.err.println("Failed to create output folder: " + e.getMessage());
            return;
        }

        // Initialize FileMerger
        FileMerger merger = new FileMerger(inputFolder, outputFile, maxFileSizeMB);

        // Continuously monitor and process files
        while (true) {
            try {
                boolean newFilesProcessed = merger.mergeFiles();
                if (!newFilesProcessed) {
                    System.out.println("No new files found. Waiting for updates...");
                }
            } catch (Exception e) {
                System.err.println("Error during merging: " + e.getMessage());
                e.printStackTrace();
            }

            // Pause before re-checking the folder
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
                break;
            }
        }

        System.out.println("File merging process completed.");
    }
}
