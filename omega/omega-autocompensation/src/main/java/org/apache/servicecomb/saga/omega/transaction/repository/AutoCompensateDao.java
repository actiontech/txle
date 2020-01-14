/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.repository;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

/**
 * The data interface for auto-compensation. Aim to execute complex and special SQL.
 *
 * @author Gannalyo
 * @since 201807-30
 */
public class AutoCompensateDao implements IAutoCompensateDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void setDataSource(DataSource dataSource) {
        if (dataSource != null && (jdbcTemplate.getDataSource() == null || !dataSource.equals(jdbcTemplate.getDataSource()))) {
            synchronized (AutoCompensateDao.class) {
                if (jdbcTemplate.getDataSource() == null || !dataSource.equals(jdbcTemplate.getDataSource())) {
                    jdbcTemplate.setDataSource(dataSource);
                }
            }
        }
    }

    @Modifying
    @Transactional
    @Override
    public int executeUpdate(String sql) {
        return jdbcTemplate.update(sql + TxleConstants.ACTION_SQL);
    }

    @Modifying
    @Transactional
    @Override
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public int executeQueryCount(String sql, Object... params) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, params);
        if (list != null) {
            return Integer.parseInt(list.get(0).values().stream().findFirst().get().toString());
        }
        return 0;
    }
}
