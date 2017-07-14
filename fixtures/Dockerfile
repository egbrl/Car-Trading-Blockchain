FROM ubuntu:16.04

ENV DEBIAN_FRONTEND noninteractive

ENV EGABB_PROJECT_ROOT /var/egabb/

RUN mkdir -p $EGABB_PROJECT_ROOT/app
RUN mkdir -p $EGABB_PROJECT_ROOT/chaincode
RUN mkdir -p $EGABB_PROJECT_ROOT/fixtures

RUN apt-get update
RUN apt-get install maven -y
RUN apt-get install openjdk-8-jdk -y

EXPOSE 8080

RUN cd $EGABB_PROJECT_ROOT/app

CMD ["sleep", "infinity"]
