#!/usr/bin/env bash

IMG_VERSION=latest

echo "Building car app image"
docker build -t egabb/car_cc_app:$IMG_VERSION .
