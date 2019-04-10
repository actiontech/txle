package org.apache.servicecomb.saga.alpha.server.accidentplatform;

import com.google.gson.JsonParser;
import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.rmi.accidentplatform.AccidentPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class ServerAccidentPlatformService extends AccidentPlatformService {

    @Autowired
    IConfigCenterService dbDegradationConfigService;

    public ServerAccidentPlatformService(String accidentPlatformAddress, int retries, int interval, RestTemplate restTemplate) {
        super(accidentPlatformAddress, retries, interval, restTemplate);
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        String instanceId = new JsonParser().parse(jsonParams).getAsJsonObject().get("instanceId").getAsString();
        if (dbDegradationConfigService.isEnabledTx(instanceId, ConfigCenterType.AccidentReport)) {// 差错平台上报支持配置降级功能
            return super.reportMsgToAccidentPlatform(jsonParams);
        }
        return false;
    }

}