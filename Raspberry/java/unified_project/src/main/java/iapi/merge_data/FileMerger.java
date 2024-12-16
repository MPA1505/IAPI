package iapi.merge_data;

import iapi.convert_data.CSVDataCleaner;
import iapi.convert_data.ParquetWriterUtil;
import iapi.convert_data.RobotData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class FileMerger {

    private final String inputFolder;
    private final long maxFileSizeBytes;
    private final Set<String> processedFiles;
    private final ParquetWriterUtil parquetWriterUtil;
    private final ExecutorService executor;

    public FileMerger(String inputFolder, long maxFileSizeBytes, String outputFilePath, org.apache.hadoop.conf.Configuration conf) throws IOException {
        this.inputFolder = inputFolder;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.processedFiles = ConcurrentHashMap.newKeySet();
        this.parquetWriterUtil = ParquetWriterUtil.getInstance(outputFilePath, conf, maxFileSizeBytes);
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Merges files concurrently.
     */
    public void mergeFilesConcurrently() {
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

        List<Future<?>> futures = new ArrayList<>();
        for (File file : files) {
            if (processedFiles.add(file.getName())) {
                futures.add(executor.submit(() -> {
                    try {
                        processFile(file);
                        System.out.println("File processed: " + file.getName());
                    } catch (IOException e) {
                        System.err.println("Error processing file: " + file.getName() + ". Reason: " + e.getMessage());
                    }
                }));
            }
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error during parallel file processing: " + e.getMessage());
            }
        }
    }

    /**
     * Processes a single file with batch writing.
     */
    private void processFile(File file) throws IOException {
        List<RobotData> batch = new ArrayList<>();
        int skippedRows = 0;

        try (
                Reader reader = new BufferedReader(new FileReader(file));
                CSVParser csvParser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreSurroundingSpaces(true)
                        .build()
                        .parse(reader)
        ) {
            for (CSVRecord record : csvParser) {
                try {
                    RobotData data = CSVDataCleaner.cleanRecord(record);
                    batch.add(data);

                    if (batch.size() >= 1000) { // Write in batches of 1000
                        parquetWriterUtil.writeBatch(batch);
                        batch.clear();
                    }
                } catch (IllegalArgumentException e) {
                    skippedRows++;
                }
            }

            // Write remaining records in the batch
            if (!batch.isEmpty()) {
                parquetWriterUtil.writeBatch(batch);
            }

            if (skippedRows > 0) {
                System.out.println("Skipped rows in file " + file.getName() + ": " + skippedRows);
            }
        }
    }

    /**
     * Extracts a number from the file name for sorting.
     */
    private int extractNumber(String fileName) {
        String num = fileName.replaceAll("\\D+", "");
        return num.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(num);
    }

    /**
     * Shuts down resources.
     */
    public void shutdown() {
        try {
            parquetWriterUtil.closeWriter();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Error shutting down FileMerger: " + e.getMessage());
        }
    }
}
