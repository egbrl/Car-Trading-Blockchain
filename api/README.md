Getting Fabric
==============

If you don't have the rest of fabric, you can build from source (a bit tricky at 
the moment) or just pull the pre-built Docker images from (instructions are taken 
from http://hyperledger-fabric.readthedocs.io/en/latest/getting_started.html):


    mkdir fabric-docker
    cd fabric-docker
    curl -sS https://raw.githubusercontent.com/hyperledger/fabric/master/examples/e2e_cli/bootstrap.sh | bash 

If you do a "docker images", you should see the list of Docker images as given on
the getting started page.

Getting the Java SDK
====================

Pull the fabric-sdk-java repo:

    git clone git@github.com:hyperledger/fabric-sdk-java.git


at the moment, we are holding on the alpha2 tag:

    git checkout v1.0.0-alpha2

build the project and install it. This makes the SDK available to maven:

    mvn clean install

Launching the Fabric Network
============================

Launch Hyperledger network using the fabric configuration from the Java SDK integration tests:

    cd ~/hyperledger/fabric-sdk-java/src/test/fixture/sdkintegration
    ./fabric.sh restart


Start the project
=============================

Change to the directory where you cloned this repository

    mvn spring-boot:run


Creating and using chain code
=============================

This project uses the information contained in the End2endIT test from "org.hyperledger.fabric.sdkintegration". You'll need to restart the network often at the moment.

Do these operations from a clean network to make it work:

    curl "http://localhost:8080/rest/setupclient"
    curl "http://localhost:8080/rest/getconfig"
    curl "http://localhost:8080/rest/enrolladmin"
    curl "http://localhost:8080/rest/enrollusers"
    curl "http://localhost:8080/rest/enrollorgadmin"
    curl "http://localhost:8080/rest/constructchain"
    curl "http://localhost:8080/rest/installchaincode"
    curl "http://localhost:8080/rest/instantiatechaincode"

now you can do these as often as you like:

    curl "http://localhost:8080/rest/querya"
    curl "http://localhost:8080/rest/queryb"
    curl "http://localhost:8080/rest/move1fromatob"

If you want to get a dump of the chain:

    curl "http://localhost:8080/rest/dumpchain"

When you are finished:

    curl "http://localhost:8080/rest/destroychain"

If you want to get a list of the installed chain codes:

    curl "http://localhost:8080/rest/listchaincodes"


Restarting the client on an existing network
============================================

Do these operations from an existing network:

    curl "http://localhost:8080/rest/setupclient"
    curl "http://localhost:8080/rest/getconfig"
    curl "http://localhost:8080/rest/enrolladmin"
    curl "http://localhost:8080/rest/enrollusers"
    curl "http://localhost:8080/rest/enrollorgadmin"
    curl "http://localhost:8080/rest/getchain"

From this point on you should be able to continue as above.