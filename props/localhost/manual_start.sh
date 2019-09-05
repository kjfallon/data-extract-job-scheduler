#!/bin/sh

# load credentials if present in execution environment
if [ -r "/opt/extract-publisher/app_env_credentials.sh" ]; then
  . "/opt/extract-publisher/app_env_credentials.sh"
fi

java -Dloader.path=/opt/extract-publisher/ -jar /opt/extract-publisher/data-extract-job-scheduler-1.0.jar
