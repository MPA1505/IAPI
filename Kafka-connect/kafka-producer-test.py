from confluent_kafka import Producer
import json
import time
from datetime import datetime
import random

# Kafka configuration
kafka_config = {
    'bootstrap.servers': 'localhost:9093',
}

# Initialize the Kafka producer
producer = Producer(kafka_config)

# Message delivery reports
def delivery_report(err, msg):
    if err is not None:
        print(f"Delivery failed for record {msg.key()}: {err}")
    else:
        print(f"Record {msg.key()} successfully produced to {msg.topic()} [{msg.partition()}]")

# Produce messages
for i in range(10):
    record_key = f"robot-{i}"
    record_value = json.dumps({
        'timestamp': datetime.utcnow().isoformat(),
        'value': random.randint(1, 100),
        'robotId': random.randint(1, 3)
    })
    producer.produce(
        'test-topic',
        key=record_key,
        value=record_value,
        callback=delivery_report
    )
    time.sleep(1)

producer.flush()
