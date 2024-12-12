package iapi;

import org.apache.hadoop.conf.Configuration;

public class HadoopConfig {
    public static Configuration getHadoopConfiguration() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");

        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            System.out.println("Configuring for Windows...");
            conf.set("hadoop.home.dir", "C:\\hadoop"); // Ensure winutils.exe is in this path
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
            System.out.println("Configuring for Linux/Unix...");
            conf.set("hadoop.home.dir", "/tmp"); // A dummy directory, as winutils.exe is not required on Linux
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + osName);
        }

        return conf;
    }
}
