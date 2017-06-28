# Blockchain Car Demo

[![Build Status](https://travis-ci.org/EGabb/Car-Trading-Blockchain.svg?branch=master)](https://travis-ci.org/EGabb/Car-Trading-Blockchain)

## Env Setup

You may need to clean your docker env first (radical, hard-core way):
```
sudo systemctl stop docker
sudo rm -rf /var/lib/docker/
sudo systemctl start docker
```

Then type `bash download_images.sh` from the project root to assemble and install the docker images with the script provided. You should have a clean setup now. If you encounter problems, try a `docker rm $(docker ps -aq)` to remove all containers from time to time.

## CC Development
To test if cc builds locally with most recent fabric-preview.:
```
local$    go get github.com/hyperledger/fabric
local$    cd $GOPATH/src/github.com/hyperledger/fabric
local$    git checkout -b v1.0.0-preview origin/v1.0.0-preview
```

To see if the cc actually builds, clone this repo and execute from that folder:
```
local$    cd chaincode
local$    go build
```

To run all tests:
```
local$    go test
```

Or to run only some tests (car tests in this case):
```
local$    go test -run car
```

## Run CC in Dockers
The cc in the `./chaincode` directory is mounted in the `fabric-cli` container. To install and execute the cc do:
```
local$    docker-compose up
local$    docker exec -it fabric-cli bash
root@cli# peer chaincode install -v 1.0 -n car_cc -p github.com/EGabb/Car-Trading-Blockchain/chaincode
root@cli# peer chaincode instantiate -v 1.0 -n car_cc -c '{"Args":["init", "999"]}'
```

You can do other intersting stuff now, such as creating cars:
```
root@cli# peer chaincode invoke -n car_cc -c '{"Args":["create", "amag", "garage", "{\"vin\": \"WVW ZZZ 6RZ HY26 0780\"}"]}'
```

If you are lucky you should see your new car flashing over the screen, congrats! See the test files for some more ideas or head over to the [specification](https://docs.google.com/document/d/1U7C9dJmDg_-l5gKeseZEKqc5ooru2wMxZ8BwhkbjIbk/edit?usp=sharing).

## References
Docker setup from [yeasy(v1.0)](https://github.com/yeasy/docker-compose-files/tree/master/hyperledger/1.0).
