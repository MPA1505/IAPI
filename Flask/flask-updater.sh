#!/bin/bash

# Update the ConfigMap
minikube kubectl -- create configmap flask-app-code --from-file=app.py --from-file=requirements.txt --dry-run=client -o yaml | kubectl apply -f -

# Restart the Deployment
minikube kubectl -- scale deployment flask-app-deployment --replicas=0
minikube kubectl -- scale deployment flask-app-deployment --replicas=1

echo "ConfigMap updated and Deployment restarted successfully."