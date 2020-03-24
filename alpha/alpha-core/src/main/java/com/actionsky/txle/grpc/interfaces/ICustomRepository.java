/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ICustomRepository {

    List executeQuery(String sql, Object... params);

    long count(String sql, Object... params);

    @Transactional
    int executeUpdate(String sql, Object... params);

}
