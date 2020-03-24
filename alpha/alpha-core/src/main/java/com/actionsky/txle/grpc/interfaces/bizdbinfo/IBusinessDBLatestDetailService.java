/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.bizdbinfo;

import java.util.List;

public interface IBusinessDBLatestDetailService {

    boolean save(BusinessDBLatestDetail businessDBLatestDetail);

    boolean save(List<BusinessDBLatestDetail> businessDBLatestDetails, boolean isFullDose);

    long selectMaxTimestamp();

}
