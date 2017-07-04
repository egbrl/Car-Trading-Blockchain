#!/usr/bin/env bash

CAR_API_CONTAINER_NAME=car_cc_api
DOCKER_API_IP=127.0.0.1

curl http://$DOCKER_API_IP:8080/rest/setupclient;
curl http://$DOCKER_API_IP:8080/rest/getconfig;
curl http://$DOCKER_API_IP:8080/rest/enrolladmin;
curl http://$DOCKER_API_IP:8080/rest/enrollusers;
curl http://$DOCKER_API_IP:8080/rest/enrollorgadmin;
curl http://$DOCKER_API_IP:8080/rest/constructchain;
curl http://$DOCKER_API_IP:8080/rest/installchaincode;
curl http://$DOCKER_API_IP:8080/rest/instantiatechaincode;