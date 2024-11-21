#!/bin/bash
OUTPUT_FOLDER=$1
HEADER="ID,Timestamp,Actual Joint Positions,Actual Joint Velocities,Actual Joint Currents,Actual Cartesian Coordinates,Actual Tool Speed,Generalized Forces,Temperature of Each Joint,Execution Time,Safety Status,Tool Acceleration,Norm of Cartesian Linear Momentum,Robot Current,Joint Voltages,Elbow Position,Elbow Velocity,Tool Current,Tool Temperature,TCP Force,Anomaly State"

echo "Processing chunks in folder: $OUTPUT_FOLDER"

for chunk in "$OUTPUT_FOLDER"/dataset_part_*.csv; do
  echo "Processing chunk: $chunk"
  TEMP_FILE="${chunk}.tmp"
  
  echo "$HEADER" > "$TEMP_FILE"
  echo "Header added to $TEMP_FILE."

  awk '{ print 1 "," $0 }' "$chunk" >> "$TEMP_FILE"
  echo "Static ID column added to $TEMP_FILE."

  mv "$TEMP_FILE" "$chunk"
  echo "Chunk processed and saved: $chunk"
  echo "Finished processing file: $chunk"
done
