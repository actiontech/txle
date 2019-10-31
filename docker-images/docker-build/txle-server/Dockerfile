FROM centos/systemd

RUN yum install -y nc mysql which wget java && \
    yum clean all && rm -rf /var/cache/yum

RUN wget -P /opt https://github.com/actiontech/txle/releases/download/v2.19.10.0/actiontech-txle-2.19.10.0.tar.gz && \
    tar -xzf /opt/actiontech-txle-2.19.10.0.tar.gz -C /opt && \
    mv /opt/actiontech-txle-2.19.10.0 /opt/txle && \
    sed -i '/bootstrap.servers/ s/kafka1:9092,kafka2:9092,kafka3:9092/172.20.0.44:9092/g' /opt/txle/conf/kafka.properties &&\
    rm -f /opt/actiontech-txle-2.19.10.0.tar.gz

COPY quick-start/application.yaml /opt/txle/conf/application.yaml
COPY quick-start/wait-for-it.sh /opt/txle/bin/
COPY quick-start/docker_entrypoint.sh /opt/txle/bin/
RUN chmod 777 /opt/txle/bin/*

VOLUME /opt/txle/log/
EXPOSE 8090

CMD ["/opt/txle/bin/docker_entrypoint.sh"]
