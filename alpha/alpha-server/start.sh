#!/bin/bash


# Java_HOME
JAVA_HOME=$(which java)
echo $JAVA_HOME

# main class
MAIN_CLASS=org.apache.servicecomb.saga.alpha.server.AlphaApplication

# cd `dirname $0`
BIN_DIR=`pwd`
echo $BIN_DIR

cd ..
DEPLOY_DIR=`pwd`

echo $DEPLOY_DIR
CONF_DIR=$DEPLOY_DIR/conf
LOG_DIR=$DEPLOY_DIR/log
LIB_DIR=$DEPLOY_DIR/lib

SERVER_NAME=Utx

STDOUT_FILE=$LOG_DIR/stdout.log

LOG_BACK="-Dlogback.configurationFile=conf/logback.xml"

param1=$1
echo $param1

param2=$2
echo $param2

start() {
    PIDS=`ps --no-heading -C java -f --width 1000 | grep "$CONF_DIR" | awk '{print $param2}'`
    if [ -n "$PIDS" ]; then
	echo "warning: $SERVER_NAME server has been started!"
	echo "PID: $PIDS"
	exit 1
    fi

    if [ ! -d $LOG_DIR ]; then
	mkdir $LOG_DIR
    fi

    # like: /dir/a.jar:/dir/b.jar...
    LIB_JARS=`ls $LIB_DIR |grep .jar |awk '{print "'$LIB_DIR'/"$0}' |tr "\n" ":"`

    for xml in $DEPLOY_DIR/*.xml
	do	LIB_JARS=$LIB_JARS:$xml
    done

    for env in $DEPLOY_DIR/*.properties
	do	LIB_JARS=$LIB_JARS:$env
    done

    JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "
    JAVA_DEBUG_OPTS=""
    if [ "$param2" = "debug" ]; then
	JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
    fi

    JAVA_JMX_OPTS=""

    JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn256m -XX:PermSize=128m  -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "

    echo "JAVA_MEM_OPTS=$JAVA_MEM_OPTS"
    echo -e "Starting the $SERVER_NAME server....\c"

    nohup java $JAVA_OPTS $JAVA_MEM_OPTS $LOG_BACK -classpath $DEPLOY_DIR:$LIB_DIR:$CONF_DIR:$LIB_JARS $MAIN_CLASS > /dev/null 2>>$STDOUT_FILE &
    

    COUNT=0
    while [ $COUNT -lt 1 ]; do
	echo -e ".\c"
	sleep 1

	COUNT=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" | awk '{print $param2}' | wc -l`

	if [ $COUNT -gt 0 ]; then
		break
	fi
    done

    echo "Start successfully!"
    PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" | awk '{print $param2}'`
    echo "PID: $PIDS"
}


stop(){
    PIDS=`ps --no-heading -C java -f --width 1000 | grep "$CONF_DIR" |awk '{print $param2}'`
    if [ -z "$PIDS" ]; then
	echo "ERROR: The $SERVER_NAME has not been started!"
    fi

    echo -e "Stopping the $SERVER_NAME ....\c"
    for PID in $PIDS ; do
	kill $PID > /dev/null 2>&1
    done

    COUNT=0
    while [ $COUNT -lt 1 ]; do
	echo -e ".\c"
	sleep 1
	COUNT=1
	for PID in $PIDS ; do
		PID_EXIST=`ps --no-heading -p $PID`
		if [ -n "$PID_EXIST" ]; then
			COUNT=0
			break
		fi
	done
    done
    echo "stop OK!"
    echo "PID: $PIDS"
}


case $param1 in
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



