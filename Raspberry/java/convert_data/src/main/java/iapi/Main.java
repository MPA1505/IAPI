package iapi;

import org.apache.commons.csv.CSVRecord;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Not enough arguments provided. Defaulting to:");
            System.out.println("Input folder: .\\datasets\\merged_datasets");
            System.out.println("Output file: .\\datasets\\parquet_files\\dataset.parquet");

            // Default arguments
            args = new String[]{
                    ".\\datasets\\merged_datasets",
                    ".\\datasets\\parquet_files\\dataset.parquet"
            };
        }

        String inputFolder = args[0];
        String outputFile = args[1];

        System.out.println("Starting the file merging process.");
        System.out.println("Input folder: " + inputFolder);
        System.out.println("Output file: " + outputFile);

        List<CSVRecord> records = CSVReader.readCSV("E:\\GitHub\\IAPI\\Raspberry\\datasets\\merged_datasets\\merged_dataset_1.csv");
        List<RobotData> cleanedData = new ArrayList<>();

        int cleanedCount = 0;
        int uncleanedCount = 0;
        int skippedCount = 0;

        for (CSVRecord record : records) {
            try {
                // Attempt to clean and add the row
                RobotData cleanedRow = CSVDataCleaner.cleanRecord(record);
                cleanedData.add(cleanedRow);
                cleanedCount++;
                if (cleanedCount % 1000 == 0) {
                    System.out.println("Total Records: " + records.size());
                    System.out.println("Cleaned Records: " + cleanedCount);
                    System.out.println("Skipped Records: " + skippedCount);
                }
            } catch (IllegalArgumentException e) {
                // If an exception occurs, print the skipped row and expected format
                skippedCount++;
                System.err.println("Skipped Row: " + record);
                System.err.println("Reason: " + e.getMessage());
                System.err.println("Expected Format: { "
                        + "ID: int, Timestamp: double, "
                        + "Actual Joint Positions: [double,...], "
                        + "Actual Joint Velocities: [double,...], "
                        + "Actual Joint Currents: [double,...], "
                        + "etc. }"
                );
            }
        }

        // Print summary
        uncleanedCount = records.size() - cleanedCount - skippedCount;
        System.out.println("Processing Summary:");
        System.out.println("Total Records: " + records.size());
        System.out.println("Cleaned Records: " + cleanedCount);
        System.out.println("Skipped Records: " + skippedCount);
        System.out.println("Uncleaned Records (defaulted): " + uncleanedCount);

        ParquetWriterUtil.writeToParquet(cleanedData, outputFile);
    }

}