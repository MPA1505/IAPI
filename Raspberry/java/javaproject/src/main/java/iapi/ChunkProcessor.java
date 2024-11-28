package iapi;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

public class ChunkProcessor {
    private final String inputFile;
    private final String outputFolder;
    private final int chunkSize;
    private int chunkCounter; // Persistent chunk counter
    private final int numThreads;

    public ChunkProcessor(String inputFile, String outputFolder, int chunkSize, int numThreads) {
        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
        this.chunkSize = chunkSize;
        this.numThreads = numThreads;
        this.chunkCounter = 1; // Initialize chunk counter
    }

    public void processFile() throws IOException {
        while (true) { // Loop forever
            ExecutorService executor = Executors.newFixedThreadPool(numThreads); // Re-instantiate executor
            try (Reader reader = new BufferedReader(new FileReader(inputFile));
                 CSVReader csvReader = new CSVReader(reader)) {

                String[] header = csvReader.readNext();
                if (header == null) {
                    throw new IOException("Empty CSV file.");
                }

                // Add "ID" to the header
                String[] modifiedHeader = CSVUtils.addColumnToHeader(header, "ID");

                List<String[]> chunk = new ArrayList<>(chunkSize);
                String[] row;

                while ((row = csvReader.readNext()) != null) {
                    chunk.add(row);

                    if (chunk.size() == chunkSize) {
                        // Submit chunk to the thread pool
                        final int currentChunk = chunkCounter++;
                        final List<String[]> chunkToProcess = new ArrayList<>(chunk);
                        executor.submit(() -> saveChunk(chunkToProcess, modifiedHeader, currentChunk));
                        chunk.clear();
                    }
                }

                // Process remaining rows
                if (!chunk.isEmpty()) {
                    final int currentChunk = chunkCounter++;
                    final List<String[]> chunkToProcess = new ArrayList<>(chunk);
                    executor.submit(() -> saveChunk(chunkToProcess, modifiedHeader, currentChunk));
                }

                // Wait for all tasks to finish before restarting
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Thread pool interrupted during shutdown.", e);
                }
            } catch (CsvValidationException e) {
                throw new IOException("CSV validation error while reading file: " + inputFile, e);
            }
        }
    }

    private void saveChunk(List<String[]> chunk, String[] header, int chunkCounter) {
        String outputFile = new File(outputFolder, "dataset_part_" + chunkCounter + ".csv").getPath();
        try {
            CSVUtils.writeChunkToFile(outputFile, header, chunk);
            System.out.println("Saved chunk " + chunkCounter + " to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error saving chunk " + chunkCounter + ": " + e.getMessage());
        }
    }
}
