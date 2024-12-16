#!/bin/bash

INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.parquet"
JAR_FILE="unified_project-1.0-SNAPSHOT_fixed.jar"

echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"
echo "JAR file: $JAR_FILE"

java -Xms1g -Xmx2g -jar "$JAR_FILE" "$INPUT_DIR" "$OUTPUT_DIR"