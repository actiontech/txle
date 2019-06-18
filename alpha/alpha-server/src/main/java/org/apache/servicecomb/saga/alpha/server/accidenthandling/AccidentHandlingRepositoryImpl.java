package org.apache.servicecomb.saga.alpha.server.accidenthandling;

import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleStatus;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandling;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class AccidentHandlingRepositoryImpl implements IAccidentHandlingRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final PageRequest pageRequest = new PageRequest(0, 100);

    private AccidentHandlingEntityRepository accidentHandlingEntityRepository;

    public AccidentHandlingRepositoryImpl(AccidentHandlingEntityRepository accidentHandlingEntityRepository) {
        this.accidentHandlingEntityRepository = accidentHandlingEntityRepository;
    }

    @Override
    public boolean save(AccidentHandling accidentHandling) {
        try {
            accidentHandlingEntityRepository.save(accidentHandling);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to save accident handling.", e);
        }
        return false;
    }

    @Override
    public List<AccidentHandling> findAccidentHandlingList() {
        return accidentHandlingEntityRepository.findAccidentHandlingList(pageRequest);
    }

    @Override
    public List<AccidentHandling> findAccidentHandlingList(AccidentHandleStatus status) {
        return accidentHandlingEntityRepository.findAccidentListByStatus(status.toInteger());
    }

    @Override
    public boolean updateAccidentStatusByIdList(List<Long> idList, AccidentHandleStatus status) {
        return accidentHandlingEntityRepository.updateAccidentStatusByIdList(idList, status.toInteger()) > 0;
    }
}
