from confluent_kafka import Producer
import json

kafka_config = {
    'bootstrap.servers': 'kafka:9092',
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
    record_key = f"key-{i}"
    record_value = json.dumps({'value': f"Message {i}"})
    producer.produce(
        'test-topic',
        key=record_key,
        value=record_value,
        callback=delivery_report
    )

producer.flush()
