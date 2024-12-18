#!/bin/bash

# Variables
MINIKUBE_PROFILE="minikube-kafka"
KAFKA_NAMESPACE="kafka"
DEPLOYMENT_FILE="Kafka/"
INPUT_DIR="./datasets/datasets_20hz_1_robot_1_minute"
OUTPUT_DIR="./datasets/merged_datasets/merged_dataset.parquet"
FILE_SIZE_MB="300"
JAR_FILE="unified_project-1.0-SNAPSHOT_fixed.jar"
BOOTSTRAP_SERVER="129.151.195.201"  # Public IP of the Oracle VM
KAFKA_PORT_INTERNAL=9093
KAFKA_PORT_EXTERNAL=9093  # Port to expose on the public IP

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

# Get Minikube IP
MINIKUBE_IP=$(minikube ip --profile "$MINIKUBE_PROFILE")
if [ -z "$MINIKUBE_IP" ]; then
    echo "Failed to get Minikube IP. Exiting."
    exit 1
fi
echo "Minikube IP: $MINIKUBE_IP"

# Add iptables rules to expose Kafka
echo "Adding iptables rules to expose Kafka on public IP..."
sudo iptables -t nat -A PREROUTING -p tcp --dport $KAFKA_PORT_EXTERNAL -j DNAT --to-destination "$MINIKUBE_IP:$KAFKA_PORT_INTERNAL"
sudo iptables -t nat -A POSTROUTING -p tcp -d "$MINIKUBE_IP" --dport $KAFKA_PORT_INTERNAL -j MASQUERADE

if [ $? -ne 0 ]; then
    echo "Failed to set up iptables rules. Exiting."
    exit 1
fi

# Verify iptables
echo "Verifying iptables rules..."
sudo iptables -t nat -L PREROUTING -n -v | grep "$KAFKA_PORT_EXTERNAL"

# Cleanup iptables rules (Optional: Uncomment if you want to clean up after script execution)
# echo "Cleaning up iptables rules..."
# sudo iptables -t nat -D PREROUTING -p tcp --dport $KAFKA_PORT_EXTERNAL -j DNAT --to-destination "$MINIKUBE_IP:$KAFKA_PORT_INTERNAL"
# sudo iptables -t nat -D POSTROUTING -p tcp -d "$MINIKUBE_IP" --dport $KAFKA_PORT_INTERNAL -j MASQUERADE

echo "Setup script completed."
