package iapi.convert_data;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopOutputFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ParquetWriterUtil {

    private static volatile ParquetWriterUtil instance;

    private ParquetWriter<RobotData> writer;
    private final String outputFilePath;
    private final Configuration conf;
    private final long maxFileSizeBytes;
    private final AtomicLong currentFileSize = new AtomicLong(0);
    private int fileIndex = 1;

    private final BlockingQueue<RobotData> recordQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunning = true;
    private final Thread writerThread;

    private ParquetWriterUtil(String outputFilePath, Configuration conf, long maxFileSizeBytes) throws IOException {
        this.outputFilePath = outputFilePath;
        this.conf = conf;
        this.maxFileSizeBytes = maxFileSizeBytes;
        initializeWriter();

        // Start the asynchronous writer thread
        writerThread = new Thread(this::processQueue);
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public static ParquetWriterUtil getInstance(String outputFilePath, Configuration conf, long maxFileSizeBytes) throws IOException {
        if (instance == null) {
            synchronized (ParquetWriterUtil.class) {
                if (instance == null) {
                    instance = new ParquetWriterUtil(outputFilePath, conf, maxFileSizeBytes);
                }
            }
        }
        return instance;
    }

    private void initializeWriter() throws IOException {
        String newOutputPath = generateNewOutputPath();
        Path path = new Path(newOutputPath);
        writer = AvroParquetWriter.<RobotData>builder(HadoopOutputFile.fromPath(path, conf))
                .withSchema(RobotData.getClassSchema())
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .build();
        System.out.println("Initialized Parquet writer for file: " + newOutputPath);
    }

    private String generateNewOutputPath() {
        Path outputPath = new Path(outputFilePath);
        String folderName = outputPath.getName();

        if (folderName.endsWith(".parquet")) {
            folderName = folderName.substring(0, folderName.lastIndexOf('.'));
            outputPath = outputPath.getParent();
        }
        return outputPath + "/" + folderName + "_" + fileIndex + ".parquet";
    }

    public void writeRecord(RobotData record) {
        if (!isRunning) {
            throw new IllegalStateException("Cannot write to a closed writer.");
        }
        recordQueue.offer(record);
    }

    public synchronized void writeBatch(List<RobotData> records) throws IOException {
        for (RobotData record : records) {
            writeRecord(record);
        }
    }

    private void processQueue() {
        try {
            while (isRunning || !recordQueue.isEmpty()) {
                RobotData record = recordQueue.poll(); // Fetch a record from the queue
                if (record != null) {
                    writeRecordToFile(record);
                }
            }
        } catch (Exception e) {
            System.err.println("Error while processing record queue: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeWriter();
        }
    }

    private synchronized void writeRecordToFile(RobotData record) throws IOException {
        if (currentFileSize.get() >= maxFileSizeBytes) {
            rotateFile();
        }
        writer.write(record);
        currentFileSize.addAndGet(estimateRecordSize(record));
    }

    private long estimateRecordSize(RobotData record) {
        return record.toString().getBytes().length; // Replace with better estimation if needed
    }

    private void rotateFile() throws IOException {
        closeWriter();
        fileIndex++;
        initializeWriter();
        currentFileSize.set(0);
        System.out.println("Max file size reached. Created new Parquet file: " + generateNewOutputPath());
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

    public void shutdown() {
        isRunning = false;
        try {
            writerThread.join(); // Wait for the writer thread to finish processing
        } catch (InterruptedException e) {
            System.err.println("Writer thread interrupted during shutdown: " + e.getMessage());
            e.printStackTrace();
        }
        closeWriter();
        System.out.println("Shutdown complete. Parquet writer closed.");
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null) {
                instance.shutdown();
                System.out.println("Shutdown hook executed. Parquet writer closed.");
            }
        }));
    }
}
