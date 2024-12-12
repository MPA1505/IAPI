import pandas as pd
import sys

def read_parquet_file(file_name):
    """Read a Parquet file and return the DataFrame."""
    df = pd.read_parquet(file_name)
    return df

if __name__ == "__main__":
    # Check if a file path has been provided
    if len(sys.argv) != 2:
        print("Usage: python3 readParquet.py path/to/file")
        sys.exit(1)

    file_to_read = sys.argv[1]  # Get the file path from command-line arguments
    try:
        data_frame = read_parquet_file(file_to_read)
        # Display the DataFrame
        print(data_frame)
    except Exception as e:
        print(f"Error reading the file: {e}")
