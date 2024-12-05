#!/bin/bash

INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.csv"

echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"

java -jar merge_data-1.0-SNAPSHOT.jar "$INPUT_DIR" "$OUTPUT_DIR"
