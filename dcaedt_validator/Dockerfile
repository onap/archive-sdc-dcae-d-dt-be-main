#
# Oracle Java 8 Dockerfile
#
#
# Image built by Thomas Nelson See Docker Github
# Pull base image.
FROM jdk-8u101_ubuntu:16.04

# Define commonly used ENV variables
#ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
#ENV PATH $PATH:$JAVA_HOME/bin:/opt/apache-tomcat-8.0.37/bin
ENV INSDIR /opt/app/validator
ENV LOGS ${INSDIR}/logs

RUN mkdir -p $INSDIR 

RUN mkdir ${INSDIR}/ssl && \
    mkdir $LOGS
COPY service/target/ASC-Validator-*.jar ${INSDIR}/ASC-Validator.jar
COPY service/target/classes/application.properties ${INSDIR}/
COPY configure-and-run.sh /usr/local/bin/
# Define working directory.
WORKDIR $INSDIR 
VOLUME $LOGS

# Define default command.
CMD ["/usr/local/bin/configure-and-run.sh"]

