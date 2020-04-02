#!/bin/bash 
SERVICE_URL=$1
PARTNER_ID=$2
ADMIN_SECRET=$3
PATH_TO_FILE=$4
ENTRY_ID_TO_UPDATE=''
if [ -n "$5" ];then
    ENTRY_ID_TO_UPDATE=$5
fi

#compile
javac -cp .:KalturaClient-3.3.1.jar:httpclient-4.2.3.jar:commons-httpclient-3.1.jar:log4j-1.2.15.jar:json-20090211.jar UploadTest.java

#run
java -cp .:KalturaClient-3.3.1.jar:httpclient-4.2.3.jar:commons-httpclient-3.1.jar:log4j-1.2.15.jar:json-20090211.jar:commons-codec-1.6.jar:commons-logging-1.1.1.jar UploadTest $SERVICE_URL $PARTNER_ID $ADMIN_SECRET $PATH_TO_FILE $ENTRY_ID_TO_UPDATE 
