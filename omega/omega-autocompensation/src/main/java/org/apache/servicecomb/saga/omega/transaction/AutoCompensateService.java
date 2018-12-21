package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.UtxConstants;
import org.apache.servicecomb.saga.common.rmi.accidentplatform.IAccidentPlatformService;
import org.apache.servicecomb.saga.omega.transaction.repository.IAutoCompensateDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
	IAccidentPlatformService accidentPlatformService;

//	@Transactional(propagation = Propagation.NOT_SUPPORTED) // Propagation.NOT_SUPPORTED/REQUIRED_NEW indeed is okay, if data are not same among transactions. 
	@Override
	public boolean executeAutoCompensateByLocalTxId(String globalTxId, String localTxId) {
		AtomicInteger result = new AtomicInteger(0);
        autoCompensateDao.setDataSource(DataSourceMappingCache.get(localTxId));
        List<Map<String, Object>> sagaUndoLogList = autoCompensateDao.execute("SELECT * FROM saga_undo_log T WHERE T.globalTxId = ? AND T.localTxId = ?", globalTxId, localTxId);
        if (sagaUndoLogList != null && !sagaUndoLogList.isEmpty()) {
            sagaUndoLogList.forEach(map -> {
				try {
					String[] compensateSqlArr = map.get("compensateSql").toString().split(";\n");
					for (String compensateSql : compensateSqlArr) {
						// TODO 依据条件查出新数据，采用probuf编码，与编码好的老数据进行对比，如果相等则继续执行，如果不等，则报差错平台，查询新数据时要先锁上数据避免查询完更新前被修改
						boolean tempResult = autoCompensateDao.executeAutoCompensateSql(compensateSql);
						if (tempResult) {
							result.incrementAndGet();
							LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Success to executed AutoCompensable SQL [{}], result [{}]", compensateSql, tempResult);
						} else {
							// TODO 报差错平台，其余的是否继续执行？？？ TODO 手动补偿时，也需报差错平台
							accidentPlatformService.reportMsgToAccidentPlatform("localTxId = " + localTxId);
							LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to executed AutoCompensable SQL [{}], result [{}]", compensateSql, tempResult);
							throw new RuntimeException(UtxConstants.logErrorPrefixWithTime() + "Fail to executed AutoCompensable SQL [" + compensateSql + "], result [" + tempResult + "]");
						}
					}
				} catch (Exception e) {
					LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to execute AutoCompensable SQL, UndoLog [{}]", StringUtils.collectionToCommaDelimitedString(sagaUndoLogList), e);
				}
			});
			// TODO to update compensation status in saga_undo_log.
		}
		return result.get() > 0;
	}

}