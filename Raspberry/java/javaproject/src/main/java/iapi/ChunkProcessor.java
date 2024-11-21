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
    private final ExecutorService executor;

    public ChunkProcessor(String inputFile, String outputFolder, int chunkSize, int numThreads) {
        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
        this.chunkSize = chunkSize;
        this.executor = Executors.newFixedThreadPool(numThreads); // Create a thread pool
    }

    public void processFile() throws IOException {
        try (Reader reader = new BufferedReader(new FileReader(inputFile));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IOException("Empty CSV file.");
            }

            // Add "ID" to the header
            String[] modifiedHeader = CSVUtils.addColumnToHeader(header, "ID");

            List<String[]> chunk = new ArrayList<>(chunkSize);
            int chunkCounter = 1;
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
                final int currentChunk = chunkCounter;
                final List<String[]> chunkToProcess = new ArrayList<>(chunk);
                executor.submit(() -> saveChunk(chunkToProcess, modifiedHeader, currentChunk));
            }

        } catch (CsvValidationException e) {
            throw new IOException("CSV validation error while reading file: " + inputFile, e);
        } finally {
            executor.shutdown();
            try {
                // Wait for all threads to finish
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Thread pool interrupted during shutdown.", e);
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
