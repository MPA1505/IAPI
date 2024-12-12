package iapi.convert_data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.ExampleParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ParquetWriterUtil {
    private static ParquetWriterUtil instance;

    private ParquetWriter<RobotData> writer;
    private String outputFilePath;
    private Configuration conf;
    private long maxFileSizeBytes;
    private AtomicLong currentFileSize;
    private int fileIndex;

    private ParquetWriterUtil(String outputFilePath, Configuration conf, long maxFileSizeBytes) throws IOException {
        this.outputFilePath = outputFilePath;
        this.conf = conf;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.currentFileSize = new AtomicLong(0);
        this.fileIndex = 1;
        initializeWriter();
    }

    public static synchronized ParquetWriterUtil getInstance(String outputFilePath, Configuration conf, long maxFileSizeBytes) throws IOException {
        if (instance == null) {
            instance = new ParquetWriterUtil(outputFilePath, conf, maxFileSizeBytes);
        }
        return instance;
    }

    private void initializeWriter() throws IOException {
        String newOutputPath = generateNewOutputPath();
        Path path = new Path(newOutputPath);
        writer = AvroParquetWriter.<RobotData>builder(HadoopOutputFile.fromPath(path, conf))
                .withSchema(RobotData.getClassSchema())
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE) // Ensure OVERWRITE mode
                .build();

        System.out.println("Initialized Parquet writer for file: " + newOutputPath);
    }

    private String generateNewOutputPath() {
        // Ensure outputFilePath is treated as a directory
        Path outputPath = new Path(outputFilePath);
        String folderName = outputPath.getName();

        // If outputFilePath ends with .parquet, treat it as a file-like name and remove it
        if (folderName.endsWith(".parquet")) {
            folderName = folderName.substring(0, folderName.lastIndexOf('.'));
            outputPath = outputPath.getParent(); // Get the parent directory
        }

        // Construct the new file path with the folder name and index
        return outputPath + "/" + folderName + "_" + fileIndex + ".parquet";
    }

    public synchronized void writeRecord(RobotData record) throws IOException {
        if (currentFileSize.get() >= maxFileSizeBytes) {
            // Close current writer and initialize a new one
            closeWriter();
            fileIndex++;
            initializeWriter();
            currentFileSize.set(0); // Reset file size counter
            System.out.println("Max file size reached. Created new Parquet file: " + generateNewOutputPath());
        }

        writer.write(record);
        // Approximate size increment based on Avro serialization size
        long recordSize = record.toString().getBytes().length;
        currentFileSize.addAndGet(recordSize);
    }

    private long calculateRecordSize(RobotData record) {
        // Approximation: Sum of string lengths and numerical bytes
        // Adjust this method based on your actual data size estimation needs
        return record.toString().getBytes().length;
    }

    public synchronized void closeWriter() {
        if (writer != null) {
            try {
                writer.close();
                System.out.println("Closed Parquet writer.");
            } catch (IOException e) {
                System.err.println("Error closing Parquet writer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    // Ensure that writer is closed when the application shuts down
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.closeWriter();
                System.out.println("Shutdown hook executed. Parquet writer closed.");
            }
        }));
    }
}
