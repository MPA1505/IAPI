package iapi;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class FileMerger {
    private final String inputFolder;
    private final String outputFile;
    private final long maxFileSizeBytes;
    private final Set<String> processedFiles; // Thread-safe set
    private final AtomicLong currentFileSize; // Tracks the current output file size
    private volatile int fileIndex; // Tracks the current output file index

    public FileMerger(String inputFolder, String outputFile, int maxFileSizeMB) {
        this.inputFolder = inputFolder;
        this.outputFile = outputFile;
        this.maxFileSizeBytes = maxFileSizeMB * 1024L * 1024L; // Convert MB to bytes
        this.processedFiles = ConcurrentHashMap.newKeySet(); // Thread-safe set
        this.currentFileSize = new AtomicLong(0); // Tracks current output file size
        this.fileIndex = 1; // Start file numbering
    }

    public boolean mergeFiles() {
        File folder = new File(inputFolder);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No files in the input folder.");
            return false;
        }

        AtomicBoolean newFilesProcessed = new AtomicBoolean(false);

        // Process files in parallel
        Stream.of(files).parallel().forEach(file -> {
            if (!processedFiles.contains(file.getName())) {
                System.out.println("Processing file: " + file.getName());
                try {
                    processFile(file);
                    processedFiles.add(file.getName()); // Mark as processed
                    newFilesProcessed.set(true);
                } catch (Exception e) {
                    System.err.println("Error processing file: " + file.getName());
                    e.printStackTrace();
                }
            }
        });

        return newFilesProcessed.get();
    }

    private synchronized void processFile(File file) throws IOException, CsvValidationException {
        File currentOutputFile = getOrCreateOutputFile();
        try (CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(file)));
             CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new FileWriter(currentOutputFile, true)))) {

            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IOException("Empty file: " + file.getName());
            }

            // Write header only if file is new
            if (currentFileSize.get() == 0) {
                csvWriter.writeNext(header);
                currentFileSize.addAndGet(calculateRowSize(header));
            }

            String[] row;
            while ((row = csvReader.readNext()) != null) {
                long rowSize = calculateRowSize(row);

                // Check if the current file size exceeds the limit
                if (currentFileSize.addAndGet(rowSize) > maxFileSizeBytes) {
                    System.out.println("File size limit reached. Creating new file...");
                    currentOutputFile = getNextOutputFile();
                    try (CSVWriter newCsvWriter = new CSVWriter(new BufferedWriter(new FileWriter(currentOutputFile)))) {
                        newCsvWriter.writeNext(header); // Write header to new file
                        currentFileSize.set(calculateRowSize(header) + rowSize); // Reset size
                        newCsvWriter.writeNext(row); // Write current row to new file
                    }
                } else {
                    csvWriter.writeNext(row); // Write row to current file
                }
            }
        }
    }

    private synchronized File getOrCreateOutputFile() throws IOException {
        File file = Paths.get(outputFile.replace(".csv", "_" + fileIndex + ".csv")).toFile();
        if (!file.exists()) {
            System.out.println("Creating new output file: " + file.getName());
            currentFileSize.set(0); // Reset file size tracker for the new file
        }
        return file;
    }

    private synchronized File getNextOutputFile() throws IOException {
        fileIndex++;
        return getOrCreateOutputFile();
    }

    private long calculateRowSize(String[] row) {
        return Arrays.stream(row).mapToInt(String::length).sum() + row.length + 1; // Approximation
    }
}
