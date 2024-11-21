package iapi;

import java.io.*;
import java.util.*;
import com.opencsv.*;

public class CSVUtils {
    public static String[] addColumnToHeader(String[] header, String newColumn) {
        String[] modifiedHeader = new String[header.length + 1];
        modifiedHeader[0] = newColumn;
        System.arraycopy(header, 0, modifiedHeader, 1, header.length);
        return modifiedHeader;
    }

    public static void writeChunkToFile(String filePath, String[] header, List<String[]> chunk) throws IOException {
        try (Writer writer = new FileWriter(filePath);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            // Write header
            csvWriter.writeNext(header);

            // Write rows with an "ID" column added
            for (String[] row : chunk) {
                String[] modifiedRow = new String[row.length + 1];
                modifiedRow[0] = "1"; // Static "ID" column value
                System.arraycopy(row, 0, modifiedRow, 1, row.length);
                csvWriter.writeNext(modifiedRow);
            }
        }
    }
}
