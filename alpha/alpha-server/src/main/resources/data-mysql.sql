-- *** 所有英文数据值无特殊说明均小写，便于调用处操作 ***
-- 初始化部分配置
INSERT INTO Config VALUES (1, null, null, null, 12, 0,	1, '1',	'历史表间隔规则。值：0-日，1-月，2-季，3-年。注：不转储10天内的数据。', now());

-- TxEvent数据表字段配置信息
INSERT INTO TableField VALUES (5, 'txevent', 'surrogateid', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (10, 'txevent', 'globaltxid', '全局事务标识', 'varchar', 36, 0, 'true', 'true', 10, '', now());
INSERT INTO TableField VALUES (15, 'txevent', 'localtxid', '子事务标识', 'varchar', 36, 0, 'true', '', 15, '', now());
INSERT INTO TableField VALUES (20, 'txevent', 'parenttxid', '父事务标识', 'varchar', 36, 0, '', '', 20, '', now());
INSERT INTO TableField VALUES (25, 'txevent', 'servicename', '服务名称', 'varchar', 100, 0, 'true', 'true', 25, '', now());
INSERT INTO TableField VALUES (30, 'txevent', 'instanceid', '服务代号', 'varchar', 100, 0, 'true', 'true', 30, '', now());
INSERT INTO TableField VALUES (35, 'txevent', 'category', '服务类别', 'varchar', 36, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (37, 'txevent', 'status', '状态', 'varchar', 20, 0, '', 'true', 37, '', now());
INSERT INTO TableField VALUES (40, 'txevent', 'type', '类型', 'varchar', 50, 0, 'true', '', 40, '', now());
INSERT INTO TableField VALUES (45, 'txevent', 'expirytime', '到期时间', 'datetime', 0, 0, 'true', 'true', 45, '', now());
INSERT INTO TableField VALUES (50, 'txevent', 'retries', '重试次数', 'int', 11, 0, 'true', 'true', 50, '', now());
INSERT INTO TableField VALUES (55, 'txevent', 'retrymethod', '重试方法', 'varchar', 256, 0, '', '', 55, '', now());
INSERT INTO TableField VALUES (60, 'txevent', 'compensationmethod', '补偿方法', 'varchar', 256, 0, 'true', '', 60, '', now());
INSERT INTO TableField VALUES (65, 'txevent', 'payloads', '参数', 'blob', 0, 0, '', '', 65, '', now());
INSERT INTO TableField VALUES (70, 'txevent', 'creationtime', '开始时间', 'datetime', 0, 0, 'true', 'true', 70, '', now());
INSERT INTO TableField VALUES (75, 'txevent', 'endtime', '结束时间', 'datetime', 0, 0, 'true', 'true', 75, '', now());

-- Config数据表字段配置信息
INSERT INTO TableField VALUES (80, 'config', 'id', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (85, 'config', 'servicename', '服务名称', 'varchar', 100, 0, '', 'true', 10, '', now());
INSERT INTO TableField VALUES (90, 'config', 'instanceid', '服务代号', 'varchar', 100, 0, '', 'true', 15, '', now());
INSERT INTO TableField VALUES (95, 'config', 'category', '服务类别', 'varchar', 100, 0, '', 'true', 20, '', now());
INSERT INTO TableField VALUES (100, 'config', 'type', '类型', 'int', 2, 0, 'true', 'true', 25, '', now());
INSERT INTO TableField VALUES (105, 'config', 'status', '状态', 'int', 1, 0, 'true', 'true', 30, '', now());
INSERT INTO TableField VALUES (110, 'config', 'ability', '能力', 'int', 1, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (115, 'config', 'value', '值', 'varchar', 100, 0, 'true', 'true', 40, '', now());
INSERT INTO TableField VALUES (120, 'config', 'remark', '备注', 'varchar', 500, 0, '', 'true', 145, '', now());
INSERT INTO TableField VALUES (125, 'config', 'updatetime', '更新时间', 'datetime', 0, 0, 'true', 'true', 50, '', now());

-- Accident数据表字段配置信息
INSERT INTO TableField VALUES (130, 'accident', 'id', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (135, 'accident', 'globaltxid', '全局事务标识', 'varchar', 36, 0, 'true', 'true', 10, '', now());
INSERT INTO TableField VALUES (140, 'accident', 'localtxid', '子事务标识', 'varchar', 36, 0, 'true', 'true', 15, '', now());
INSERT INTO TableField VALUES (145, 'accident', 'servicename', '服务名称', 'varchar', 100, 0, '', 'true', 20, '', now());
INSERT INTO TableField VALUES (150, 'accident', 'instanceid', '服务代号', 'varchar', 100, 0, '', 'true', 25, '', now());
-- INSERT INTO TableField VALUES (155, 'accident', 'category', '服务类别', 'varchar', 100, 0, '', 'true', 30, '', now());
INSERT INTO TableField VALUES (160, 'accident', 'type', '类型', 'int', 1, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (165, 'accident', 'status', '状态', 'int', 1, 0, 'true', 'true', 40, '', now());
INSERT INTO TableField VALUES (170, 'accident', 'bizinfo', '业务信息', 'varchar', 1000, 0, 'true', 'true', 45, '', now());
INSERT INTO TableField VALUES (175, 'accident', 'remark', '备注', 'varchar', 500, 0, '', 'true', 50, '', now());
INSERT INTO TableField VALUES (180, 'accident', 'createtime', '上报时间', 'datetime', 0, 0, 'true', 'true', 55, '', now());
INSERT INTO TableField VALUES (185, 'accident', 'completetime', '完成时间', 'datetime', 0, 0, 'true', 'true', 60, '', now());

-- DataDictionary数据表内容初始化信息
INSERT INTO DataDictionary VALUES (1, '全局事务状态', 'global-tx-status', '', now());
INSERT INTO DataDictionary VALUES (2, '配置中心类型', 'config-center-type', '', now());
INSERT INTO DataDictionary VALUES (3, '配置中心状态', 'config-center-status', '', now());
INSERT INTO DataDictionary VALUES (4, '配置中心能力', 'config-center-ability', '', now());
INSERT INTO DataDictionary VALUES (5, '配置中心值', 'config-center-value', '', now());
INSERT INTO DataDictionary VALUES (6, '差错处理类型', 'accident-handle-type', '', now());
INSERT INTO DataDictionary VALUES (7, '差错处理状态', 'accident-handle-status', '', now());
INSERT INTO DataDictionary VALUES (8, '全局事务服务信息', 'global-tx-server-info', '', now());

-- DataDictionaryItem数据表内容初始化信息
INSERT INTO DataDictionaryItem VALUES (10, 'global-tx-status', '运行中', 'gts-running', '0', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (20, 'global-tx-status', '运行异常', 'gts-aborted', '1', 10, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (30, 'global-tx-status', '暂停', 'gts-suspended', '2', 15, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (40, 'global-tx-status', '正常结束', 'gts-over', '3', 20, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (50, 'global-tx-status', '异常结束', 'gts-terminated', '4', 25, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (60, 'config-center-type', '全局事务', 'cct-global-tx', '1', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (70, 'config-center-type', '手动补偿', 'cct-compensation', '2', 10, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (80, 'config-center-type', '自动补偿', 'cct-auto-compensation', '3', 15, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (90, 'config-center-type', '业务信息上报', 'cct-bizinfo-to-kafka', '4', 20, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (100, 'config-center-type', '事务监控', 'cct-tx-monitor', '5', 25, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (110, 'config-center-type', '告警', 'cct-alert', '6', 30, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (120, 'config-center-type', '定时任务', 'cct-schedule', '7', 35, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (130, 'config-center-type', '全局事务容错', 'cct-global-tx-fault-tolerant', '8', 40, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (140, 'config-center-type', '手动补偿容错', 'cct-compensation-fault-tolerant', '9', 45, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (150, 'config-center-type', '自动补偿容错', 'cct-auto-compensation-fault-tolerant', '10', 50, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (160, 'config-center-type', '暂停全局事务', 'cct-pause-global-tx', '11', 55, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (161, 'config-center-type', '历史表间隔规则', 'cct-history-table-interval-rule', '12', 56, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (170, 'config-center-type', '差错上报', 'cct-accident-report', '50', 60, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (180, 'config-center-type', 'SQL监控', 'cct-sql-monitor', '51', 65, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (190, 'config-center-status', '正常', 'ccs-normal', '0', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (200, 'config-center-status', '历史', 'ccs-history', '1', 10, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (210, 'config-center-status', '废弃', 'ccs-dumped', '2', 15, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (220, 'config-center-ability', '有', 'cca-yes', '1', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (230, 'config-center-ability', '无', 'cca-no', '0', 10, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (240, 'config-center-value', '启用', 'ccv-enabled', 'enabled', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (250, 'config-center-value', '禁用', 'ccv-disabled', 'disabled', 10, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (260, 'accident-handle-type', '回滚失败', 'aht-rollback-error', '1', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (270, 'accident-handle-type', '上报信息至Kafka失败', 'aht-send-message-error', '2', 10, 1, '', now());

INSERT INTO DataDictionaryItem VALUES (280, 'accident-handle-status', '发送中', 'ahs-sending', '0', 5, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (290, 'accident-handle-status', '发送成功', 'ahs-sending', '1', 10, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (300, 'accident-handle-status', '发送上报', 'ahs-sending', '2', 15, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (310, 'accident-handle-status', '处理成功', 'ahs-handle-ok', '3', 20, 1, '', now());
INSERT INTO DataDictionaryItem VALUES (320, 'accident-handle-status', '处理失败', 'ahs-handle-fail', '4', 25, 1, '', now());