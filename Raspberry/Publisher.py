import os
import time
import json
import pyarrow.parquet as pq
from kafka import KafkaProducer

# Kafka configuration
kafka_broker = 'your_cloud_kafka_broker:9093'  # Replace with your Kafka broker address
topic = 'your_kafka_topic'  # Replace with your Kafka topic name

# SSL Configuration
ssl_config = {
    'security_protocol': 'SSL',
    'ssl_cafile': 'ca.crt',  # Path to the CA certificate
    'ssl_certfile': 'kafka.crt',  # Path to your client certificate
    'ssl_keyfile': 'kafka.key'  # Path to your private key
}

# Create a Kafka producer
producer = KafkaProducer(
    bootstrap_servers=kafka_broker,
    security_protocol=ssl_config['security_protocol'],
    ssl_cafile=ssl_config['ssl_cafile'],
    ssl_certfile=ssl_config['ssl_certfile'],
    ssl_keyfile=ssl_config['ssl_keyfile'],
    value_serializer=lambda v: json.dumps(v).encode('utf-8')
)

# Directory containing the Parquet files
parquet_directory = '/home/pi/iapi/IAPI/Raspberry/datasets/parquet_files/'

# Function to send a Parquet file to Kafka
def send_parquet_file(file_path):
    print(f'Reading {file_path}...')
    table = pq.read_table(file_path)  # Read the Parquet file
    data = table.to_pandas()  # Convert to a DataFrame

    # Send each row in the DataFrame to Kafka
    for index, row in data.iterrows():
        producer.send(topic, value=row.to_dict())
        print(f'Sent: {row.to_dict()}')
    
    producer.flush()  # Ensure all messages are sent

# Main loop to process Parquet files
if __name__ == "__main__":
    processed_files = set()  # To keep track of processed files

    while True:
        # List all Parquet files in the directory
        for filename in os.listdir(parquet_directory):
            if filename.endswith('.parquet'):
                full_path = os.path.join(parquet_directory, filename)
                
                # Check if the file has already been processed
                if full_path not in processed_files:
                    # Send the Parquet file to Kafka
                    send_parquet_file(full_path)

                    # Mark the file as processed
                    processed_files.add(full_path)

        # Sleep for a specified time before checking again
        time.sleep(10)  # Adjust the sleep duration as needed
