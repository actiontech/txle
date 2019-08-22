package org.apache.servicecomb.saga.omega.transaction;

import com.google.gson.JsonObject;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandleType;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.ClientAccidentHandlingService;
import org.apache.servicecomb.saga.omega.transaction.repository.IAutoCompensateDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Business interface for auto-compensation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
@Service
public class AutoCompensateService implements IAutoCompensateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoCompensateService.class);

    @Autowired
    private IAutoCompensateDao autoCompensateDao;

    @Autowired
    private ClientAccidentHandlingService clientAccidentHandlingService;

    @Autowired
    private MessageSender sender;

    // @Transactional(propagation = Propagation.NOT_SUPPORTED) // Propagation.NOT_SUPPORTED/REQUIRED_NEW indeed is okay, if data are not same among transactions.
    @Override
    public boolean executeAutoCompensateByLocalTxId(String globalTxId, String localTxId) {
        AtomicInteger result = new AtomicInteger(0);
        LOG.error("Executing AutoCompensable method 'executeAutoCompensateByLocalTxId'.");
        try {
            autoCompensateDao.setDataSource(DataSourceMappingCache.get(localTxId));
        } catch (Exception e) {
            LOG.error("Failed to change datasource globalTxId [{}], localTxId [{}].", globalTxId, localTxId, e);
        }
        final List<Map<String, Object>> txleUndoLogList;
        try {
            txleUndoLogList = autoCompensateDao.execute("SELECT * FROM txle_undo_log T WHERE T.globalTxId = ? AND T.localTxId = ?", globalTxId, localTxId);
        } catch (Exception e) {
            reportMsgToAccidentPlatform(globalTxId, localTxId, "", "Failed to select undo_log info, " + e.getMessage());
            return false;
        }
        if (txleUndoLogList == null || txleUndoLogList.isEmpty()) {
            reportMsgToAccidentPlatform(globalTxId, localTxId, "", "The undo_log info is empty.");
            return false;
        }

        txleUndoLogList.forEach(map -> {
            String[] compensateSqlArr;
            try {
                compensateSqlArr = map.get("compensateSql").toString().split(";\n");
            } catch (Exception e) {
                reportMsgToAccidentPlatform(globalTxId, localTxId, map.get("compensateSql") + "", "Failed to parse AutoCompensable SQL.");
                return;
            }
            for (String compensateSql : compensateSqlArr) {
                // TODO 依据条件查出新数据，采用probuf编码，与编码好的老数据进行对比，如果相等则继续执行，如果不等，则报差错平台，查询新数据时要先锁上数据避免查询完更新前被修改
                try {
                    if (autoCompensateDao.executeAutoCompensateSql(compensateSql)) {
                        result.incrementAndGet();
                        LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Successfully to execute AutoCompensable SQL [[{}]]", compensateSql);
                    } else {
                        reportMsgToAccidentPlatform(globalTxId, localTxId, compensateSql, "Got false value after executing AutoCompensable SQL [" + compensateSql + "].");
                    }
                } catch (Exception e) {
                    // 如果被补偿的接口是因为数据库连接数过大等数据库原因，那么此处findOne方法也很可能会执行失败，所以捕获下
                    reportMsgToAccidentPlatform(globalTxId, localTxId, compensateSql, "Failed to execute AutoCompensable SQL [" + compensateSql + "], " + e.getMessage());
                }
            }
        });

        // TODO to update compensation status in txle_undo_log.
        return result.get() > 0;
    }

    private void reportMsgToAccidentPlatform(String globalTxId, String localTxId, String bizinfo, String remark) {
        JsonObject jsonParams = new JsonObject();
        try {
            // TODO 报差错平台，其余的是否继续执行？？？ TODO 手动补偿时，也需报差错平台
            jsonParams.addProperty("type", AccidentHandleType.ROLLBACK_ERROR.toInteger());
            jsonParams.addProperty("globaltxid", globalTxId);
            jsonParams.addProperty("localtxid", localTxId);
            jsonParams.addProperty("bizinfo", bizinfo);
            jsonParams.addProperty("remark", remark);
            LOG.error(TxleConstants.logErrorPrefixWithTime() + "Failed to execute AutoCompensable method jsonParams [{}]", jsonParams.toString());
            clientAccidentHandlingService.reportMsgToAccidentPlatform(jsonParams.toString());
            // 不要抛出异常，否则org.apache.servicecomb.saga.omega.context.CompensationContext中报(IllegalAccessException | InvocationTargetException)错误
            // throw new RuntimeException(TxleConstants.logErrorPrefixWithTime() + "Failed to execute AutoCompensable SQL [" + compensateSql + "], result [" + tempResult + "]");
        } catch (Exception e) {
            LOG.error("Failed to report accident for method 'executeAutoCompensateByLocalTxId', params [{}].", jsonParams.toString(), e);
        }
    }

}