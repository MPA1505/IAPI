import pandas as pd
import os
import logging

# Configure logging
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s')

# Read the CSV file in chunks due to its large size
chunk_size = 1200  # Number of rows per file
input_file = './Raspberry/datasets/Industrial_Robotic_Arm_IMU_Data_(CASPER_1_&_2)/right_arm.csv'
output_folder = './Raspberry/datasets/datasets_20hz_1_robot_1_minute'

logging.info('Starting the CSV chunk processing script.')
logging.debug(f'Input file: {input_file}')
logging.debug(f'Output folder: {output_folder}')
logging.debug(f'Chunk size: {chunk_size}')

# Ensure output directory exists
if not os.path.exists(output_folder):
    logging.debug('Output folder does not exist. Creating the folder.')
    os.makedirs(output_folder)

# Initialize variables for chunk processing
header = [
    'Timestamp', 'Actual Joint Positions', 'Actual Joint Velocities', 'Actual Joint Currents',
    'Actual Cartesian Coordinates', 'Actual Tool Speed', 'Generalized Forces', 
    'Temperature of Each Joint', 'Execution Time', 'Safety Status', 'Tool Acceleration',
    'Norm of Cartesian Linear Momentum', 'Robot Current', 'Joint Voltages', 'Elbow Position',
    'Elbow Velocity', 'Tool Current', 'Tool Temperature', 'TCP Force', 'Anomaly State'
]

# Process the CSV in chunks
chunk_counter = 1
global_id = 1  # Start the global ID counter

try:
    for chunk in pd.read_csv(input_file, header=0, names=header, chunksize=chunk_size):
        logging.info(f'Processing chunk {chunk_counter}')
        
        # Split lists into separate columns
        for column in header:
            logging.debug(f'Processing column: {column}')
            chunk[column] = chunk[column].apply(lambda x: eval(x) if isinstance(x, str) and x.startswith('[') else x)
        
        # Add an ID column as the first column
        chunk.insert(0, 'ID', global_id)
        
        # Save each chunk to a new CSV file
        output_file = os.path.join(output_folder, f'dataset_part_{chunk_counter}.csv')
        chunk.to_csv(output_file, index=False)
        logging.info(f'Saved chunk {chunk_counter} to {output_file}')
        
        chunk_counter += 1
except Exception as e:
    logging.error(f'An error occurred: {e}', exc_info=True)

logging.info('CSV chunk processing completed.')
