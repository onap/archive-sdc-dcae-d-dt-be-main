FROM java:8u40-jre
MAINTAINER Arthur Martella <am153x@att.com>
ENV insdir  /opt/app/catalog
RUN \
    mkdir -p ${insdir}/lib \
    && mkdir -p ${insdir}/etc \
    && mkdir -p ${insdir}/log \ 
    && useradd -d ${insdir} catalog \
    && chown -R catalog:catalog ${insdir}
WORKDIR ${insdir}
USER catalog
COPY target/ASC-Catalog*.jar ${insdir}/lib/ASC-Catalog.jar
#COPY target/deps/*.jar ${insdir}/lib/
COPY src/main/resources/log4j.properties ${insdir}/etc/log4j.properties
#COPY src/main/resources/config.json ${insdir}/etc/config.json
#COPY src/main/resources/cluster.xml ${insdir}/etc/cluster.xml
VOLUME ${insdir}/log
EXPOSE 7575
CMD ["/usr/bin/java","-jar","lib/ASC-Catalog.jar","--spring.profiles.active=default"]
