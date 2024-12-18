#!/bin/bash

# Variables
INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.parquet"
FILE_SIZE_MB="300"
JAR_FILE="unified_project-1.0-SNAPSHOT_fixed.jar"
BOOTSTRAP_SERVER="129.151.195.201:9093"
TOPIC="test-topic3"
MINIKUBE_PROFILE="minikube-kafka"
MINIKUBE_IP="$(minikube ip --profile $MINIKUBE_PROFILE)"
EXTERNAL_IP="$(curl -s ifconfig.me)"  # Get the public IP of the VM

# Display variables
echo "Input folder: $INPUT_DIR"
echo "Output folder: $OUTPUT_DIR"
echo "File size: $FILE_SIZE_MB"
echo "JAR file: $JAR_FILE"
echo "Bootstrap server: $BOOTSTRAP_SERVER"
echo "Topic: $TOPIC"

# Step 1: Start Minikube
echo "Starting Minikube with profile: $MINIKUBE_PROFILE..."
minikube start --driver=docker --profile=$MINIKUBE_PROFILE --addons=ingress --listen-address=0.0.0.0 || {
  echo "Failed to start Minikube";
  exit 1;
}
echo "Minikube started."

# Step 2: Deploy Kafka
echo "Creating Kafka namespace..."
minikube kubectl -- create namespace kafka || {
  echo "Failed to create Kafka namespace";
  exit 1;
}
echo "Deploying Kafka to Minikube..."
minikube kubectl -- apply -f /Kafka || {
  echo "Failed to deploy Kafka";
  exit 1;
}

# Wait for Kafka deployment to be ready
echo "Waiting for Kafka pods to be ready..."
minikube kubectl -- wait --for=condition=ready pod -l app=kafka --timeout=300s

echo "Kafka deployed successfully."

# Step 3: Expose Kafka port externally
KAFKA_SERVICE_NAME="kafka-service"
KAFKA_EXTERNAL_PORT=9093
minikube kubectl -- expose deployment kafka --type=LoadBalancer --name=$KAFKA_SERVICE_NAME || {
  echo "Failed to expose Kafka service";
  exit 1;
}

# Get Minikube service IP
echo "Configuring Kafka external access..."
minikube kubectl -- patch svc $KAFKA_SERVICE_NAME -p '{"spec": {"type": "NodePort"}}'
KAFKA_NODE_PORT=$(kubectl get svc $KAFKA_SERVICE_NAME -o=jsonpath='{.spec.ports[0].nodePort}')

iptables -A PREROUTING -t nat -i eth0 -p tcp --dport $KAFKA_NODE_PORT -j REDIRECT --to-port $KAFKA_EXTERNAL_PORT
iptables -A FORWARD -p tcp -d $MINIKUBE_IP --dport $KAFKA_EXTERNAL_PORT -j ACCEPT