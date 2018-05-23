#!/bin/sh
#set -x

# Run chef-solo for configuration
cd /var/opt/dcae-be/chef-solo
chef-solo -c solo.rb -E ${ENVNAME} --log_level "debug" --logfile "/tmp/Chef-Solo.log"

status=$?
if [ $status != 0 ]; then
  echo "[ERROR] Problem detected while running chef. Aborting !"
  exit 1
fi

# Execute Jetty
cd /var/lib/jetty
/docker-entrypoint.sh &

while true; do sleep 2; done
#exec "$@";
