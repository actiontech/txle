#!/bin/bash

# Start utx server with the HA way.

if [ $# -lt 1 ];
then
  echo "USAGE: $0 [start|stop|restart] [--port serverport] [--rpc-port rpc port] [--metrics-port metrics port] [--config-dir config dir] [--log log name]"
  exit 1
fi

# Getting current file path. It is compatible to execute even if this file is not its path.
DEPLOY_DIR=$(cd ../`dirname $0`; pwd)



SERVER_NAME=Utx
CONF_DIR=""
LOG_FILE=""
SERVER_PORT=""
ONLY_PORT=""
RPC_PORT=""
METRICS_PORT=""
P_DEBUG=""

for p_name in $@; do
    val=`echo "$p_name" | sed -e 's;^--[^=]*=;;'`
    case $p_name in
	--config-dir=*) CONF_DIR="--config-dir=$val" ;;
	--log=*) LOG_FILE="--log=$val" ;;
	--debug) P_DEBUG="--debug" ;;
	--port=*) SERVER_PORT="--port=$val" ; ONLY_PORT="$val";;
	--rpc-port=*) RPC_PORT="--rpc-port=$val";;
	--metrics-port=*) METRICS_PORT="--metrics-port=$val";;
    esac
done

UTX_SERVICE_NAME=utx$ONLY_PORT.service

start(){
	UTX_SERVICE=/usr/lib/systemd/system/$UTX_SERVICE_NAME
	if [ -f "$UTX_SERVICE" ]; then
		# delete first and create again, the aim is to load new parameters.
		rm "$UTX_SERVICE"
	fi



	touch "$UTX_SERVICE"

	echo "[Unit]
	Description=utx$ONLY_PORT

	[Service]
	ExecStart=$DEPLOY_DIR/bin/utx-server.sh start $SERVER_PORT $RPC_PORT $METRICS_PORT $CONF_DIR $LOG_FILE $P_DEBUG
	ExecStop=$DEPLOY_DIR/bin/utx-server.sh stop $SERVER_PORT $RPC_PORT $METRICS_PORT $CONF_DIR $LOG_FILE $P_DEBUG --ha
	ExecReload=$DEPLOY_DIR/bin/utx-server.sh restart $SERVER_PORT $RPC_PORT $METRICS_PORT $CONF_DIR $LOG_FILE $P_DEBUG --ha
	PIDFile=$DEPLOY_DIR/conf/utx_server$ONLY_PORT.pid
	Restart=always

	[Install]
	WantedBy=multi-user.target" > $UTX_SERVICE




	cd /usr/lib/systemd/system

	systemctl daemon-reload

	systemctl start $UTX_SERVICE_NAME

	echo "Starting server in a highly available way."
	echo "Please pay more attention on this server after several seconds."
}

stop(){
	echo "It will take a while to stop, please wait patiently ...."

	# stop service in systemd
	systemctl stop $UTX_SERVICE_NAME

	echo "Successfully to stop server."
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