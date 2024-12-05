#!/bin/bash

echo "Install script Version 0.2"

echo "Starting Minikube..."
minikube start
if [ $? -ne 0 ]; then
  echo "Failed to start Minikube"
  exit 1
fi

echo "Enabling metrics-server addon..."
minikube addons enable metrics-server
if [ $? -ne 0 ]; then
  echo "Failed to enable metrics-server addon"
  exit 1
fi

echo "Creating Flask deployment..."
minikube kubectl -- apply -f Flask/
if [ $? -ne 0 ]; then
  echo "Failed to create Flask deployment"
  exit 1
fi

echo "Creating Prometheus namespace..."
minikube kubectl -- create namespace prometheus
if [ $? -ne 0 ]; then
  echo "Failed to create Prometheus namespace"
  exit 1
fi

echo "Creating Prometheus cluster..."
minikube kubectl -- apply -f Prometheus/
if [ $? -ne 0 ]; then
  echo "Failed to create Prometheus cluster"
  exit 1
fi

echo "Creating Grafana namespace..."
minikube kubectl -- create namespace grafana
if [ $? -ne 0 ]; then
  echo "Failed to create Grafana namespace"
  exit 1
fi

echo "Creating Grafana cluster..."
minikube kubectl -- apply -f Grafana/
if [ $? -ne 0 ]; then
  echo "Failed to create Grafana cluster"
  exit 1
fi

echo "Creating Kafka namespace..."
minikube kubectl -- create namespace kafka
if [ $? -ne 0 ]; then
  echo "Failed to create Kafka namespace"
  exit 1
fi

echo "Creating Kafka cluster..."
minikube kubectl -- apply -f Kafka/
if [ $? -ne 0 ]; then
  echo "Failed to create Kafka cluster"
  exit 1
fi

echo "Creating Keycloak namespace..."
minikube kubectl -- create namespace keycloak
if [ $? -ne 0 ]; then
  echo "Failed to create Keycloak namespace"
  exit 1
fi

echo "Creating Keycloak cluster..."
minikube kubectl -- apply -f Keycloak/
if [ $? -ne 0 ]; then
  echo "Failed to create Keycloak cluster"
  exit 1
fi

echo "Creating MongoDB deployment..."
minikube kubectl -- apply -f MongoDB/
if [ $? -ne 0 ]; then
  echo "Failed to create MongoDB deployment"
  exit 1
fi

echo "Setup completed."
