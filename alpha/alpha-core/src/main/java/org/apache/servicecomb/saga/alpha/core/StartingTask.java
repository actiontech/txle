/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.session.model.NewSession;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY;
import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY_VALUE;

@Component
public class StartingTask implements ApplicationRunner {
    private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired(required = false)
    private ConsulClient consulClient;

    @Autowired
    private ITxleCache txleCache;

    @Value("${spring.application.name:\"\"}")
    private String serverName;

    @Value("${server.port:8090}")
    private int serverPort;

    @Value("${spring.cloud.consul.discovery.instanceId:\"\"}")
    private String consulInstanceId;

    private static String consulSessionId;
    private boolean isMaster;

    @PostConstruct
    void init() {
        // Register current server to Consul when starting.
        registerConsulSession();
    }

    @Override
    public void run(ApplicationArguments args) {
        // Notify all servers to do something, like reloading cache, updating service list and the like.
        // Synchronize cache from the leader server.
        txleCache.synchronizeCacheFromLeader(consulSessionId);
        // Notify all servers to reload the cache of service list from Consul.
        txleCache.refreshServiceListCache(true);
    }

    @PreDestroy
    void shutdown() {
        try {
            destroyConsulCriticalServices();
            txleCache.refreshServiceListCache(true);
        } catch (Exception e) {
            log.error("Failed to add ShutdownHook for destroying/deregistering Consul Session, Checks and Services, serverName [{}], serverPort [{}].", serverName, serverPort, e);
        }
    }

    // Once current server is elected as a leader, then it's always leader until dies.
    public boolean isMaster() {
        if (!isMaster) {
            isMaster = consulClient != null && consulClient.setKVValue(CONSUL_LEADER_KEY + "?acquire=" + consulSessionId, CONSUL_LEADER_KEY_VALUE).getValue();
            if (isMaster) {
                log.info("Server " + serverName + "-" + serverPort + " is leader.");
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
    private String registerConsulSession() {
        if (consulClient == null) {
            return null;
        }
        String serverHost = "127.0.0.1";
        try {
            destroyConsulCriticalServices();
            // To create a key for leader election no matter if it is exists.
            consulClient.setKVValue(CONSUL_LEADER_KEY, CONSUL_LEADER_KEY_VALUE);
            NewSession session = new NewSession();
            serverHost = InetAddress.getLocalHost().getHostAddress();
            session.setName("session-" + serverName + "-" + serverHost + "-" + serverPort + "-" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            consulSessionId = consulClient.sessionCreate(session, null).getValue();
        } catch (Exception e) {
            log.error("Failed to register Consul Session, serverName [{}], serverHost [{}], serverPort [{}].", serverName, serverHost, serverPort, e);
        }
        return consulSessionId;
    }

    private void destroyConsulCriticalServices() {
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
                if (check.getStatus() != Check.CheckStatus.PASSING || check.getServiceId().equals(consulInstanceId)) {
                    log.info("Executing method 'destroyConsulCriticalServices', check id = " + check.getCheckId() + ", service id = " + check.getServiceId() + " .");
                    consulClient.agentCheckDeregister(check.getCheckId());
                    consulClient.agentServiceDeregister(check.getServiceId());
                }
            });
        }
    }

}
