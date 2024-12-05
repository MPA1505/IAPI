echo "Install script Version 0.2"
echo "Starting Minikube..."
minikube start

echo "Enabling metrics-server addon..."
minikube addons enable metrics-server

echo "Creating Flask deployment..."
minikube kubectl -- apply -f Flask/

echo "Creating Prometheus namespace..."
minikube kubectl -- create namespace prometheus
echo "Creating Prometheus cluster..."
minikube kubectl -- apply -f Prometheus/

echo "Creating Grafana namespace..."
minikube kubectl -- create namespace grafana
echo "Creating Grafana cluster..."
minikube kubectl -- apply -f Grafana/

echo "Creating Kafka namespace..."
minikube kubectl -- create namespace kafka
echo "Creating Kafka cluster..."
minikube kubectl -- apply -f Kafka/

echo "Creating Kecloak namespace..."
minikube kubectl -- create namespace keycloak
echo "Creating Keycloak cluster..."
minikube kubectl -- apply -f Keycloak/


echo "Creating MongoDB cluster..."
minikube kubectl -- apply -f MongoDB/


echo "Setup completed."
