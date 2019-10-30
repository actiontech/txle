#!/usr/bin/env bash

/opt/wait-for-it.sh 172.20.0.11:3306
/opt/wait-for-it.sh 172.20.0.21:8080

java -jar /opt/sample-txle-springboot-user-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &

/bin/bash
