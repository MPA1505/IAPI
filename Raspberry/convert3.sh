#!/bin/bash

INPUT_DIR="./datasets/merged_datasets"
OUTPUT_DIR="./datasets/parquet_files"

echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"

java -Xms1g -Xmx2g -jar convert_data_fixed.jar "$INPUT_DIR" "$OUTPUT_DIR"
