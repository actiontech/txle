package org.apache.servicecomb.saga.omega.transaction;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.servicecomb.saga.omega.context.UtxConstants;
import org.apache.servicecomb.saga.omega.rmi.accidentplatform.IAccidentPlatformService;
import org.apache.servicecomb.saga.omega.transaction.repository.AutoCompensateEntityRepository;
import org.apache.servicecomb.saga.omega.transaction.repository.IAutoCompensateDao;
import org.apache.servicecomb.saga.omega.transaction.repository.entity.SagaUndoLogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

	private AutoCompensateEntityRepository autoCompensateRepository;
	
	public AutoCompensateService(AutoCompensateEntityRepository autoCompensateRepository) {
		this.autoCompensateRepository = autoCompensateRepository;
	}
	
	@Override
	public boolean saveAutoCompensableInfo(String globalTxId, String localTxId, LinkedList<String> executeSqlList, LinkedList<String> originalInfoList, String server) {
		return saveSagaUndoLogEntity(globalTxId, localTxId, originalInfoList, null, originalInfoList, server);
	}
	
	private boolean saveSagaUndoLogEntity(String globalTxId, String localTxId, LinkedList<String> executeSqlList, LinkedList<String> compensateSqlList, LinkedList<String> originalInfoList, String server) {
		boolean result = false;
		String autoCompensableInfo = "";
		try {
			StringBuffer executeSql = new StringBuffer();
			StringBuffer compensateSql = new StringBuffer();
			StringBuffer originalInfoSql = new StringBuffer();
			
			executeSqlList.forEach(sql -> executeSql.append(sql));
			compensateSqlList.forEach(sql -> compensateSql.append(sql));
			originalInfoList.forEach(sql -> originalInfoSql.append(sql));
			
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "To save SagaUndoLogEntity：globalTxId = {}, localTxId = {}, executeSql = {}, compensateSql = {}, server = {}", globalTxId, localTxId, executeSql, compensateSql, server);
			
			Date currentDateTime = new Date();
			SagaUndoLogEntity sagaUndoLogEntity = new SagaUndoLogEntity(globalTxId, localTxId, executeSql.toString(), compensateSql.toString(), originalInfoSql.toString(), 0, server, currentDateTime, currentDateTime);
			result = autoCompensateRepository.save(sagaUndoLogEntity) != null;
			if (!result) {
				autoCompensableInfo = sagaUndoLogEntity.entityToString();
			}
		} catch (Exception e) {
			LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Fail to save auto-compensable info, SagaUndoLogEntity[{}].", autoCompensableInfo, e);
			throw e;
		}
		return result;
	}

//	@Transactional(propagation = Propagation.NOT_SUPPORTED) // Propagation.NOT_SUPPORTED/REQUIRED_NEW indeed is okay, if data are not same among transactions. 
	@Override
	public boolean executeAutoCompensateByLocalTxId(String globalTxId, String localTxId) {
		AtomicInteger result = new AtomicInteger(0);
		List<SagaUndoLogEntity> sagaUndoLogEntityList = autoCompensateRepository.findSagaUndoLogEntityByGlobalTxId(globalTxId, localTxId);
		if (sagaUndoLogEntityList != null && !sagaUndoLogEntityList.isEmpty()) {
			sagaUndoLogEntityList.forEach(entity -> {
				try {
					String[] compensateSqlArr = entity.getCompensatesql().split(";\n");
					for (String compensateSql : compensateSqlArr) {
						// TODO 依据条件查出新数据，采用probuf编码，与编码好的老数据进行对比，如果相等则继续执行，如果不等，则报差错平台，查询新数据时要先锁上数据避免查询完更新前被修改
						boolean tempResult = autoCompensateDao.executeAutoCompensateSql(compensateSql);
						if (tempResult) {
							result.incrementAndGet();
							LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Success to executed AutoCompensable SQL [{}], result [{}]", compensateSql, tempResult);
						} else {
							// TODO 报差错平台，其余的是否继续执行？？？ TODO 手动补偿时，也需报差错平台
							accidentPlatformService.test("localTxId = " + localTxId);
							LOG.error(UtxConstants.logErrorPrefixWithTime() + "Fail to executed AutoCompensable SQL [{}], result [{}]", compensateSql, tempResult);
							throw new RuntimeException(UtxConstants.logErrorPrefixWithTime() + "Fail to executed AutoCompensable SQL [" + compensateSql + "], result [" + tempResult + "]");
						}
					}
				} catch (Exception e) {
					LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Fail to execute AutoCompensable SQL, UndoLog [{}]", entity.entityToString(), e);
				}
			});
			// TODO to update compensation status in saga_undo_log.
		}
		return result.get() > 0;
	}

}