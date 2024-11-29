package iapi;

import java.util.ArrayList;
import java.util.List;

public class DataValidator {
    public static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer: " + value);
        }
    }

    public static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid double: " + value);
        }
    }

    public static List<Double> parseArray(String value) {
        try {
            value = value.replace("[", "").replace("]", "");
            String[] parts = value.split(",");
            List<Double> result = new ArrayList<>();
            for (String part : parts) {
                result.add(Double.parseDouble(part.trim()));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid array: " + value);
        }
    }
}

