#!/bin/sh
set -x 
# Run chef-solo for configuration
cd /var/opt/dcae-tools/chef-solo
chef-solo -c solo.rb -E ${ENVNAME} --log_level "debug" --logfile "/tmp/Chef-Solo.log"

status=$?
if [ $status != 0 ]; then
  echo "[ERROR] Problem detected while running chef. Aborting !"
  exit 1
fi

# Execute DCAE tools
cd /var/opt/dcae-tools/app
java -jar dcaedt_tools.jar conf/environment.json conf/config.json

exec "$@";