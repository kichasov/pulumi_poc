#!/bin/bash

export PULUMI_CONFIG_PASSPHRASE=""
cd pulumi/e2e
pulumi stack rm --force