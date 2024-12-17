#!/bin/bash

INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.parquet"
FILE_SIZE_MB="300"
JAR_FILE="unified_project-1.0-SNAPSHOT_fixed.jar"
BOOTSTRAP_SERVER="129.151.195.201:9093"
TOPIC="test-topic"

echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"
echo "File size: $FILE_SIZE_MB"
echo "JAR file: $JAR_FILE"
echo "Bootstrap server: $BOOTSTRAP_SERVER"
echo "Topic: $TOPIC"

java -Xms1g -Xmx2g -jar "$JAR_FILE" --input="$INPUT_DIR" --output="$OUTPUT_DIR" --size="$FILE_SIZE_MB" --server="$BOOTSTRAP_SERVER" --topic="$TOPIC"