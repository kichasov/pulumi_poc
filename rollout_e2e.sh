#!/bin/bash

export PULUMI_CONFIG_PASSPHRASE=""
echo Creating namespace robot-shop
kubectl create namespace robot-shop
echo Installing app
helm install app viadee/springboot-helm-chart -n robot-shop
cd pulumi/e2e
pulumi stack init -s dev --non-interactive
echo Installing infrastructure
pulumi up -y -f
echo Patching app
kubectl patch deploy app-springboot-helm-chart -n robot-shop -p '{"spec": {"template":{"metadata":{"annotations":{"instrumentation.opentelemetry.io/inject-java":"true"}}}} }'