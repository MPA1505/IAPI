import pandas as pd
import sys

# Check for the argument
if len(sys.argv) < 2:
    print("Usage: python verify_parquet_file.py <path_to_parquet_file>")
    sys.exit(1)

# Read the file path from the command-line argument
file_path = sys.argv[1]

try:
    # Read the Parquet file
    df = pd.read_parquet(file_path)
    print("First 5 rows of the Parquet file:")
    print(df.head())

    # Check for missing values
    print("\nMissing values per column:")
    print(df.isnull().sum())

    # Validate column data types
    print("\nColumn data types:")
    print(df.dtypes)

except Exception as e:
    print(f"Error reading Parquet file: {e}")
