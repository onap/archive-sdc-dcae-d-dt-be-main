#!/bin/sh

JAVA_OPTIONS=" ${JAVA_OPTIONS} -Dconfig.home=${JETTY_BASE}/config \
               -Dlog.home=${JETTY_BASE}/logs \
               -Djetty.console-capture.dir=${JETTY_BASE}/logs \
               -Djavax.net.ssl.trustStore=${JETTY_BASE}/etc/org.onap.sdc.trust.jks \
               -Djavax.net.ssl.trustStorePassword=].][xgtze]hBhz*wy]}m#lf*"

cd /var/lib/jetty/chef-solo
chef-solo -c solo.rb -E ${ENVNAME}

status=$?
if [[ ${status} != 0 ]]; then
  echo "[ERROR] Problem detected while running chef. Aborting !"
  exit 1
fi

cd ${JETTY_BASE}/webapps
java ${JAVA_OPTIONS} -jar dcaedt_tools.jar ../conf/environment.json ../conf/config.json

exec "$@";
