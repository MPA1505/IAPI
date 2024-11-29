package iapi;

import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.hadoop.conf.Configuration;

import java.util.List;

public class ParquetWriterUtil {
    public static void writeToParquet(List<RobotData> cleanedData, String outputFile) {
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", "file:///"); // Use local file system
            conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());

            Path path = new Path(outputFile);
            ParquetWriter<RobotData> writer = AvroParquetWriter.<RobotData>builder(HadoopOutputFile.fromPath(path, new Configuration()))
                    .withSchema(RobotData.getClassSchema())
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                    .build();

            for (RobotData data : cleanedData) {
                writer.write(data);
            }

            writer.close();
            System.out.println("Data successfully written to Parquet file: " + outputFile);
        } catch (Exception e) {
            System.err.println("Error writing to Parquet file: " + e.getMessage());
        }
    }
}

