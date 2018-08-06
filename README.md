# Composition
Composition Refactoring

## Docker Build
```sh
$ docker login -u <user> -p <password> <docker_registry>
$ mkdir -p docker/target
$ cp dcaedt_be/target/dcae.war docker/target/dcae.war
$ cd docker
$ docker build --no-cache -t dcaed-be -f Dockerfile .
```
## Environment Configuration
The environment file must include the following new definition under "override_attributes":
  ``"SDC": {
        "BE":{
            "fqdn"      : "zldcrdm2sdc4cbe01.3f1a87.rdm2.tci.att.com"
        }
    },``
The environment json file should be injected into the docker by one of the following ways:
1. Place the json file under ```<host>:/data/environments```.
  Run the docker with volume:
  ```--volume /data/environments:/root/chef-solo/environments```
2. Use ``docker cp`` to copy the json file into the docker ```/root/chef-solo/environments```
## Docker Run
```sh
$ docker run -dit -p 8444:8444 --restart="always" --name=dcaed-be \
     --env ENVNAME=<environment_name> \
     --env JAVA_OPTIONS="-XX:MaxPermSize=256m -Xmx4713m \
           -Dconfig.home=\${JETTY_BASE}/config \
           -Dlog.home=/opt/logs/be \
           -Dlogging.config=\${JETTY_BASE}/config/dcae-be/logback-spring.xml" \
     [ --volume /data/environments:/root/chef-solo/environments ] \
     dcaed-be:latest \
     /bin/sh
```
