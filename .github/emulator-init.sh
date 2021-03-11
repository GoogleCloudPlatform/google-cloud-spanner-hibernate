#!/bin/bash

# This runs the commands needed to initialize the Spanner emulator on a new container instance.
# Follows these steps: https://medium.com/google-cloud/cloud-spanner-emulator-bf12d141c12

gcloud config configurations create spanner-hibernate-emulator
gcloud config set auth/disable_credentials true
gcloud config set project cloud-spanner-hibernate-ci
gcloud config set api_endpoint_overrides/spanner

gcloud spanner instances create test-instance --config=spanner-hibernate-emulator --description="Test Instance" --nodes=1
gcloud spanner databases create test-database --instance test-instance
gcloud spanner databases create hibernate-sample-db --instance test-instance
