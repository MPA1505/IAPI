package iapi;

import java.io.*;
import java.util.*;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;

public class ChunkProcessor {
    private final String inputFile;
    private final String outputFolder;
    private final int chunkSize;

    public ChunkProcessor(String inputFile, String outputFolder, int chunkSize) {
        this.inputFile = inputFile;
        this.outputFolder = outputFolder;
        this.chunkSize = chunkSize;
    }

    public void processFile() throws IOException {
        try (Reader reader = new FileReader(inputFile);
             CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IOException("Empty CSV file.");
            }

            // Add "ID" to the header
            String[] modifiedHeader = CSVUtils.addColumnToHeader(header, "ID");

            List<String[]> chunk = new ArrayList<>();
            int chunkCounter = 1;
            String[] row;

            while ((row = csvReader.readNext()) != null) {
                chunk.add(row);

                if (chunk.size() == chunkSize) {
                    // Save the current chunk to a file
                    saveChunk(chunk, modifiedHeader, chunkCounter++);
                    chunk.clear();
                }
            }

            // Save any remaining rows
            if (!chunk.isEmpty()) {
                saveChunk(chunk, modifiedHeader, chunkCounter);
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveChunk(List<String[]> chunk, String[] header, int chunkCounter) {
        String outputFile = outputFolder + "/dataset_part_" + chunkCounter + ".csv";
        try {
            CSVUtils.writeChunkToFile(outputFile, header, chunk);
            System.out.println("Saved chunk " + chunkCounter + " to " + outputFile);
        } catch (IOException e) {
            System.err.println("Error saving chunk " + chunkCounter + ": " + e.getMessage());
        }
    }
}