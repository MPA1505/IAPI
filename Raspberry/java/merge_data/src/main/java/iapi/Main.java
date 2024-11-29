package iapi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // Check if the required arguments are provided
        if (args.length < 2) {
            System.err.println("Not enough arguments provided. Defaulting to:");
            System.err.println("Input folder: .\\datasets\\datasets_20hz_1_robot_1_minute");
            System.err.println("Output file: .\\datasets\\merged_datasets\\merged_dataset.csv");

            // Default arguments
            args = new String[]{
                    ".\\datasets\\datasets_20hz_1_robot_1_minute",
                    ".\\datasets\\merged_datasets\\merged_dataset.csv"
            };
        }

        String inputFolder = args[0];
        String outputFile = args[1];
        int maxFileSizeMB = 300; // Maximum output file size in MB

        System.out.println("Starting the file merging process.");
        System.out.println("Input folder: " + inputFolder);
        System.out.println("Output file: " + outputFile);

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
