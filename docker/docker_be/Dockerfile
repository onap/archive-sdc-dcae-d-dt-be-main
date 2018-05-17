FROM onap/base_sdc-jetty:1.2.0-SNAPSHOT-latest

COPY chef-solo /var/opt/dcae-be/chef-solo/

COPY startup.sh /var/opt/dcae-be/

ADD target/dcae.war ${JETTY_BASE}/webapps/

USER root

COPY set_user.sh /tmp/set_user.sh

RUN sh -x /tmp/set_user.sh && rm -f /tmp/set_user.sh

RUN mkdir -p /opt/logs/be

RUN chown -R jetty:jetty ${JETTY_BASE}/webapps  /var/opt/dcae-be  /opt/logs  /var/lib/jetty

RUN chmod 770 /var/opt/dcae-be/startup.sh

EXPOSE 8082 8444

USER jetty

ENTRYPOINT [ "/var/opt/dcae-be/startup.sh" ]
