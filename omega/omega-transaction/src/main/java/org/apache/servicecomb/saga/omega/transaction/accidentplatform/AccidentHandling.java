package org.apache.servicecomb.saga.omega.transaction.accidentplatform;

import java.util.Date;

/**
 * Accident Handling.
 *
 * @author Gannalyo
 * @date 2019/06/14
 */
public class AccidentHandling {
    private Long id;

    private String servicename;
    private String instanceid;
    private String globaltxid;
    private String localtxid;

    private int type;// 1-rollback_error, 2-send_msg_error
    private int status;// 0-init, 1-sending, 2-success, 3-fail
    private String bizinfo;
    private String remark;
    private Date createtime;
    private Date completetime;

    private AccidentHandling() {
    }

    public AccidentHandling(String servicename, String instanceid, String globaltxid, String localtxid, int type, String bizinfo, String remark) {
        this.globaltxid = globaltxid;
        this.localtxid = localtxid;
        this.servicename = servicename;
        this.instanceid = instanceid;
        this.type = type;
        this.bizinfo = bizinfo;
        this.remark = remark;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServicename() {
        return servicename;
    }

    public void setServicename(String servicename) {
        this.servicename = servicename;
    }

    public String getInstanceid() {
        return instanceid;
    }

    public void setInstanceid(String instanceid) {
        this.instanceid = instanceid;
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getBizinfo() {
        return bizinfo;
    }

    public void setBizinfo(String bizinfo) {
        this.bizinfo = bizinfo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Date getCompletetime() {
        return completetime;
    }

    public void setCompletetime(Date completetime) {
        this.completetime = completetime;
    }
}
