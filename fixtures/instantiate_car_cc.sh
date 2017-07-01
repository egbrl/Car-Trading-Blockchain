#!/usr/bin/env bash

curl "http://localhost:8080/rest/setupclient";
curl "http://localhost:8080/rest/getconfig";
curl "http://localhost:8080/rest/enrolladmin";
curl "http://localhost:8080/rest/enrollusers";
curl "http://localhost:8080/rest/enrollorgadmin";
curl "http://localhost:8080/rest/constructchain";
curl "http://localhost:8080/rest/installchaincode";
curl "http://localhost:8080/rest/instantiatechaincode";