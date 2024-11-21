echo "Install script Version 0.1"
echo "Starting Minikube..."
minikube start

echo "Enabling metrics-server addon..."
minikube addons enable metrics-server

echo "Creating Kafka namespace..."
minikube kubectl -- create namespace kafka
echo "Creating Kafka cluster..."
minikube kubectl -- apply -f Kafka/

echo "Setup completed."
