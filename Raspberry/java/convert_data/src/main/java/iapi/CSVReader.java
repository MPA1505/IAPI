package iapi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
    public static List<CSVRecord> readCSV(String filePath) throws Exception {
        Reader reader = new FileReader(filePath);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withHeader()
                .parse(reader);
        List<CSVRecord> csvRecords = new ArrayList<>();
        for (CSVRecord record : records) {
            csvRecords.add(record);
        }
        return csvRecords;
    }
}

