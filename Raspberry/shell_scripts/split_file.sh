#!/bin/bash
INPUT_FILE=$1
OUTPUT_FOLDER=$2
CHUNK_SIZE=$3

echo "Splitting the input file: $INPUT_FILE into chunks of $CHUNK_SIZE rows."
echo "Output folder: $OUTPUT_FOLDER"

if [ ! -f "$INPUT_FILE" ]; then
  echo "Error: Input file does not exist at $INPUT_FILE."
  exit 1
fi

# Perform the split operation with numeric suffixes
split -l "$CHUNK_SIZE" --numeric-suffixes=1 --suffix-length=4 "$INPUT_FILE" "$OUTPUT_FOLDER/dataset_part_"

# Rename split files to have a .csv extension and echo the file name
echo "Renaming split files and adding .csv extension..."
for file in "$OUTPUT_FOLDER"/dataset_part_*; do
  mv "$file" "$file.csv"
  echo "Created split file: $file.csv"
done

echo "File splitting completed."
