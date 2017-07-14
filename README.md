# Blockchain Car Demo

[![Build Status](https://travis-ci.org/EGabb/Car-Trading-Blockchain.svg?branch=master)](https://travis-ci.org/EGabb/Car-Trading-Blockchain)

## Env Setup

You may need to clean your docker env first (radical, hard-core way):
```
sudo systemctl stop docker
sudo rm -rf /var/lib/docker/
sudo systemctl start docker
```

Then type the following command from the project root:
```
cd fixtures
bash fabric.sh restart
```

This will assemble and install the docker image for the car API. It will then download the required fabric images and spin up the fabric network. The web app can be accessed on [http://localhost:8080](http://localhost:8080).

## Fetch Cars Directly on Peer(s)
To check out the bootstrapped car, log into the docker containers of `peer0` in `org1` and query the car:
```
local$           docker exec -it peer0.org1.example.com bash
root@peer0.org1# peer chaincode invoke -n car_cc_go -C foo -c '{"Args":["readCar", "garage", "garage", "WVWZZZ6RZHY260780"]}'
```

If you encounter problems, try a `docker rm $(docker ps -aq)` to remove all containers from time to time.

## CC Development
To test if cc builds locally with most recent fabric-preview.:
```
go get github.com/hyperledger/fabric
cd $GOPATH/src/github.com/hyperledger/fabric
git checkout -b v1.0.0-preview origin/v1.0.0-preview
```

To see if the cc actually builds, clone this repo and execute from that folder:
```
cd chaincode/src/github.com/car_cc/
go build
```

To run all tests:
```
go test
```

Or to run only some tests (TestTransferCar test in this case):
```
go test -run TestTransferCar
```

## Documentation
On [Google Drive](https://docs.google.com/document/d/1U7C9dJmDg_-l5gKeseZEKqc5ooru2wMxZ8BwhkbjIbk/edit?usp=sharing)

## References
* Docker setup from [yeasy(v1.0)](https://github.com/yeasy/docker-compose-files/tree/master/hyperledger/1.0).
* Spring template and inspiration: [https://bitbucket.org/isparkes/fabric-sdk-spring-boot-rest-poc/](https://bitbucket.org/isparkes/fabric-sdk-spring-boot-rest-poc/)
