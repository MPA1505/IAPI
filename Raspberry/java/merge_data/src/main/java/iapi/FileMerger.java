package iapi;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

        // Sort files by the smallest number in their names
        Arrays.sort(files, (f1, f2) -> {
            int num1 = extractNumber(f1.getName());
            int num2 = extractNumber(f2.getName());
            return Integer.compare(num1, num2);
        });

        // Process files in order
        Stream.of(files).forEach(file -> {
            if (!processedFiles.contains(file.getName())) {
                System.out.println("Processing file: " + file.getName());
                try {
                    processFile(file);
                    processedFiles.add(file.getName()); // Mark as processed
                    newFilesProcessed.set(true);
                } catch (Exception e) {
                    System.err.println("Error processing file: " + file.getName());
                    System.err.println("Reason: " + e.getMessage());
                }
            }
        });

        return newFilesProcessed.get();
    }

    private int extractNumber(String fileName) {
        // Regex to find the first number in the file name
        String num = fileName.replaceAll("\\D+", ""); // Remove all non-digit characters
        try {
            return Integer.parseInt(num); // Parse the number
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Return a large value if no number is found
        }
    }

    private synchronized void processFile(File file) throws IOException, CsvValidationException {
        File currentOutputFile = getOrCreateOutputFile();
        List<String[]> buffer = new ArrayList<>(); // Buffer for batch writing
        int skippedRows = 0; // Counter for skipped rows

        try (CSVReader csvReader = new CSVReader(new BufferedReader(new FileReader(file)))) {
            String[] header = csvReader.readNext();
            if (header == null) {
                throw new IOException("Empty file: " + file.getName());
            }

            // Determine the expected number of columns from the header
            int expectedColumns = header.length;

            // Write header only if the file is new
            synchronized (this) {
                if (currentFileSize.get() == 0) {
                    List<String[]> headerAsList = new ArrayList<>();
                    headerAsList.add(header);
                    writeBatchToFile(currentOutputFile, headerAsList); // Write header to the new file
                    currentFileSize.addAndGet(calculateRowSize(header));
                }
            }

            String[] row;
            while (true) {
                try {
                    row = csvReader.readNext();
                    if (row == null) break;

                    // Validate the row
                    if (isValidRow(row, expectedColumns)) {
                        buffer.add(row);
                    } else {
                        skippedRows++;
                    }

                    long rowSize = calculateRowSize(row);

                    // Check if the current file size exceeds the limit
                    if (currentFileSize.addAndGet(rowSize) > maxFileSizeBytes) {
                        synchronized (this) {
                            // Flush buffer to the current file and create a new one
                            writeBatchToFile(currentOutputFile, buffer);
                            buffer.clear(); // Clear buffer for next batch
                            currentOutputFile = getNextOutputFile();
                            List<String[]> headerAsList = new ArrayList<>();
                            headerAsList.add(header);
                            writeBatchToFile(currentOutputFile, headerAsList); // Write header to the new file
                            currentFileSize.set(calculateRowSize(header) + rowSize);
                        }
                    }
                } catch (CsvMalformedLineException e) {
                    skippedRows++;
                    System.err.println("Skipped malformed row: " + e.getMessage());
                }
            }

            // Write remaining rows in the buffer
            if (!buffer.isEmpty()) {
                synchronized (this) {
                    writeBatchToFile(currentOutputFile, buffer);
                }
            }

            if (skippedRows > 0) {
                System.out.println("Skipped rows: " + skippedRows);
            }
        } catch (CsvMalformedLineException e) {
            System.err.println("Error processing file: " + file.getName());
            System.err.println("Reason: " + e.getMessage());
        }
    }

    private boolean isValidRow(String[] row, int expectedColumns) {
        // Validate the row by checking the number of columns and ensuring no field is null or empty
        return row.length == expectedColumns && Arrays.stream(row).allMatch(field -> field != null && !field.trim().isEmpty());
    }

    private void writeBatchToFile(File file, List<String[]> rows) throws IOException {
        try (CSVWriter csvWriter = new CSVWriter(new BufferedWriter(new FileWriter(file, true)))) {
            csvWriter.writeAll(rows); // Batch write
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
        File newFile = getOrCreateOutputFile();
        System.out.println("Switching to new output file: " + newFile.getAbsolutePath());
        return newFile;
    }


    private long calculateRowSize(String[] row) {
        return Arrays.stream(row).mapToInt(String::length).sum() + row.length + 1; // Approximation
    }
}
