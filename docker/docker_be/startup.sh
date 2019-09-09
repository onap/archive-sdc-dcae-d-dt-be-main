#!/bin/sh

JAVA_OPTIONS=" ${JAVA_OPTIONS} -Dconfig.home=${JETTY_BASE}/config \
               -Dlog.home=${JETTY_BASE}/logs \
               -Dlogback.configurationFile=${JETTY_BASE}/dcae-be/logback-spring.xml
               -Djavax.net.ssl.trustStore=${JETTY_BASE}/etc/org.onap.sdc.trust.jks \
               -Djavax.net.ssl.trustStorePassword=].][xgtze]hBhz*wy]}m#lf* \
               -Djetty.console-capture.dir=${JETTY_BASE}/logs"

cd /root/chef-solo
chef-solo -c solo.rb -E ${ENVNAME}

status=$?
if [[ ${status} != 0 ]]; then
  echo "[ERROR] Problem detected while running chef. Aborting !"
  exit 1
fi

cd /var/lib/jetty
/docker-entrypoint.sh &

while true; do sleep 2; done
