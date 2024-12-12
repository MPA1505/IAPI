package iapi.merge_data;

import iapi.convert_data.CSVDataCleaner;
import iapi.convert_data.ParquetWriterUtil;
import iapi.convert_data.RobotData;
import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileMerger {

    private final String inputFolder;
    private final long maxFileSizeBytes;
    private final Set<String> processedFiles; // Thread-safe set
    private final ParquetWriterUtil parquetWriterUtil;

    public FileMerger(String inputFolder, long maxFileSizeBytes, String outputFilePath, org.apache.hadoop.conf.Configuration conf) throws IOException {
        this.inputFolder = inputFolder;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.processedFiles = ConcurrentHashMap.newKeySet(); // Thread-safe set
        this.parquetWriterUtil = ParquetWriterUtil.getInstance(outputFilePath, conf, maxFileSizeBytes);
    }

    /**
     * Merges new CSV files and writes the combined data to Parquet files.
     */
    public void mergeFiles() {
        File folder = new File(inputFolder);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            System.out.println("No files in the input folder.");
            return;
        }

        // Sort files by the smallest number in their names
        Arrays.sort(files, (f1, f2) -> {
            int num1 = extractNumber(f1.getName());
            int num2 = extractNumber(f2.getName());
            return Integer.compare(num1, num2);
        });

        // Process files in order
        for (File file : files) {
            if (!processedFiles.contains(file.getName())) {
                System.out.println("Processing file: " + file.getName());
                try {
                    processFile(file);
                    processedFiles.add(file.getName()); // Mark as processed
                } catch (Exception e) {
                    System.err.println("Error processing file: " + file.getName() + ". Reason: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Extracts the first number found in a file name.
     *
     * @param fileName The name of the file.
     * @return The extracted number or Integer.MAX_VALUE if none found.
     */
    private int extractNumber(String fileName) {
        // Regex to find the first number in the file name
        String num = fileName.replaceAll("\\D+", ""); // Remove all non-digit characters
        try {
            return Integer.parseInt(num); // Parse the number
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Return a large value if no number is found
        }
    }

    /**
     * Processes a single CSV file and writes each RobotData record to Parquet.
     *
     * @param file The CSV file to process.
     * @throws IOException
     */
    private void processFile(File file) throws IOException {
        int skippedRows = 0; // Counter for skipped rows

        try (
                Reader reader = new BufferedReader(new FileReader(file));
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())
        ) {
            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            if (headerMap == null || headerMap.isEmpty()) {
                throw new IOException("Empty or invalid header in file: " + file.getName());
            }

            for (CSVRecord record : csvParser) {
                try {
                    // Validate and clean the record
                    RobotData data = CSVDataCleaner.cleanRecord(record);
                    // Write directly to Parquet
                    parquetWriterUtil.writeRecord(data);
                } catch (IllegalArgumentException e) {
                    skippedRows++;
                    System.out.println("Skipped a row in " + file.getName() + ". Reason: " + e.getMessage());
                } catch (UncheckedIOException e) {
                    if (e.getCause() instanceof org.apache.commons.csv.CSVException) {
                        System.out.println("Skipped file " + file.getName() + ". The file is malformed");
                    } else {
                        System.err.println("I/O error while writing to Parquet: " + e.getMessage());
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    System.err.println("I/O error while writing to Parquet: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (skippedRows > 0) {
                System.out.println("Skipped rows in file " + file.getName() + ": " + skippedRows);
            }
        } catch (UncheckedIOException e) {
            if (e.getCause() instanceof org.apache.commons.csv.CSVException) {
                System.out.println("Skipped file " + file.getName() + ". The file is malformed");
            } else {
                System.err.println("I/O error while processing file: " + file.getName() + ". Reason: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.err.println("Error processing file: " + file.getName() + ". Reason: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
