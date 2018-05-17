#!/bin/sh
# Create user

USER="jetty"
GROUP="jetty"
UID="352070"
GID="35953"

# Remove user:
deluser ${USER}

delgroup ${GROUP}

echo "${USER}:x:${UID}:${GID}::/home/${USER}:Linux User,,,:/home/jetty:/bin/false" >> /etc/passwd

echo "${GROUP}:x:${GID}:${USER}" >> /etc/group

echo "${USER}:!:17501:0:99999:7:::" >> /etc/shadow

mkdir /home/${USER} && chown ${USER}:${GROUP} /home/${USER}