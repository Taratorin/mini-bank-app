#!/bin/bash

set -e

./gradlew :bank-ui:bootJar :bank-gateway:bootJar :accounts-service:bootJar :cash-service:bootJar :transfer-service:bootJar :notifications-service:bootJar

docker build -t bank-ui:latest ./bank-ui
docker build -t bank-gateway:latest ./bank-gateway
docker build -t accounts-service:latest ./accounts-service
docker build -t cash-service:latest ./cash-service
docker build -t transfer-service:latest ./transfer-service
docker build -t notifications-service:latest ./notifications-service

kind load docker-image bank-ui:latest bank-gateway:latest accounts-service:latest cash-service:latest transfer-service:latest notifications-service:latest --name mini-bank

helm upgrade --install bank-app ./helm/bank-app

kubectl rollout restart deployment bank-app-accounts-service
kubectl rollout restart deployment bank-app-bank-gateway
kubectl rollout restart deployment bank-app-bank-ui
kubectl rollout restart deployment bank-app-cash-service
kubectl rollout restart deployment bank-app-transfer-service
kubectl rollout restart deployment bank-app-notifications-service

kubectl get pods