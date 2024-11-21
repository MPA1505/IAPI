import os
import shutil

def combine_files(folder_path, output_file, max_size_gb=3):
    try:
        # Define max size in bytes
        max_size_bytes = max_size_gb * 1024**3
        
        # Collect files and their sizes
        files = [(file, os.path.getsize(os.path.join(folder_path, file))) 
                 for file in os.listdir(folder_path) 
                 if os.path.isfile(os.path.join(folder_path, file))]
        
        # Sort files by name (or any other order you prefer)
        files.sort()

        # Initialize variables for combining files
        total_size = 0
        files_to_combine = []

        for file, size in files:
            if total_size + size <= max_size_bytes:
                files_to_combine.append(file)
                total_size += size
            else:
                break
        
        # Combine files
        with open(output_file, 'wb') as output:
            for file in files_to_combine:
                file_path = os.path.join(folder_path, file)
                with open(file_path, 'rb') as input_file:
                    shutil.copyfileobj(input_file, output)

        # Delete original files if no errors occurred
        #for file in files_to_combine:
            #os.remove(os.path.join(folder_path, file))

        print(f"Successfully combined {len(files_to_combine)} files into {output_file}.")
    except Exception as e:
        print(f"An error occurred: {e}")

# Specify the folder path and output file
folder_path = "./Raspberry/datasets/datasets_20hz_1_robot_1_minute"
output_file = "combined_file.csv"

# Run the script
combine_files(folder_path, output_file)
