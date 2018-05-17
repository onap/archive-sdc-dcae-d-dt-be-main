FROM onap/base_sdc-jetty:1.2.0-SNAPSHOT-latest

USER root

RUN adduser -h /home/dcae -s /bin/sh  -D dcae

COPY target/dcaedt_tools-*.jar /var/opt/dcae-tools/app/dcaedt_tools.jar

COPY chef-solo /var/opt/dcae-tools/chef-solo/

COPY startup.sh /var/opt/dcae-tools

RUN chmod 775 /var/opt/dcae-tools/startup.sh

RUN chown -R dcae /var/opt/dcae-tools

USER dcae

ENTRYPOINT [ "/var/opt/dcae-tools/startup.sh" ]

