# Blockchain Car Demo

## CC Development
To test if cc builds locally with most recent fabric-preview.:
```
local$    go get github.com/hyperledger/fabric
local$    cd $GOPATH/src/github.com/hyperledger/fabric
local$    git checkout -b v1.0.0-preview origin/v1.0.0-preview
```

## CC Development
To see if the cc actually builds, clone this repo and execute from that folder:
```
local$    cd chaincode
local$    go build
```

To run all tests:
```
local$    go test
```

## Run CC
Use the fabric-cli to create a directory and deploy the cc:
```
local$    docker-compose up
local$    docker exec -it fabric-cli bash
root@cli# mkdir -p /go/src/github.com/EGabb/Car_Sharing_Blockchain/chaincode
```

Copy the cc from the local machine into the container, install and execute:
```
local$    docker cp chaincode.go fabric-cli:/go/src/github.com/EGabb/Car_Sharing_Blockchain/chaincode/
root@cli# peer chaincode install -v 1.0 -n car_demo_cc -p github.com/EGabb/Car_Sharing_Blockchain/chaincode/
root@cli# peer chaincode instantiate -v 1.0 -n car_demo_cc -c '{"Args":["init", "999"]}'
root@cli# peer chaincode query -n car_demo_cc -c '{"Args":["read", "abc"]}'
```

`Query Result: 999` your good to go.

## References
`docker-compose.yaml` is from [yeasy(v1.0)](https://github.com/yeasy/docker-compose-files/tree/master/hyperledger/1.0).
