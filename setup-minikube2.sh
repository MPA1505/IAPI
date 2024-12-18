#!/bin/bash

# Variables
MINIKUBE_PROFILE="minikube-kafka"
KAFKA_NAMESPACE="kafka"
DEPLOYMENT_FILE="Kafka/"
INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.parquet"
FILE_SIZE_MB="300"
JAR_FILE="unified_project-1.0-SNAPSHOT_fixed.jar"
BOOTSTRAP_SERVER="129.151.195.201"
TOPIC="test-topic3"
EXTERNAL_PORT=9093  # Default port

echo "Starting setup script..."

# Start Minikube
echo "Starting Minikube with profile $MINIKUBE_PROFILE..."
minikube start --profile "$MINIKUBE_PROFILE" --driver=docker --addons=ingress --listen-address=0.0.0.0
if [ $? -ne 0 ]; then
    echo "Failed to start Minikube. Exiting."
    exit 1
fi

# Create namespace if it doesn't exist
echo "Ensuring namespace $KAFKA_NAMESPACE exists..."
minikube kubectl -- get namespace "$KAFKA_NAMESPACE" >/dev/null 2>&1
if [ $? -ne 0 ]; then
    minikube kubectl -- create namespace "$KAFKA_NAMESPACE"
    if [ $? -ne 0 ]; then
        echo "Failed to create namespace $KAFKA_NAMESPACE. Exiting."
        exit 1
    fi
fi

# Deploy Kafka
echo "Deploying Kafka in namespace $KAFKA_NAMESPACE..."
minikube kubectl -- apply -f "$DEPLOYMENT_FILE" -n "$KAFKA_NAMESPACE"
if [ $? -ne 0 ]; then
    echo "Failed to deploy Kafka. Exiting."
    exit 1
fi

# Wait for Kafka pod to be ready
echo "Waiting for Kafka pod to be ready..."
minikube kubectl -- wait --for=condition=ready pod -l app=kafka -n "$KAFKA_NAMESPACE" --timeout=300s
if [ $? -ne 0 ]; then
    echo "Kafka pod is not ready. Exiting."
    exit 1
fi

# Check if the external port is already in use
echo "Checking if port $EXTERNAL_PORT is available..."
if lsof -i :$EXTERNAL_PORT >/dev/null; then
    echo "Port $EXTERNAL_PORT is already in use. Looking for an alternative port..."
    EXTERNAL_PORT=$(shuf -i 30000-40000 -n 1)
    echo "Using alternative port $EXTERNAL_PORT."
fi

# Expose Kafka service to the public IP
echo "Exposing Kafka service to public IP on port $EXTERNAL_PORT..."
minikube kubectl -- port-forward svc/kafka $EXTERNAL_PORT:9093 -n "$KAFKA_NAMESPACE" &
PORT_FORWARD_PID=$!

# Wait for port-forward to establish
sleep 5
if ! ps -p $PORT_FORWARD_PID > /dev/null; then
    echo "Failed to port-forward Kafka service. Exiting."
    exit 1
fi

echo "Kafka is now accessible at $BOOTSTRAP_SERVER:$EXTERNAL_PORT."

# Update Bootstrap Server with External Port
BOOTSTRAP_SERVER="$BOOTSTRAP_SERVER:$EXTERNAL_PORT"

echo "Setup script completed."
