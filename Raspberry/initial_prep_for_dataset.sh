#!/bin/bash

# Enable debugging
set -euo pipefail
trap 'echo "Error occurred at line $LINENO"; exit 1;' ERR

# Input file and output folder
INPUT_FILE="./Raspberry/datasets/right_arm.csv"
OUTPUT_FOLDER="./Raspberry/datasets/shell_datasets_20hz_1_robot_1_minute"
CHUNK_SIZE=1200  # Number of rows per chunk
HEADER="ID,Timestamp,Actual Joint Positions,Actual Joint Velocities,Actual Joint Currents,Actual Cartesian Coordinates,Actual Tool Speed,Generalized Forces,Temperature of Each Joint,Execution Time,Safety Status,Tool Acceleration,Norm of Cartesian Linear Momentum,Robot Current,Joint Voltages,Elbow Position,Elbow Velocity,Tool Current,Tool Temperature,TCP Force,Anomaly State"

# Debugging logs
echo "Starting the script."
echo "Input file: $INPUT_FILE"
echo "Output folder: $OUTPUT_FOLDER"
echo "Chunk size: $CHUNK_SIZE"

# Ensure output folder exists
if [ ! -d "$OUTPUT_FOLDER" ]; then
  echo "Output folder does not exist. Creating it..."
  mkdir -p "$OUTPUT_FOLDER"
  echo "Output folder created."
fi

# Check if input file exists
if [ ! -f "$INPUT_FILE" ]; then
  echo "Error: Input file does not exist at $INPUT_FILE."
  exit 1
fi

# Split the input file into chunks
echo "Splitting the input file into chunks..."
tail -n +2 "$INPUT_FILE" | split -l $CHUNK_SIZE - "$OUTPUT_FOLDER/dataset_part_" --additional-suffix=.csv
echo "File splitting completed."

# Process each chunk
echo "Processing chunks..."
for chunk in "$OUTPUT_FOLDER"/dataset_part_*.csv; do
  echo "Processing chunk: $chunk"
  TEMP_FILE="${chunk}.tmp"
  
  # Add header to the temporary file
  echo "$HEADER" > "$TEMP_FILE"
  echo "Header added to $TEMP_FILE."

  # Prepend static ID to each row
  awk '{
      print 1 "," $0  # Prepend the static ID value "1" to each row
    }' "$chunk" >> "$TEMP_FILE"
  echo "Static ID column added to $TEMP_FILE."

  # Replace the original file with the new one
  mv "$TEMP_FILE" "$chunk"
  echo "Chunk processed and saved: $chunk"
done

echo "All chunks processed successfully."
echo "CSV chunk processing completed."
