package iapi;

import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Not enough arguments provided. Using default values:");
            System.out.println("Input folder: .\\datasets\\merged_datasets");
            System.out.println("Output folder: .\\datasets\\parquet_files");

            // Default arguments
            args = new String[]{
                    ".\\datasets\\merged_datasets",
                    ".\\datasets\\parquet_files"
            };
        }

        String inputFolder = args[0];
        String outputFolder = args[1];

        System.out.println("Monitoring folder for new CSV files...");
        System.out.println("Input folder: " + inputFolder);
        System.out.println("Output folder: " + outputFolder);

        // Set to track processed files
        Set<String> processedFiles = new HashSet<>();

        while (true) {
            System.out.println("Checking folder for new files...");
            File folder = new File(inputFolder);
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));

            if (files != null) {
                for (File file : files) {
                    if (!processedFiles.contains(file.getName())) {
                        System.out.println("Found new file: " + file.getName());

                        // Process the file
                        String inputFilePath = file.getAbsolutePath();
                        String outputFilePath = outputFolder + "\\" + file.getName().replace(".csv", ".parquet");

                        System.out.println("Processing file: " + inputFilePath);
                        processFile(inputFilePath, outputFilePath);

                        // Mark the file as processed
                        processedFiles.add(file.getName());
                        System.out.println("Finished processing file: " + file.getName());
                    }
                }
            } else {
                System.out.println("No files found in folder: " + inputFolder);
            }

            // Wait for 5 seconds before checking again
            System.out.println("Waiting for 5 seconds...");
            Thread.sleep(5000);
        }
    }

    private static void processFile(String inputFilePath, String outputFilePath) {
        try {
            System.out.println("Reading CSV file: " + inputFilePath);
            List<CSVRecord> records = CSVReader.readCSV(inputFilePath);
            System.out.println("Total records found in file: " + records.size());

            List<RobotData> cleanedData = new ArrayList<>();
            int cleanedCount = 0;
            int skippedCount = 0;

            System.out.println("Cleaning data...");
            for (CSVRecord record : records) {
                try {
                    // Attempt to clean and add the row
                    RobotData cleanedRow = CSVDataCleaner.cleanRecord(record);
                    cleanedData.add(cleanedRow);
                    cleanedCount++;

                    if (cleanedCount % 1000 == 0) {
                        System.out.println("Cleaned Records: " + cleanedCount);
                        System.out.println("Skipped Records: " + skippedCount);
                    }
                } catch (IllegalArgumentException e) {
                    // Handle skipped rows
                    skippedCount++;
                    System.out.println("Skipped a row. Reason: " + e.getMessage());
                }
            }

            // Summary
            System.out.println("Cleaning complete for file: " + inputFilePath);
            System.out.println("Total Records: " + records.size());
            System.out.println("Cleaned Records: " + cleanedCount);
            System.out.println("Skipped Records: " + skippedCount);

            System.out.println("Writing cleaned data to Parquet file: " + outputFilePath);
            ParquetWriterUtil.writeToParquet(cleanedData, outputFilePath);
            System.out.println("Parquet file written successfully: " + outputFilePath);

        } catch (Exception e) {
            System.out.println("Failed to process file: " + inputFilePath);
            System.out.println("Reason: " + e.getMessage());
        }
    }
}
