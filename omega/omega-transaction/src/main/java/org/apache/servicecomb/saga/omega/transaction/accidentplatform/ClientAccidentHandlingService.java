/*
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.accidentplatform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;

public class ClientAccidentHandlingService {

    @Autowired
    private MessageSender sender;

    public boolean reportMsgToAccidentPlatform(String jsonParams) {
        JsonObject jsonObject = new JsonParser().parse(jsonParams).getAsJsonObject();
        String category = null;
        JsonElement categoryJson = jsonObject.get("category");
        if (categoryJson != null) {
            category = categoryJson.getAsString();
        }

        // report exception to Accident Platform in case of enabling global transaction.
        if (sender.readConfigFromServer(ConfigCenterType.AccidentReport.toInteger(), category).getStatus()) {
            String serviceName = "", instanceId = "", globalTxId = "", localTxId = "", bizinfo = "", remark = "";
            int type = 1;
            JsonElement jsonElement = jsonObject.get("servicename");
            if (jsonElement != null) {
                serviceName = jsonElement.getAsString();
            }
            jsonElement = jsonObject.get("instanceid");
            if (jsonElement != null) {
                instanceId = jsonElement.getAsString();
            }
            jsonElement = jsonObject.get("globaltxid");
            if (jsonElement != null) {
                globalTxId = jsonElement.getAsString();
            }
            jsonElement = jsonObject.get("localtxid");
            if (jsonElement != null) {
                localTxId = jsonElement.getAsString();
            }
            jsonElement = jsonObject.get("type");
            if (jsonElement != null) {
                type = jsonElement.getAsInt();
            }
            jsonElement = jsonObject.get("bizinfo");
            if (jsonElement != null) {
                bizinfo = jsonElement.getAsString();
            }
            jsonElement = jsonObject.get("remark");
            if (jsonElement != null) {
                remark = jsonElement.getAsString();
            }
            AccidentHandling accident = new AccidentHandling(serviceName, instanceId, globalTxId, localTxId, type, bizinfo, remark);
            return "true".equals(sender.reportAccidentToServer(accident));
        }
        return false;
    }

}