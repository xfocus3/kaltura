#!/bin/bash 
PATH_TO_FOLDER=$1


#compile
javac -cp .:commons-codec-1.6.jar:commons-logging-1.1.1.jar:gson-2.8.6.jar:httpclient-4.2.3.jar:json-20090211.jar:KalturaClient-3.3.1.jar:kotlin-stdlib-1.2.60.jar:kotlin-stdlib-common-1.2.60.jar:log4j-api-2.13.1.jar:log4j-core-2.13.1.jar:mockwebserver-4.4.1.jar:okhttp-4.4.1.jar:okio-2.0.0.jar UploadTest.java

#run
java -cp .:commons-codec-1.6.jar:commons-logging-1.1.1.jar:gson-2.8.6.jar:httpclient-4.2.3.jar:json-20090211.jar:KalturaClient-3.3.1.jar:kotlin-stdlib-1.2.60.jar:kotlin-stdlib-common-1.2.60.jar:log4j-api-2.13.1.jar:log4j-core-2.13.1.jar:mockwebserver-4.4.1.jar:okhttp-4.4.1.jar:okio-2.0.0.jar UploadTest $PATH_TO_FOLDER
