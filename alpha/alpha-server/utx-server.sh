#!/bin/bash

if [ $# -lt 1 ];
then
  echo "USAGE: $0 [start|stop|restart] [--port serverport] [--rpc-port rpc port] [--metrics-port metrics port] [--config-dir config dir] [--log log name]"
  exit 1
fi

# Java_HOME
JAVA_HOME=$(which java)
echo $JAVA_HOME

# ensure the 'bashrc' file can be loaded in using 'systemd' case.
source ~/.bashrc

# main class
MAIN_CLASS=org.apache.servicecomb.saga.alpha.server.AlphaApplication

# Getting current file path. It is compatible to execute even if this file is not its path.
BIN_DIR=$(cd `dirname $0`; pwd)

cd $BIN_DIR
cd ..

DEPLOY_DIR=`pwd`

SERVER_NAME=Utx
LIB_DIR=$DEPLOY_DIR/lib
CONF_DIR=$DEPLOY_DIR/conf
LOG_FILE="$DEPLOY_DIR/log/stdout.log"
SERVER_PORT=""
ONLY_PORT=""
RPC_PORT=""
METRICS_PORT=""
P_HA=""

for p_name in $@; do
    val=`echo "$p_name" | sed -e 's;^--[^=]*=;;'`
    case $p_name in
        --config-dir=*) CONF_DIR=$val ;;
        --log=*) LOG_FILE=$val ;;
        --debug) P_DEBUG="true" ;;
        --port=*) SERVER_PORT="-Dserver.port=$val" ; ONLY_PORT="$val";;
        --rpc-port=*) RPC_PORT="-Dalpha.server.port=$val";;
        --metrics-port=*) METRICS_PORT="-Dutx.prometheus.metrics.port=$val";;
        --ha) P_HA="true" ;;
    esac
done

UTXPIDFILE=$CONF_DIR/utx_server$ONLY_PORT.pid

#. "$CONF_DIR/server.cfg"
#SERVER_PORT=""
#LOG_FILE="$DEPLOY_DIR/log/stdout.log"
#
#if [ -n "$port" ]; then
#    SERVER_PORT="-Dserver.port=$port"
#fi
#if [ -n "$log_url" ]; then
#    LOG_FILE="$log_url"
#fi

start() {
    if [ ! -f "$UTXPIDFILE" ]; then
	touch "$UTXPIDFILE"
    else
	PID=`cat "$UTXPIDFILE"`
	SERVER_STATUS=`ps -ef|grep -w "$PID" |grep -v "grep" |wc -l`
	if [ $SERVER_STATUS -gt 0 ]; then
		echo "warning: $SERVER_NAME server has been started!"
		echo "PID: $PID"
		exit 1
	else
		# PIDFILE is existent, but it is not alive.
		rm "$UTXPIDFILE"
	fi
    fi

    if [ ! -f $LOG_FILE ]; then
	mkdir -p $LOG_FILE
	rmdir $LOG_FILE
	touch $LOG_FILE
    fi

    # like: /dir/a.jar:/dir/b.jar...
    LIB_JARS=`ls $LIB_DIR |grep .jar |awk '{print "'$LIB_DIR'/"$0}' |tr "\n" ":"`
    for xml in $CONF_DIR/*.xml
	do	LIB_JARS=$LIB_JARS:$xml
    done
    for env in $CONF_DIR/*.properties
	do	LIB_JARS=$LIB_JARS:$env
    done

    if [ -n "$P_CONFIG" ]; then
	LIB_JARS=$LIB_JARS:$P_CONFIG
    else
	for yml in $CONF_DIR/*.yaml
		do     LIB_JARS=$LIB_JARS:$yml
	done
    fi

    JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "
    JAVA_DEBUG_OPTS=""
    if [ "$P_DEBUG" = "true" ]; then
	JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
    fi

    JAVA_JMX_OPTS=""

    JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn256m -XX:PermSize=128m  -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "

    echo -e "Starting the $SERVER_NAME server...."
    echo "JAVA_MEM_OPTS=$JAVA_MEM_OPTS $SERVER_PORT $RPC_PORT $METRICS_PORT"

    nohup java $JAVA_OPTS $JAVA_MEM_OPTS $SERVER_PORT $RPC_PORT $METRICS_PORT -classpath $DEPLOY_DIR:$LIB_DIR:$CONF_DIR:$LIB_JARS $MAIN_CLASS > /dev/null 2>>$LOG_FILE &
    
    if [ $? -eq 0 ]; then
	echo $! > "$UTXPIDFILE"
    fi

    echo "Start successfully!"
    echo "PID: `cat "$UTXPIDFILE"`"
}


stop(){
    if [ ! -f "$UTXPIDFILE" ]; then
	echo "ERROR: The $SERVER_NAME has not been started!"
	exit 0
    fi

    PID=`cat "$UTXPIDFILE"`
    SERVER_STATUS=`ps -ef|grep -w "$PID" |grep -v "grep" |wc -l`
    if [ $SERVER_STATUS -eq 0 ]; then
	# PIDFILE is existent, but it is not alive.
	rm "$UTXPIDFILE"
	echo "ERROR: The $SERVER_NAME has not been started!"
	exit 0
    fi

    echo -e "Stopping the $SERVER_NAME server ....\c"

    kill "$PID" > /dev/null 2>&1
    rm "$UTXPIDFILE"

    while :
    do
	echo -e "."
	PID_EXIST=`ps --no-heading -p $PID`
	if [ -n "$PID_EXIST" ]; then
		break
	fi
	sleep 1
    done

    if [ "$P_HA" != "true" ]; then
        # stop service in systemd. it will give a mistake 'Job for utx.service canceled.' out when use the command 'systemctl restart utx.service'.
        systemctl stop utx$ONLY_PORT.service
    fi

    echo "stop OK!"
    echo "PID: $PID"
}


case $1 in
	start)
		start;
	;;

	stop)
		stop;
	;;

	restart)
		echo "$SERVER_NAME server is restarting ...."
		stop;
		sleep 1;
		start;
	;;

	*)
		echo "Usage: start.sh {start|stop|restart}"
	;;
esac
exit 0
