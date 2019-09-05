FROM onap/base_sdc-jetty:1.4.1

COPY chef-solo /root/chef-solo/

COPY chef-repo/cookbooks /root/chef-solo/cookbooks/

ADD --chown=jetty:jetty target/dcaedt_tools-*.jar ${JETTY_BASE}/webapps/dcaedt_tools.jar

USER root

RUN apk add --no-cache python

COPY startup.sh /root/

RUN chmod 770 /root/startup.sh

ENTRYPOINT [ "/root/startup.sh" ]
