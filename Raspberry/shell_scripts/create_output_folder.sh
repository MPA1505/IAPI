#!/bin/bash
OUTPUT_FOLDER=$1

echo "Ensuring the output folder exists: $OUTPUT_FOLDER"

if [ ! -d "$OUTPUT_FOLDER" ]; then
  echo "Output folder does not exist. Creating it..."
  mkdir -p "$OUTPUT_FOLDER"
  echo "Output folder created."
else
  echo "Output folder already exists."
fi
