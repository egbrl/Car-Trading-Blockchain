# Blockchain Car Demo

## Env Setup

You may need to clean your docker env first (radical, hard-core way):
```
sudo systemctl stop docker
sudo rm -rf /var/lib/docker/
sudo systemctl start docker
```

Install the docker images with this script: [yeasy(download_images.sh)](https://raw.githubusercontent.com/yeasy/docker-compose-files/c984ad3f477795ff6beb7a5146aa28d48a329942/hyperledger/1.0/scripts/download_images.sh)

In the latest version where `IMG_VERSION=0.9.4` I encountered [this](https://github.com/yeasy/docker-compose-files/issues/48) error. So just use the link given above to download the exact version of the script where `IMG_VERSION=0.9.3` is used.

Then do `docker-compose up` from the project root folder.

## CC Development
To test if cc builds locally with most recent fabric-preview.:
```
local$    go get github.com/minio/minio-go/pkg/encrypt
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

## Run CC in Dockers
Use the fabric-cli to create a directory and deploy the cc:
```
local$    docker-compose up
local$    docker exec -it fabric-cli bash
root@cli# mkdir -p /go/src/github.com/EGabb/Car_Sharing_Blockchain
```

Copy the cc from the local machine (project root folder) into the container, install and execute:
```
local$    docker cp chaincode/ fabric-cli:/go/src/github.com/EGabb/Car_Sharing_Blockchain/
root@cli# go get github.com/minio/minio-go/pkg/encrypt
root@cli# peer chaincode install -v 1.0 -n car_cc -p github.com/EGabb/Car_Sharing_Blockchain/chaincode
root@cli# peer chaincode instantiate -v 1.0 -n car_cc -c '{"Args":["init", "999"]}'
root@cli# peer chaincode query -n car_cc -c '{"Args":["read", "abc"]}'
```

`Query Result: 999` your good to go. You can do other intersting stuff now, such as creating cars:
```
root@cli# peer chaincode invoke -n car_cc -c '{"Args":["create", "{\"vin\": \"WVW ZZZ 6RZ HY26 0780\"}", "{\"name\": \"amag\"}", "my-secret-key-00"]}'
```

If you are lucky you should see the keys of your new car flashing over the screen, congrats! See the test files for some more ideas or head over to the [specification](https://docs.google.com/document/d/1U7C9dJmDg_-l5gKeseZEKqc5ooru2wMxZ8BwhkbjIbk/edit?usp=sharing).

## References
Docker setup from [yeasy(v1.0)](https://github.com/yeasy/docker-compose-files/tree/master/hyperledger/1.0).
