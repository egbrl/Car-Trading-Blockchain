# Blockchain Car Demo

`docker-compose.yaml` from [yeasy(v1.0)](https://github.com/yeasy/docker-compose-files/tree/master/hyperledger/1.0).

## Sample Instructions

```
docker-compose up
docker exec -it fabric-cli bash
root@cli# peer chaincode install -v 1.0 -n test_cc -p github.com/hyperledger/fabric/examples/chaincode/go/chaincode_example02
root@cli# peer chaincode instantiate -v 1.0 -n test_cc -c '{"Args":["init","a","100","b","200"]}' -o orderer0:7050
root@cli# peer chaincode query -n test_cc -c '{"Args":["query","a"]}'
root@cli# peer chaincode invoke -n test_cc -c '{"Args":["invoke","a","b","10"]}' -o orderer0:7050
```