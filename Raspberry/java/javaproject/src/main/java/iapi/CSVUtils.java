package iapi;

import java.io.*;
import java.util.*;
import com.opencsv.*;

public class CSVUtils {
    public static String[] addColumnToHeader(String[] header, String newColumn) {
        // Use ArrayList for better flexibility if adding/removing columns dynamically
        List<String> headerList = new ArrayList<>(Arrays.asList(header));
        headerList.add(0, newColumn);
        return headerList.toArray(new String[0]);
    }

    public static void writeChunkToFile(String filePath, String[] header, List<String[]> chunk) throws IOException {
        try (Writer writer = new BufferedWriter(new FileWriter(filePath)); // Use BufferedWriter for efficiency
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // Write header
            csvWriter.writeNext(header);

            // Reuse a single array for row modification to reduce object creation
            String[] modifiedRow = new String[header.length];
            modifiedRow[0] = "1"; // Static "ID" column value

            // Write rows
            for (String[] row : chunk) {
                System.arraycopy(row, 0, modifiedRow, 1, row.length); // Avoid creating new arrays per row
                csvWriter.writeNext(modifiedRow);
            }
        } catch (IOException e) {
            throw new IOException("Error writing chunk to file: " + filePath, e);
        }
    }
}
