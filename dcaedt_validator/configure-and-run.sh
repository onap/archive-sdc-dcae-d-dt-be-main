#!/bin/bash

LOGFILE=/opt/app/validator/logs/validator-`date +%Y%m%d`.log
echo "`date`:<--------------------    Starting     -------------------->" >> $LOGFILE
exec java -cp .:ASC-Validator.jar ${INTROSCOPE} ${INTRONAME} org.springframework.boot.loader.JarLauncher 2>&1 | tee -a $LOGFILE
#exec java -cp .:ASC-Validator-Service-0.0.1704-SNAPSHOT.jar org.springframework.boot.loader.JarLauncher 2>&1 | tee -a $LOGFILE

