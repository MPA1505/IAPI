package iapi;

import java.io.*;
import java.nio.file.*;

public class Main {
    private static final int CHUNK_SIZE = 1200; // Number of rows per chunk
    private static final String INPUT_FILE = "./datasets/right_arm.csv";
    private static final String OUTPUT_FOLDER = "./datasets/datasets_20hz_1_robot_1_minute";

    public static void main(String[] args) {
        System.out.println("Starting the CSV chunk processing script.");
        System.out.println("Input file: " + INPUT_FILE);
        System.out.println("Output folder: " + OUTPUT_FOLDER);
        System.out.println("Chunk size: " + CHUNK_SIZE);

        // Ensure the output folder exists
        try {
            Files.createDirectories(Paths.get(OUTPUT_FOLDER));
        } catch (IOException e) {
            System.err.println("Failed to create output folder: " + e.getMessage());
            return;
        }

        // Process the CSV file
        try {
            ChunkProcessor processor = new ChunkProcessor(INPUT_FILE, OUTPUT_FOLDER, CHUNK_SIZE);
            processor.processFile();
        } catch (IOException e) {
            System.err.println("Error during processing: " + e.getMessage());
        }

        System.out.println("CSV chunk processing completed.");
    }
}
