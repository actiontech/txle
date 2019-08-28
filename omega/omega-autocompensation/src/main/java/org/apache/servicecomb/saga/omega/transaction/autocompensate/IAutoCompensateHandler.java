/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.autocompensate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public interface IAutoCompensateHandler {

    void saveAutoCompensationInfo(PreparedStatement delegate, String executeSql, boolean isBeforeNotice, Map<String, Object> standbyParams) throws SQLException;

}
