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
            System.err.println("Input file: .\\datasets\\right_arm.csv");
            System.err.println("Output folder: .\\datasets\\datasets_20hz_1_robot_1_minute");
            System.err.println("Optional chunkSize: 1200");
            
            // Default arguments
            args = new String[] { ".\\datasets\\right_arm.csv", ".\\datasets\\datasets_20hz_1_robot_1_minute" };
        } else if (args.length < 3) {
            System.err.println("Optional chunkSize not provided. Proceeding with default values for input and output.");
        }
        
        // Read paths from command-line arguments
        String inputFile = args[0];
        String outputFolder = args[1];
        
        // Optional: handle chunkSize if provided
        int chunkSize = args.length >= 3 ? Integer.parseInt(args[2]) : 1200; // Example default value
        
        int numThreads = Runtime.getRuntime().availableProcessors(); // Use all available CPU cores

        // Allow optional chunk size argument
        if (args.length > 2) {
            try {
                chunkSize = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid chunk size. Using default value: " + chunkSize);
            }
        }

        System.out.println("Starting the CSV chunk processing script.");
        System.out.println("Input file: " + inputFile);
        System.out.println("Output folder: " + outputFolder);
        System.out.println("Chunk size: " + chunkSize);
        System.out.println("Threads: " + numThreads);

        // Validate input file
        File input = new File(inputFile);
        if (!input.exists() || !input.isFile()) {
            System.err.println("Error: Input file does not exist or is not a file: " + inputFile);
            System.exit(1);
        }

        // Validate output folder
        File output = new File(outputFolder);
        if (!output.exists()) {
            if (!output.mkdirs()) {
                System.err.println("Error: Could not create output folder: " + outputFolder);
                System.exit(1);
            }
        }

        // Ensure the output folder exists
        try {
            Files.createDirectories(Paths.get(outputFolder));
        } catch (IOException e) {
            System.err.println("Failed to create output folder: " + e.getMessage());
            return;
        }

        try {
            ChunkProcessor processor = new ChunkProcessor(inputFile, outputFolder, chunkSize, numThreads);
            processor.processFile();
        } catch (Exception e) {
            System.err.println("Error during processing: " + e.getMessage());
        }

        System.out.println("CSV chunk processing completed.");
    }
}
