#!/bin/bash

INPUT_DIR="./datasets/right_arm.csv"
OUTPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"

echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"

java -jar javaproject-1.jar "$INPUT_DIR" "$OUTPUT_DIR"
