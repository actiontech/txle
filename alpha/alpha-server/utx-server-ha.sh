#!/bin/bash

# Start utx server with the HA way.

if [ $# -lt 1 ];
then
  echo "USAGE: $0 [start|stop|restart] [--port serverport] [--config-dir config dir] [--log log name]"
  exit 1
fi

# Getting current file path. It is compatible to execute even if this file is not its path.
DEPLOY_DIR=$(cd ../`dirname $0`; pwd)



SERVER_NAME=Utx
CONF_DIR=""
LOG_FILE=""
SERVER_PORT=""
P_DEBUG=""

for p_name in $@; do
    val=`echo "$p_name" | sed -e 's;^--[^=]*=;;'`
    case $p_name in
	--config-dir=*) CONF_DIR="--config-dir=$val" ;;
	--log=*) LOG_FILE="--log=$val" ;;
	--debug) P_DEBUG="--debug" ;;
	--port=*) SERVER_PORT="--port=$val" ;;
    esac
done



start(){
	UTX_SERVICE=/usr/lib/systemd/system/utx.service
	if [ -f "$UTX_SERVICE" ]; then
		# delete first and create again, the aim is load new parameters.
		rm "$UTX_SERVICE"
	fi



	touch "$UTX_SERVICE"

	echo "[Unit]
	Description=utx

	[Service]
	ExecStart=$DEPLOY_DIR/bin/utx-server.sh start $SERVER_PORT $CONF_DIR $LOG_FILE $P_DEBUG
	ExecStop=$DEPLOY_DIR/bin/utx-server.sh stop --ha
	ExecReload=$DEPLOY_DIR/bin/utx-server.sh restart $SERVER_PORT $CONF_DIR $LOG_FILE $P_DEBUG --ha
	PIDFile=$DEPLOY_DIR/conf/utx_server.pid
	Restart=always

	[Install]
	WantedBy=multi-user.target" > $UTX_SERVICE




	cd /usr/lib/systemd/system

	systemctl daemon-reload

	systemctl start utx

	echo "Starting server in a highly available way."
	echo "Please pay more attention on this server after several seconds."
}

stop(){
	echo "It will take a while to stop, please wait patiently ...."

	# stop service in systemd
	systemctl stop utx.service

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