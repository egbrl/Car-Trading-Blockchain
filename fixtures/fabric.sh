#!/usr/bin/env bash
# simple batch script making it easier to cleanup and start a relatively fresh fabric env.

if [ ! -e "docker-compose.yaml" ];then
  echo "docker-compose.yaml not found."
  exit 8
fi

function ping_gw() {
  ping -q -w 1 -c 1 google.com > /dev/null && return 1 || return 0
}

function clean(){

  rm -rf /var/hyperledger/*

  if [ -e "/tmp/HFCSampletest.properties" ];then
    rm -f "/tmp/HFCSampletest.properties"
  fi

  lines=`docker ps -a | grep 'dev-peer' | wc -l`

  if [ "$lines" -gt 0 ]; then
    docker ps -a | grep 'dev-peer' | awk '{print $1}' | xargs docker rm -f
  fi

  lines=`docker images | grep 'dev-peer' | grep 'dev-peer' | wc -l`
  if [ "$lines" -gt 0 ]; then
    docker images | grep 'dev-peer' | awk '{print $1}' | xargs docker rmi -f
  fi

  lines=`docker ps -aq | wc -l`
  if [ "$lines" -gt 0 ]; then
    docker stop -f `docker ps -aq`
    docker rm -f `docker ps -aq`
  fi

  echo "Building car app image (if not latest already)"
  docker build -t egabb/car_cc_app:latest .

}

function up(){
  # docker-compose up --force-recreate
  docker-compose up
}

function down(){
  docker-compose down;
}

while ping_gw
do
  echo "No network, connect to ethernet or wireless first"
  echo "Then press any key to continue.."
  read cont
  echo "Reprobing connection.."
done

echo "Pulling latest blockchain demo.."
git pull

for opt in "$@"
do

    case "$opt" in
        up)
            up
            ;;
        down)
            down
            ;;
        clean)
            clean
            ;;
        restart)
            down
            clean
            up
            ;;

        *)
            echo $"Usage: $0 {up|down|clean|restart}"
            exit 1

esac
done
