/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.session.model.NewSession;
import com.ecwid.consul.v1.session.model.Session;
import org.apache.servicecomb.saga.common.CrossSystemInetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.ConsulProperties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY;
import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY_VALUE;

/**
 * @author Gannalyo
 * @since 2019/11/18
 */
@ConditionalOnConsulEnabled
@AutoConfigureAfter(ConsulProperties.class)
public class TxleConsulClient implements ApplicationRunner {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired(required = false)
    private ConsulProperties consulProperties;
    private ConsulClient consulClient;

    @Value("${spring.application.name:\"\"}")
    private String serverName;

    @Value("${server.port:8090}")
    private int serverPort;

    @Value("${spring.cloud.consul.enabled:false}")
    private boolean enabled;

    @Value("${spring.cloud.consul.servers:}")
    private String consulServers;

    @Value("${spring.cloud.consul.discovery.instanceId:\"\"}")
    private String consulInstanceId;

    private static String consulSessionId;
    private boolean isMaster;

    private final Map<String, ConsulClient> consulClientMap = new HashMap<>(3);

    public ConsulClient getConsulClient() {
        return consulClient;
    }

    private void initConsulCluster() {
        try {
            if (consulServers != null && consulServers.length() > 0) {
                for (String hostPorts : consulServers.split(",")) {
                    String[] hostPort = hostPorts.trim().split(":");
                    consulClientMap.put(hostPorts, new ConsulClient(hostPort[0], Integer.parseInt(hostPort[1])));
                }
            }
        } catch (Exception e) {
            // It's not a strong dependency to Consul.
            log.error("Could not connect to Consul, servers = [{}].", consulServers, e);
        }
    }

    public void setAvailableConsulClient() {
        if (consulClient != null) {
            return;
        }
        for (Map.Entry<String, ConsulClient> entry : consulClientMap.entrySet()) {
            try {
                if (entry.getValue().getStatusLeader().getValue() != null) {
                    String[] hostPort = entry.getKey().split(":");
                    consulProperties.setHost(hostPort[0]);
                    consulProperties.setPort(Integer.parseInt(hostPort[1]));
                    consulClient = entry.getValue();
                    break;
                }
            } catch (Exception e) {
                log.error("Occur an error when executing method 'setAvailableConsulClient'.", e);
                continue;
            }
        }
    }

    // Once current server is elected as a leader, then it's always leader until dies.
    public boolean isMaster() {
        if (!isMaster) {
            try {
                // Default is not leader if Consul server is not enabled, because too many leaders in a cluster will affect data's accuracy.
                isMaster = consulClient != null && consulClient.setKVValue(CONSUL_LEADER_KEY + "?acquire=" + consulSessionId, CONSUL_LEADER_KEY_VALUE).getValue();
                if (isMaster) {
                    log.info("Server " + serverName + "-" + serverPort + " is leader.");
                }
            } catch (Exception e) {
                registerConsulSession();
            }
        }
        return isMaster;
    }

    /**
     * Multiple txle apps register the same key 'CONSUL_LEADER_KEY', it would be leader in case of getting 'true'.
     * The Session, Checks and Services have to be destroyed/deregistered before shutting down JVM, so that the lock of leader key could be released.
     *
     * @return String session id
     */
    public String registerConsulSession() {
        String serverHost = "127.0.0.1";
        try {
            // Firstly, to set an available ConsulClient before registering session.
            this.setAvailableConsulClient();
            if (consulClient != null) {
                destroyConsulCriticalServices();
                // To create a key for leader election no matter if it is exists.
                consulClient.setKVValue(CONSUL_LEADER_KEY, CONSUL_LEADER_KEY_VALUE);
                NewSession session = new NewSession();
                serverHost = CrossSystemInetAddress.readCrossSystemIPv4();
                session.setName("session-" + serverName + "-" + serverHost + "-" + serverPort + "-" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                consulSessionId = consulClient.sessionCreate(session, null).getValue();
                return consulSessionId;
            }
        } catch (Exception e) {
            log.error("Failed to register Consul Session, serverName [{}], serverHost [{}], serverPort [{}].", serverName, serverHost, serverPort, e);
        }
        return consulSessionId;
    }

    public void destroyConsulCriticalServices() {
        try {
            // To deregister service could not destroy session so that current service still held the lock for leader's key.
            // So to destroy session was necessary as well.
            if (consulSessionId != null) {
                consulClient.sessionDestroy(consulSessionId, null);
            }
            // consulClient.agentServiceDeregister(consulInstanceId);
            List<Check> checkList = consulClient.getHealthChecksState(null).getValue();
            if (checkList != null) {
                log.info("checkList size = " + checkList.size());
                checkList.forEach(check -> {
                    try {
                        if (check.getStatus() != Check.CheckStatus.PASSING || check.getServiceId().equals(consulInstanceId)) {
                            log.info("Executing method 'destroyConsulCriticalServices', check id = " + check.getCheckId() + ", service id = " + check.getServiceId() + " .");
                            consulClient.agentCheckDeregister(check.getCheckId());
                            consulClient.agentServiceDeregister(check.getServiceId());
                        }
                    } catch (Exception e) {
                        log.error("Failed to destroy Consul Critical Services. checkId = {}, serviceId = {}.", check.getCheckId(), check.getServiceId(), e);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Failed to destroy Consul Critical Services.", e);
        }
    }

    public void destroyCurrentSession() {
        try {
            Response<Session> session = consulClient.getSessionInfo(consulSessionId, QueryParams.DEFAULT);
            if (session != null) {
                consulClient.sessionDestroy(session.getValue().getId(), QueryParams.DEFAULT);
            }
        } catch (Exception e) {
            log.error("Failed to destroy current session from Consul.", e);
        }
    }

    @PostConstruct
    void init() {
        if (enabled) {
            this.initConsulCluster();
            this.registerConsulSession();
        }
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
    }

    @PreDestroy
    void shutdown() {
        if (enabled) {
            this.destroyConsulCriticalServices();
            this.destroyCurrentSession();
        }
    }

}
