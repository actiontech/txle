CREATE TABLE IF NOT EXISTS txle_sample_transfer (
  id bigint NOT NULL AUTO_INCREMENT,
  userid int(11) NOT NULL,
  merchantid varchar(50) NULL DEFAULT NULL,
  amount decimal(11, 5) NOT NULL DEFAULT 0.00000,
  payway int(2) NULL DEFAULT 1,
  status int(1) NULL DEFAULT 1,
  version int(1) NULL DEFAULT 1,
  createtime datetime(0) NULL DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
) DEFAULT CHARSET=utf8;

CREATE TABLE  IF NOT EXISTS txle_undo_log  (
  id bigint NOT NULL AUTO_INCREMENT,
  globalTxId varchar(100) NOT NULL,
  localTxId varchar(100) NOT NULL,
  executeSql varchar(1000) NOT NULL,
  compensateSql varchar(1000) NOT NULL,
  originalinfo blob NULL,
  status int(1) NOT NULL DEFAULT 0,
  server varchar(50) NOT NULL COMMENT 'ip:port',
  lastModifyTime datetime(0) NOT NULL,
  createTime datetime(0) NOT NULL,
  PRIMARY KEY (id) USING BTREE,
  INDEX unionkey(globalTxId, localTxId) USING BTREE
) DEFAULT CHARSET=utf8;