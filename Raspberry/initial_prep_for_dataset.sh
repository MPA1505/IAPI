#!/bin/bash

# Enable debugging
set -euo pipefail
trap 'echo "Error occurred at line $LINENO"; exit 1;' ERR

# Input file and output folder
INPUT_FILE="./datasets/right_arm.csv"
OUTPUT_FOLDER="./datasets/shell_datasets_20hz_1_robot_1_minute"
CHUNK_SIZE=1200  # Number of rows per chunk
SHELL_SCRIPTS_FOLDER="./shell_scripts"  # Path to the folder with shell scripts

# Debugging logs
echo "Starting the main script."
echo "Input file: $INPUT_FILE"
echo "Output folder: $OUTPUT_FOLDER"
echo "Chunk size: $CHUNK_SIZE"
echo "Using shell scripts from: $SHELL_SCRIPTS_FOLDER"

# Make all scripts in SHELL_SCRIPTS_FOLDER executable
echo "Ensuring all shell scripts in $SHELL_SCRIPTS_FOLDER are executable."
for script in "$SHELL_SCRIPTS_FOLDER"/*.sh; do
  if [ -f "$script" ]; then
    chmod +x "$script"
    echo "Made executable: $script"
  fi
done

# Step 1: Create the output folder
bash "$SHELL_SCRIPTS_FOLDER/create_output_folder.sh" "$OUTPUT_FOLDER"

# Step 2: Split the input file into chunks
bash "$SHELL_SCRIPTS_FOLDER/split_file.sh" "$INPUT_FILE" "$OUTPUT_FOLDER" "$CHUNK_SIZE"

# Step 3: Process each chunk
bash "$SHELL_SCRIPTS_FOLDER/process_chunks.sh" "$OUTPUT_FOLDER"

echo "All chunks processed successfully."
echo "Script execution completed."
