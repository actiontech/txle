-- TxEvent数据表字段配置信息
INSERT INTO TableField VALUES (5, 'TxEvent', 'surrogateId', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (10, 'TxEvent', 'globalTxId', '全局事务标识', 'varchar', 36, 0, 'true', 'true', 10, '', now());
INSERT INTO TableField VALUES (15, 'TxEvent', 'localTxId', '子事务标识', 'varchar', 36, 0, 'true', '', 15, '', now());
INSERT INTO TableField VALUES (20, 'TxEvent', 'parentTxId', '父事务标识', 'varchar', 36, 0, '', '', 20, '', now());
INSERT INTO TableField VALUES (25, 'TxEvent', 'serviceName', '服务名称', 'varchar', 100, 0, 'true', 'true', 25, '', now());
INSERT INTO TableField VALUES (30, 'TxEvent', 'instanceId', '服务代号', 'varchar', 100, 0, 'true', 'true', 30, '', now());
INSERT INTO TableField VALUES (35, 'TxEvent', 'category', '服务类别', 'varchar', 36, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (37, 'TxEvent', 'status', '状态', 'varchar', 20, 0, '', 'true', 37, '', now());
INSERT INTO TableField VALUES (40, 'TxEvent', 'type', '类型', 'varchar', 50, 0, 'true', '', 40, '', now());
INSERT INTO TableField VALUES (45, 'TxEvent', 'expiryTime', '到期时间', 'datetime', 0, 0, 'true', 'true', 45, '', now());
INSERT INTO TableField VALUES (50, 'TxEvent', 'retries', '重试次数', 'int', 11, 0, 'true', 'true', 50, '', now());
INSERT INTO TableField VALUES (55, 'TxEvent', 'retryMethod', '重试方法', 'varchar', 256, 0, '', '', 55, '', now());
INSERT INTO TableField VALUES (60, 'TxEvent', 'compensationMethod', '补偿方法', 'varchar', 256, 0, 'true', '', 60, '', now());
INSERT INTO TableField VALUES (65, 'TxEvent', 'payloads', '参数', 'blob', 0, 0, '', '', 65, '', now());
INSERT INTO TableField VALUES (70, 'TxEvent', 'creationTime', '开始时间', 'datetime', 0, 0, 'true', 'true', 70, '', now());
INSERT INTO TableField VALUES (75, 'TxEvent', 'endTime', '结束时间', 'datetime', 0, 0, 'true', 'true', 75, '', now());

-- Config数据表字段配置信息
INSERT INTO TableField VALUES (80, 'Config', 'id', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (85, 'Config', 'serviceName', '服务名称', 'varchar', 100, 0, '', 'true', 10, '', now());
INSERT INTO TableField VALUES (90, 'Config', 'instanceId', '服务代号', 'varchar', 100, 0, '', 'true', 15, '', now());
INSERT INTO TableField VALUES (95, 'Config', 'category', '服务类别', 'varchar', 100, 0, '', 'true', 20, '', now());
INSERT INTO TableField VALUES (100, 'Config', 'type', '类型', 'int', 2, 0, 'true', 'true', 25, '', now());
INSERT INTO TableField VALUES (105, 'Config', 'status', '状态', 'int', 1, 0, 'true', 'true', 30, '', now());
INSERT INTO TableField VALUES (110, 'Config', 'ability', '能力', 'int', 1, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (115, 'Config', 'value', '值', 'varchar', 100, 0, 'true', 'true', 40, '', now());
INSERT INTO TableField VALUES (120, 'Config', 'remark', '备注', 'varchar', 500, 0, '', 'true', 145, '', now());
INSERT INTO TableField VALUES (125, 'Config', 'updatetime', '更新时间', 'datetime', 0, 0, 'true', 'true', 50, '', now());

-- Accident数据表字段配置信息
INSERT INTO TableField VALUES (130, 'Accident', 'id', '主键', 'bigint', 20, 0, 'true', '', 5, '', now());
INSERT INTO TableField VALUES (135, 'Accident', 'globalTxId', '全局事务标识', 'varchar', 36, 0, 'true', 'true', 10, '', now());
INSERT INTO TableField VALUES (140, 'Accident', 'localTxId', '子事务标识', 'varchar', 36, 0, 'true', 'true', 15, '', now());
INSERT INTO TableField VALUES (145, 'Accident', 'serviceName', '服务名称', 'varchar', 100, 0, '', 'true', 20, '', now());
INSERT INTO TableField VALUES (150, 'Accident', 'instanceId', '服务代号', 'varchar', 100, 0, '', 'true', 25, '', now());
INSERT INTO TableField VALUES (155, 'Accident', 'category', '服务类别', 'varchar', 100, 0, '', 'true', 30, '', now());
INSERT INTO TableField VALUES (160, 'Accident', 'type', '类型', 'int', 1, 0, 'true', 'true', 35, '', now());
INSERT INTO TableField VALUES (165, 'Accident', 'status', '状态', 'int', 1, 0, 'true', 'true', 40, '', now());
INSERT INTO TableField VALUES (170, 'Accident', 'bizinfo', '业务信息', 'varchar', 1000, 0, 'true', 'true', 45, '', now());
INSERT INTO TableField VALUES (175, 'Accident', 'remark', '备注', 'varchar', 500, 0, '', 'true', 50, '', now());
INSERT INTO TableField VALUES (180, 'Accident', 'createtime', '上报时间', 'datetime', 0, 0, 'true', 'true', 55, '', now());
INSERT INTO TableField VALUES (185, 'Accident', 'completetime', '完成时间', 'datetime', 0, 0, 'true', 'true', 60, '', now());
