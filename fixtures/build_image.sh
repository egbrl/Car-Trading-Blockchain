#!/usr/bin/env bash

IMG_VERSION=latest

echo "Building car api image"
docker build -t egabb/car_cc_api:$IMG_VERSION .
