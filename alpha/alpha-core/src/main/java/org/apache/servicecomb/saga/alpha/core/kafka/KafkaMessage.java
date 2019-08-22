package org.apache.servicecomb.saga.alpha.core.kafka;

import com.google.gson.GsonBuilder;
import org.springframework.util.MultiValueMap;

import javax.persistence.*;
import java.util.Date;

/**
 * Kafka message.
 *
 * @author Gannalyo
 * @since 2018/12/3
 */
@Entity
@Table(name = "Message")
public class KafkaMessage {
    // message head
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Date createtime;
    // 0-init, 1-sending, 2-success, 3-fail
    private int status;
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

    private KafkaMessage() {
    }

    public KafkaMessage(String globaltxid, String localtxid, String dbdrivername, String dburl, String dbusername, String tablename, String operation, String ids) {
        this.status = KafkaMessageStatus.INIT.toInteger();
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

    public KafkaMessage(MultiValueMap<String, String> mvm) {
        this.status = KafkaMessageStatus.INIT.toInteger();
        this.createtime = new Date(System.currentTimeMillis());
        this.version = 1;
        this.globaltxid = mvm.getFirst("globaltxid");
        this.localtxid = mvm.getFirst("localtxid");
        this.dbdrivername = mvm.getFirst("dbdrivername");
        this.dburl = mvm.getFirst("dburl");
        this.dbusername = mvm.getFirst("dbusername");
        this.tablename = mvm.getFirst("tablename");
        this.operation = mvm.getFirst("operation");
        this.ids = mvm.getFirst("ids");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDburl() {
        return dburl;
    }

    public void setDburl(String dburl) {
        this.dburl = dburl;
    }

    public String getDbdrivername() {
        return dbdrivername;
    }

    public void setDbdrivername(String dbdrivername) {
        this.dbdrivername = dbdrivername;
    }

    public String getDbusername() {
        return dbusername;
    }

    public void setDbusername(String dbusername) {
        this.dbusername = dbusername;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getIds() {
        return ids;
    }

    public void setIds(String ids) {
        this.ids = ids;
    }

    public String getGlobaltxid() {
        return globaltxid;
    }

    public void setGlobaltxid(String globaltxid) {
        this.globaltxid = globaltxid;
    }

    public String getLocaltxid() {
        return localtxid;
    }

    public void setLocaltxid(String localtxid) {
        this.localtxid = localtxid;
    }

    public void toJsonString() {
        new GsonBuilder().create().toJson(this);
    }
}
