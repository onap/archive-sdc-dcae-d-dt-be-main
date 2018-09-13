#!/bin/bash

#
# Constants:
#

WORKSPACE="${WORKSPACE:-}"
SUCCESS=0
FAILURE=1

RELEASE=latest
LOCAL=false

DEP_ENV="AUTO"


# Java Options:
DCAE_BE_JAVA_OPTIONS="-XX:MaxPermSize=256m -Xmx1024m -Dconfig.home=config -Dlog.home=/var/lib/jetty/logs/ -Dlogging.config=config/dcae-be/logback-spring.xml"
DCAE_FE_JAVA_OPTIONS="-XX:MaxPermSize=256m -Xmx1024m -Dconfig.home=config -Dlog.home=/var/lib/jetty/logs/ -Dlogging.config=config/dcae-fe/logback-spring.xml"
DCAE_DT_JAVA_OPTIONS="-XX:MaxPermSize=256m -Xmx1024m -Dconfig.home=config -Dlog.home=/var/lib/jetty/logs/ -Dlogging.config=config/dcae-dt/logback-spring.xml"
DCAE_TOSCA_JAVA_OPTIONS="-XX:MaxPermSize=256m -Xmx1024m"


#Define this as variable, so it can be excluded in run commands on Docker for OSX, as /etc/localtime cant be mounted there.
LOCAL_TIME_MOUNT_CMD="--volume /etc/localtime:/etc/localtime:ro"

# If os is OSX, unset this, so /etc/localtime is not mounted, otherwise leave it be
if [[ "$OSTYPE" == "darwin"* ]]; then
    LOCAL_TIME_MOUNT_CMD=""
fi

# Docker running Mode
DOCKER_RUN_MODE_BG="--detach"
DOCKER_RUN_MODE_FG="-dti"


#
# Functions:
#

function usage {
    echo "usage: dcae_docker_run.sh [ -r|--release <RELEASE-NAME> ]  [ -e|--environment <ENV-NAME> ] [ -p|--port <Docker-hub-port>] [ -l|--local <Run-without-pull>] [ -h|--help ]"
    echo "example: sudo bash dcae_docker_run.sh -e AUTO -r 1.2-STAGING-latest"
}
#


function cleanup {
    echo "Performing old dockers cleanup"

    if [ "$1" == "all" ] ; then
        docker_ids=$(docker ps -a | egrep "dcae" | awk '{print $1}')
        for X in ${docker_ids}
        do
           docker rm -f "${X}"
         done
    else
        echo "performing $1 docker cleanup"
        tmp=$(docker ps -a -q --filter="name=$1")
        if [[ -n "$tmp" ]]; then
            docker rm -f "${tmp}"
        fi
    fi
}
#


function dir_perms {
    mkdir -p "${WORKSPACE}/data/logs/DCAE-BE/DCAE"
    mkdir -p "${WORKSPACE}/data/logs/DCAE-FE/DCAE"
    mkdir -p "${WORKSPACE}/data/logs/DCAE-DT/DCAE"
    mkdir -p "${WORKSPACE}/data/logs/DCAE-TOSCA/DCAE"

    chmod -R 775 "${WORKSPACE}/data/logs"
}
#


function docker_logs {
    docker logs "$1" > "${WORKSPACE}/data/logs/docker_logs/$1_docker.log"
}
#


#
# Readiness Prob
#

function ready_probe {
    docker exec "$1" /var/lib/ready-probe.sh > /dev/null 2>&1
    rc=$?
    if [[ ${rc} == 0 ]]; then
        echo "DOCKER $1 start finished in $2 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#


function probe_docker {
    MATCH=$(docker logs --tail 30 "$1" | grep "DOCKER STARTED")
    echo MATCH is -- "${MATCH}"

    if [ -n "$MATCH" ] ; then
        echo "DOCKER start finished in $2 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#

function probe_dcae_tosca {
    health_check_http_code=$(curl -i -o /dev/null -w '%{http_code}' "http://${IP}:8085/healthcheck")
    if [[ "${health_check_http_code}" -eq 200 ]] ; then
        echo "DOCKER start finished in $1 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#

function probe_dcae_be {
    health_check_http_code=$(curl -i -o /dev/null -w '%{http_code}' "http://${IP}:8082/dcae/conf/composition")
    if [[ "${health_check_http_code}" -eq 200 ]] ; then
        echo "DOCKER start finished in $1 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#

function probe_dcae_fe {
    health_check_http_code=$(curl -i -o /dev/null -w '%{http_code}' "http://${IP}:8183/dcaed/healthCheck")
    if [[ "${health_check_http_code}" -eq 200 ]] ; then
        echo "DOCKER start finished in $1 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#

function probe_dcae_dt {
    health_check_http_code=$(curl -i -o /dev/null -w '%{http_code}' "http://${IP}:8186/dcae/healthCheckOld")
    if [[ "${health_check_http_code}" -eq 200 ]] ; then
        echo "DOCKER start finished in $1 seconds"
        return ${SUCCESS}
    fi
    return ${FAILURE}
}
#

# Not applicable for current release. Return Success in any case
function probe_dcae_tools {
   health_check_http_code=$(curl -i -o /dev/null -w '%{http_code}'  "http://${IP}:8082/dcae/getResourcesByMonitoringTemplateCategory")
    if [[ "${health_check_http_code}" -eq 200 ]] ; then
        echo "DOCKER start finished in $1 seconds"
        return ${SUCCESS}
    fi
    return ${SUCCESS}
}
#


function monitor_docker {
    DOCKER_NAME=$1
    echo "Monitor ${DOCKER_NAME} Docker"
    sleep 5
    TIME_OUT=900
    INTERVAL=20
    TIME=0

    while [ "$TIME" -lt "$TIME_OUT" ]; do

        case ${DOCKER_NAME} in

            dcae-tosca-app)
                probe_dcae_tosca ${TIME} ;
                status=$? ;
            ;;
            dcae-be)
                probe_dcae_be ${TIME} ;
                status=$? ;
            ;;
            dcae-fe)
                probe_dcae_fe ${TIME} ;
                status=$? ;
            ;;
            dcae-dt)
                probe_dcae_dt ${TIME} ;
                status=$? ;
            ;;
            dcae-tools)
                probe_dcae_tools ;
                status=$? ;
            ;;
            *)
                probe_docker "${DOCKER_NAME}" ${TIME};
                status=$? ;
            ;;

        esac

        if [ ${status} == ${SUCCESS} ] ; then
            break;
        fi

        echo "Sleep: ${INTERVAL} seconds before testing if ${DOCKER_NAME} DOCKER is up. Total wait time up now is: ${TIME} seconds. Timeout is: ${TIME_OUT} seconds"
        sleep ${INTERVAL}
        TIME=$(($TIME+$INTERVAL))
    done

    docker_logs "${DOCKER_NAME}"

    if [ "$TIME" -ge "$TIME_OUT" ]; then
        echo -e "\e[1;31mTIME OUT: DOCKER was NOT fully started in $TIME_OUT seconds... Could cause problems ...\e[0m"
    fi
}
#


function healthCheck {

    echo "BE health-Check:"
    curl --noproxy "*" "http://${IP}:8080/sdc2/rest/healthCheck"

    echo ""
    echo ""
    echo "FE health-Check:"
    curl --noproxy "*" "http://${IP}:8181/sdc1/rest/healthCheck"
}
#


function command_exit_status {
    status=$1
    docker=$2
    if [ "${status}" != "0" ] ; then
        echo "[  ERROR  ] Docker ${docker} run command exit with status [${status}]"
        exit ${FAILURE}
    fi
}
#


#
# Run Containers
#

# DCAE TOSCA
function dcae-tosca {
    DOCKER_NAME="dcae-tosca-app"
    echo "docker run ${DOCKER_NAME}..."
    if [ ${LOCAL} == false ]; then
        docker pull "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    fi
    docker run ${DOCKER_RUN_MODE_FG} --name ${DOCKER_NAME} --env HOST_IP="${IP}" --env ENVNAME="${DEP_ENV}" --env JAVA_OPTIONS="${DCAE_TOSCA_JAVA_OPTIONS}" --log-driver=json-file --log-opt max-size=100m --log-opt max-file=10 --ulimit memlock=-1:-1 --ulimit nofile=4096:100000 ${LOCAL_TIME_MOUNT_CMD}  --volume "${WORKSPACE}/data/logs/DCAE-TOSCA/:/var/lib/jetty/logs"  --publish 8085:8085  "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    command_exit_status $? ${DOCKER_NAME}
    echo "please wait while ${DOCKER_NAME^^} is starting....."
    monitor_docker ${DOCKER_NAME}
}
#


# DCAE BackEnd
function dcae-be {
    DOCKER_NAME="dcae-be"
    echo "docker run ${DOCKER_NAME}..."
    if [ ${LOCAL} == false ]; then
        docker pull "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    fi
    docker run ${DOCKER_RUN_MODE_FG} --name ${DOCKER_NAME} --env HOST_IP="${IP}" --env ENVNAME="${DEP_ENV}" --env JAVA_OPTIONS="${DCAE_BE_JAVA_OPTIONS}" --log-driver=json-file --log-opt max-size=100m --log-opt max-file=10 --ulimit memlock=-1:-1 --ulimit nofile=4096:100000 ${LOCAL_TIME_MOUNT_CMD}  --volume "${WORKSPACE}/data/logs/DCAE-BE/:/var/lib/jetty/logs" --volume "${WORKSPACE}/data/environments:/var/opt/dcae-be/chef-solo/environments" --publish 8444:8444 --publish 8082:8082 "${PREFIX}/${DOCKER_NAME}:${RELEASE}" /bin/sh
    command_exit_status $? ${DOCKER_NAME}
    echo "please wait while ${DOCKER_NAME^^} is starting....."
    monitor_docker ${DOCKER_NAME}
}
#


# DCAE Configuration
function dcae-tools {
    DOCKER_NAME="dcae-tools"
    echo "docker run ${DOCKER_NAME}..."
    if [ ${LOCAL} == false ]; then
        docker pull "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    fi
    docker run ${DOCKER_RUN_MODE_BG} --name ${DOCKER_NAME} --env HOST_IP="${IP}" --env ENVNAME="${DEP_ENV}" ${LOCAL_TIME_MOUNT_CMD}  --volume "${WORKSPACE}/data/logs/BE/:/var/lib/jetty/logs" --volume "${WORKSPACE}/data/environments:/var/opt/dcae-tools/chef-solo/environments"  "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    command_exit_status $? ${DOCKER_NAME}
    echo "please wait while ${DOCKER_NAME^^} is starting....."
    monitor_docker ${DOCKER_NAME}
}
#


# DCAE FrontEnd
function dcae-fe {
    DOCKER_NAME="dcae-fe"
    echo "docker run ${DOCKER_NAME}..."
    if [ ${LOCAL} == false ]; then
        docker pull "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    fi
    docker run ${DOCKER_RUN_MODE_FG} --name ${DOCKER_NAME} --env HOST_IP="${IP}" --env ENVNAME="${DEP_ENV}" --env JAVA_OPTIONS="${DCAE_FE_JAVA_OPTIONS}" --log-driver=json-file --log-opt max-size=100m --log-opt max-file=10 --ulimit memlock=-1:-1 --ulimit nofile=4096:100000 ${LOCAL_TIME_MOUNT_CMD}  --volume "${WORKSPACE}/data/logs/DCAE-FE/:/var/lib/jetty/logs" --volume "${WORKSPACE}/data/environments:/var/opt/dcae-fe/chef-solo/environments/" --publish 9444:9444 --publish 8183:8183 "${PREFIX}/${DOCKER_NAME}:${RELEASE}" /bin/sh
    command_exit_status $? ${DOCKER_NAME}
    echo "please wait while ${DOCKER_NAME^^} is starting....."
    monitor_docker ${DOCKER_NAME}
}
#

# DCAE DT
function dcae-dt {
    DOCKER_NAME="dcae-dt"
    echo "docker run ${DOCKER_NAME}..."
    if [ ${LOCAL} == false ]; then
        docker pull "${PREFIX}/${DOCKER_NAME}:${RELEASE}"
    fi
    docker run ${DOCKER_RUN_MODE_FG} --name ${DOCKER_NAME} --env HOST_IP="${IP}" --env ENVNAME="${DEP_ENV}" --env JAVA_OPTIONS="${DCAE_DT_JAVA_OPTIONS}" --log-driver=json-file --log-opt max-size=100m --log-opt max-file=10 --ulimit memlock=-1:-1 --ulimit nofile=4096:100000 ${LOCAL_TIME_MOUNT_CMD}  --volume "${WORKSPACE}/data/logs/DCAE-DT/:/var/lib/jetty/logs" --volume "${WORKSPACE}/data/environments:/var/opt/dcae-dt/chef-solo/environments/" --publish 9446:9446 --publish 8186:8186 "${PREFIX}/${DOCKER_NAME}:${RELEASE}" /bin/sh
    command_exit_status $? ${DOCKER_NAME}
    echo "please wait while ${DOCKER_NAME^^} is starting....."
    monitor_docker ${DOCKER_NAME}

}
#


#
# Main
#

# Handle command line arguments

if [ $# -eq 0 ]; then
    usage
    exit ${FAILURE}
fi

while [ $# -gt 0 ]; do
    case $1 in

    # -r | --release - The specific docker version to pull and deploy
    -r | --release )
          shift 1 ;
          RELEASE=$1;
          shift 1;;

    # -e | --environment - The environment name you want to deploy
    -e | --environment )
          shift 1;
          DEP_ENV=$1;
          shift 1 ;;

    # -p | --port - The port from which to connect to the docker nexus
    -p | --port )
          shift 1 ;
          PORT=$1;
          shift 1 ;;

    # -l | --local - Use this for deploying your local dockers without pulling them first
    -l | --local )
          LOCAL=true;
          shift 1;;

    # -d | --docker - The init specified docker
    -d | --docker )
          shift 1 ;
          DOCKER=$1;
          shift 1 ;;

    # -h | --help - Display the help message with all the available run options
    -h | --help )
          usage;
          exit  ${SUCCESS};;

    * )
          usage;
          exit  ${FAILURE};;
    esac
done


#Prefix those with WORKSPACE so it can be set to something other then /opt
[ -f "${WORKSPACE}/opt/config/env_name.txt" ] && DEP_ENV=$(cat "${WORKSPACE}/opt/config/env_name.txt") || echo "${DEP_ENV}"
[ -f "${WORKSPACE}/opt/config/nexus_username.txt" ] && NEXUS_USERNAME=$(cat "${WORKSPACE}/opt/config/nexus_username.txt")    || NEXUS_USERNAME="release"
[ -f "${WORKSPACE}/opt/config/nexus_password.txt" ] && NEXUS_PASSWD=$(cat "${WORKSPACE}/opt/config/nexus_password.txt")      || NEXUS_PASSWD="sfWU3DFVdBr7GVxB85mTYgAW"
[ -f "${WORKSPACE}/opt/config/nexus_docker_repo.txt" ] && NEXUS_DOCKER_REPO=$(cat "${WORKSPACE}/opt/config/nexus_docker_repo.txt") || NEXUS_DOCKER_REPO="nexus3.onap.org:${PORT}"
[ -f "${WORKSPACE}/opt/config/nexus_username.txt" ] && docker login -u ${NEXUS_USERNAME} -p ${NEXUS_PASSWD} ${NEXUS_DOCKER_REPO}



export IP=`ip route get 8.8.8.8 | awk '/src/{ print $7 }'`
#If OSX, then use this to get IP
if [[ "$OSTYPE" == "darwin"* ]]; then
    export IP=$(ipconfig getifaddr en0)
fi
export PREFIX=${NEXUS_DOCKER_REPO}'/onap'

if [ ${LOCAL} == true ]; then
    PREFIX='onap'
fi

echo ""

if [ -z "${DOCKER}" ]; then
    cleanup all
    dir_perms
    dcae-tosca
    dcae-be
    dcae-tools
    dcae-fe
    dcae-dt
    healthCheck
else
    cleanup "${DOCKER}"
    dir_perms
    ${DOCKER}
    healthCheck
fi
