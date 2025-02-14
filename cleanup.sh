#!/bin/bash

export PULUMI_CONFIG_PASSPHRASE=""
cd pulumi/cert-manager
pulumi stack rm --force
cd ../k8ssandra-operator
pulumi stack rm --force
cd ../cassandra
pulumi stack rm --force
cd ../jaeger-java
pulumi stack rm --force
cd ../opentelemetry
pulumi stack rm --force
cd ../opentelemetry-cluster-and-collector
pulumi stack rm --force