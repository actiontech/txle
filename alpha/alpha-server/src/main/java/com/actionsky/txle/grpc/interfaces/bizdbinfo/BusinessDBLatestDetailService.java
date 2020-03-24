/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.bizdbinfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BusinessDBLatestDetailService implements IBusinessDBLatestDetailService {

    private BusinessDBLatestDetailRepository bizDBDetailRepository;

    public BusinessDBLatestDetailService(BusinessDBLatestDetailRepository businessDBLatestDetailRepository) {
        this.bizDBDetailRepository = businessDBLatestDetailRepository;
    }

    @Override
    public boolean save(BusinessDBLatestDetail businessDBLatestDetail) {
        return bizDBDetailRepository.save(businessDBLatestDetail) != null;
    }

    @Override
    public boolean save(List<BusinessDBLatestDetail> businessDBLatestDetails, boolean isFullDose) {
        Set<String> deleteSqlSet = new LinkedHashSet<>();
        if (isFullDose) {
            businessDBLatestDetails.forEach(detail -> {
                // delete all data ahead of saving in the full-dose scenario
                if (!deleteSqlSet.contains(detail.getNode() + detail.getDbschema())) {
                    deleteSqlSet.add(detail.getNode() + detail.getDbschema());
                    bizDBDetailRepository.deleteHistoryInfo(detail.getNode(), detail.getDbschema());
                }
            });
        } else {
            businessDBLatestDetails.forEach(detail -> bizDBDetailRepository.deleteHistoryInfo(detail.getTimestamp(), detail.getNode(), detail.getDbschema(), detail.getTablename()));
        }

        return bizDBDetailRepository.save(businessDBLatestDetails) != null;
    }

    @Override
    public long selectMaxTimestamp() {
        return bizDBDetailRepository.selectMaxTimestamp();
    }
}
