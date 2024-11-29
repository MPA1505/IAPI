package iapi;

import org.apache.commons.csv.CSVRecord;

public class CSVDataCleaner {
    public static RobotData cleanRecord(CSVRecord record) {
        return new RobotData(
                DataValidator.parseInt(record.get("ID")),
                DataValidator.parseDouble(record.get("Timestamp")),
                DataValidator.parseArray(record.get("Actual Joint Positions")),
                DataValidator.parseArray(record.get("Actual Joint Velocities")),
                DataValidator.parseArray(record.get("Actual Joint Currents")),
                DataValidator.parseArray(record.get("Actual Cartesian Coordinates")),
                DataValidator.parseArray(record.get("Actual Tool Speed")),
                DataValidator.parseArray(record.get("Generalized Forces")),
                DataValidator.parseArray(record.get("Temperature of Each Joint")),
                DataValidator.parseDouble(record.get("Execution Time")),
                DataValidator.parseInt(record.get("Safety Status")),
                DataValidator.parseArray(record.get("Tool Acceleration")),
                DataValidator.parseDouble(record.get("Norm of Cartesian Linear Momentum")),
                DataValidator.parseDouble(record.get("Robot Current")),
                DataValidator.parseArray(record.get("Joint Voltages")),
                DataValidator.parseArray(record.get("Elbow Position")),
                DataValidator.parseArray(record.get("Elbow Velocity")),
                DataValidator.parseDouble(record.get("Tool Current")),
                DataValidator.parseDouble(record.get("Tool Temperature")),
                DataValidator.parseDouble(record.get("TCP Force")),
                DataValidator.parseInt(record.get("Anomaly State"))
        );
    }
}

