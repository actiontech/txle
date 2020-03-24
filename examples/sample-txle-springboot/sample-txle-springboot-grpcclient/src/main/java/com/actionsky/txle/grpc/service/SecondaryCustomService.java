/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.service;

import com.actionsky.txle.grpc.repository.secondary.SecondaryCustomRepository;
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
@Transactional("secondaryTransactionManager")
public class SecondaryCustomService {

    @Autowired
    private SecondaryCustomRepository secondaryCustomRepository;

    public List executeQuery(String sql, Object... params) {
        return secondaryCustomRepository.executeQuery(sql, params);
    }

    public int executeSubTxSqls(@Param("sqls") List<String> sqls) {
        int result = 0;
        for (String sql : sqls) {
            // todo create、set、commit等执行成功也不会返回1
            if (sql.startsWith("DROP PROCEDURE")) {
                // 模拟程序的写法，仅针对alter_txle_backup_table存储过程
                String backupTableName = sql.substring(sql.indexOf("txle.") + 5, sql.indexOf(" ADD globalTxId VARCHAR(36)"));
                secondaryCustomRepository.callProcedure("alter_txle_backup_table", "txle", backupTableName);
                result++;
            } else {
                result += secondaryCustomRepository.executeUpdate(sql);
            }
        }
        return result;
    }
}
