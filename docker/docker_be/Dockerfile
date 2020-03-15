FROM onap/base_sdc-jetty:1.6.0

COPY --chown=jetty:jetty chef-solo ${JETTY_BASE}/chef-solo/

COPY --chown=jetty:jetty chef-repo/cookbooks ${JETTY_BASE}/chef-solo/cookbooks/

ADD --chown=jetty:jetty target/dcae.war ${JETTY_BASE}/webapps/

USER root

RUN apk add --no-cache python

USER jetty 

COPY --chown=jetty:jetty startup.sh ${JETTY_BASE}/

RUN chmod 770 ${JETTY_BASE}/startup.sh

ENTRYPOINT  ${JETTY_BASE}/startup.sh
