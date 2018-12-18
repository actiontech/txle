package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;

import java.util.Date;

/**
 * Kafka message.
 *
 * @author Gannalyo
 * @date 2018/12/3
 */
public class KafkaMessage {
    private Date createtime;
    private int status;// 0-init, 1-sending, 2-success, 3-fail
    private int version;

    // message body, datasource, table and data and the like, from business.
    private String dbdrivername;
    private String dburl;
    private String dbusername;

    private String tablename;
    private String operation;
    private String ids;

    private String globaltxid;
    private String localtxid;

    private KafkaMessage() {}

    public KafkaMessage(String dbdrivername, String dburl, String dbusername, String tablename, String operation, String ids) {
        this.status = 0;
        this.createtime = new Date(System.currentTimeMillis());
        this.version = 1;
        this.globaltxid = CurrentThreadOmegaContext.getGlobalTxIdFromCurThread();
        this.localtxid = CurrentThreadOmegaContext.getLocalTxIdFromCurThread();
        this.dbdrivername = dbdrivername;
        this.dburl = dburl;
        this.dbusername = dbusername;
        this.tablename = tablename;
        this.operation = operation;
        this.ids = ids;
    }

    public KafkaMessage(String globaltxid, String localtxid, String dbdrivername, String dburl, String dbusername, String tablename, String operation, String ids) {
        this.status = 0;
        this.createtime = new Date(System.currentTimeMillis());
        this.version = 1;
        this.globaltxid = globaltxid;
        this.localtxid = localtxid;
        this.dbdrivername = dbdrivername;
        this.dburl = dburl;
        this.dbusername = dbusername;
        this.tablename = tablename;
        this.operation = operation;
        this.ids = ids;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public int getStatus() {
        return status;
    }

    public int getVersion() {
        return version;
    }

    public String getDburl() {
        return dburl;
    }

    public String getDbdrivername() {
        return dbdrivername;
    }

    public String getDbusername() {
        return dbusername;
    }

    public String getTablename() {
        return tablename;
    }

    public String getOperation() {
        return operation;
    }

    public String getIds() {
        return ids;
    }

    public String getGlobaltxid() {
        return globaltxid;
    }

    public String getLocaltxid() {
        return localtxid;
    }
}
