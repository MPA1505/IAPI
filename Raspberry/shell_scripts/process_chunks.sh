#!/bin/bash
OUTPUT_FOLDER=$1
HEADER="ID,Timestamp,Actual Joint Positions,Actual Joint Velocities,Actual Joint Currents,Actual Cartesian Coordinates,Actual Tool Speed,Generalized Forces,Temperature of Each Joint,Execution Time,Safety Status,Tool Acceleration,Norm of Cartesian Linear Momentum,Robot Current,Joint Voltages,Elbow Position,Elbow Velocity,Tool Current,Tool Temperature,TCP Force,Anomaly State"

echo "Processing chunks in folder: $OUTPUT_FOLDER"

# Initialize a counter
file_counter=0

for chunk in "$OUTPUT_FOLDER"/dataset_part_*.csv; do
  TEMP_FILE="${chunk}.tmp"
  
  # Add the header to the temporary file
  echo "$HEADER" > "$TEMP_FILE"

  # Prepend static ID and append to the temporary file
  awk '{ print 1 "," $0 }' "$chunk" >> "$TEMP_FILE"

  # Replace the original file with the updated one
  mv "$TEMP_FILE" "$chunk"

  # Increment the counter
  ((file_counter++))

  # Print debugging info every 100 files
  if (( file_counter % 100 == 0 )); then
    echo "Processed $file_counter files so far..."
  fi
done

# Final message
echo "Finished processing all $file_counter files in folder: $OUTPUT_FOLDER."
