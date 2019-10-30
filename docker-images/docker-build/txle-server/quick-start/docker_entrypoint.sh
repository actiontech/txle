#!/usr/bin/env bash

/opt/txle/bin/wait-for-it.sh 172.20.0.11:3306

mysql -h172.20.0.11 -P3306 -uroot -p123456 -e "create database if not exists db1"
mysql -h172.20.0.11 -P3306 -uroot -p123456 -e "create database if not exists db2"
mysql -h172.20.0.11 -P3306 -uroot -p123456 -e "create database if not exists db3"

/opt/txle/bin/txle start

/bin/bash
