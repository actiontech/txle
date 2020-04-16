/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.service;

import com.actionsky.txle.grpc.repository.primary.PrimaryCustomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Service
public class PrimaryCustomService {

    @Autowired
    private PrimaryCustomRepository primaryCustomRepository;

    public List executeQuery(String sql, Object... params) {
        return primaryCustomRepository.executeQuery(sql, params);
    }

    public int executeUpdate(String sql, Object... params) {
        return primaryCustomRepository.executeUpdate(sql, params);
    }

    @Transactional
    public int executeSubTxSqls(@Param("sqls") List<String> sqls) {
        int result = 0;
        for (String sql : sqls) {
            if (sql.startsWith("DROP PROCEDURE")) {
                // 模拟程序的写法，仅针对alter_txle_backup_table存储过程
                String backupTableName = sql.substring(sql.indexOf("txle.") + 5, sql.indexOf(" ADD globalTxId VARCHAR(36)"));
                primaryCustomRepository.callProcedure("alter_txle_backup_table", "txle", backupTableName);
                result++;
            } else {
                result += primaryCustomRepository.executeUpdate(sql);
            }
        }
        return result;
    }
}
