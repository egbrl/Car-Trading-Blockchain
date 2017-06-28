FROM yeasy/hyperledger-fabric-peer:0.9.3

ENV DEBIAN_FRONTEND noninteractive
ENV EGABB_PROJECT_ROOT /go/src/github.com/EGabb/Car-Trading-Blockchain

EXPOSE 7051

RUN cd $FABRIC_ROOT/peer \
    && CGO_CFLAGS=" " go install -ldflags "$LD_FLAGS -linkmode external -extldflags '-static -lpthread'" \
    && go clean

RUN mkdir -p $EGABB_PROJECT_ROOT
RUN apt-get install npm -y

# it is necessary to rename the nodejs binary
# (see https://github.com/nodejs/node-v0.x-archive/issues/3911#issuecomment-8956154)
RUN ln -s /usr/bin/nodejs /usr/bin/node
RUN npm install -g nodemon

CMD ["peer","node","start"]

