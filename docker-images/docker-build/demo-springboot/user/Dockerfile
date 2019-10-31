FROM ubuntu:18.04

RUN apt-get update && \
    apt-get install -y netcat openjdk-8-jre-headless && \
    apt-get autoclean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY sample-txle-springboot-user-0.0.1-SNAPSHOT.jar /opt/
COPY docker_entrypoint.sh /opt/
COPY wait-for-it.sh /opt/
RUN chmod 777 /opt/docker_entrypoint.sh /opt/wait-for-it.sh

VOLUME /log/
EXPOSE 8002

CMD ["/opt/docker_entrypoint.sh"]
