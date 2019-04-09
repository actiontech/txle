package org.apache.servicecomb.saga.omega.transaction.accidentplatform;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.rmi.accidentplatform.AccidentPlatformService;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

public class ClientAccidentPlatformService extends AccidentPlatformService {

    @Autowired
    private MessageSender sender;

    public ClientAccidentPlatformService(String accidentPlatformAddress, int retries, int interval, RestTemplate restTemplate) {
        super(accidentPlatformAddress, retries, interval, restTemplate);
    }

    @Override
    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        if (sender.readConfigFromServer(ConfigCenterType.AccidentReport.toInteger()).getStatus()) {// 差错平台上报支持配置降级功能
            return super.reportMsgToAccidentPlatform(jsonParams);
        }
        return false;
    }

}