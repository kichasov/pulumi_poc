#!/bin/bash

export PULUMI_CONFIG_PASSPHRASE=""
cd pulumi/cert-manager
pulumi stack init -s dev --non-interactive
pulumi up -y
cd ../k8ssandra-operator
pulumi stack init -s dev --non-interactive
pulumi up -y
cd ../cassandra
pulumi stack init -s dev --non-interactive
pulumi up -y
kubectl wait --for=condition=CassandraInitialized K8ssandraCluster/cassandra-cluster -n k8ssandra-operator --timeout=300s
cd ../jaeger-java
pulumi stack init -s dev --non-interactive
pulumi up -y
kubectl create namespace robot-shop
helm install app viadee/springboot-helm-chart -n robot-shop
cd ../opentelemetry
pulumi stack init -s dev --non-interactive
pulumi up -y
cd ../opentelemetry-cluster-and-collector
pulumi stack init -s dev --non-interactive
pulumi up -y
kubectl patch deploy app-springboot-helm-chart -n robot-shop -p '{"spec": {"template":{"metadata":{"annotations":{"instrumentation.opentelemetry.io/inject-java":"true"}}}} }'