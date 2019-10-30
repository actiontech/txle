DROP TABLE IF EXISTS txle_sample_merchant;
CREATE TABLE txle_sample_merchant (
  id bigint NOT NULL AUTO_INCREMENT,
  name varchar(50),
  balance decimal(11, 5) NULL DEFAULT 0.00000,
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