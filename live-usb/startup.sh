#!/usr/bin/env bash
sleep 4
cd /home/carly/Car-Trading-Blockchain/fixtures

function ping_gw() {
  ping -q -w 1 -c 1 google.com > /dev/null && return 1 || return 0
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

./fabric.sh restart
