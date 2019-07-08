package org.apache.servicecomb.saga.alpha.core.configcenter;

import com.google.gson.GsonBuilder;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.UtxConstants;
import org.springframework.util.MultiValueMap;

import javax.persistence.*;
import java.util.Date;

/**
 * ConfigCenter
 * 1.No config and all of configs except fault-tolerant are enabled by default.
 * 2.Global configs and client's configs can be set in the future.
 * 3.The client's priority is higher than global priority.
 *
 * @author Gannalyo
 * @date 2018/12/3
 */
@Entity
@Table(name = "Config")
public class ConfigCenter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String servicename;
    private String instanceid;
    private int status;// 0-normal, 1-historical, 2-dumped
    private int ability;// 0-do not provide ability, 1-provide ability  全局配置参数，非全局同步且只读：即全局配置是否提供当前配置对应功能，以“是否可手动补偿为例”，如果不提供则全局和非全局均不支持手动补偿功能，如果提供，则优先客户端再全局默认值
    private int type;// 1-globaltx, 2-compensation, 3-autocompensation, 4-bizinfotokafka, 5-txmonitor, 6-alert, 7-schedule, 8-globaltxfaulttolerant, 9-compensationfaulttolerant, 10-autocompensationfaulttolerant, 11-pauseglobaltx, 50-accidentreport, 51-sqlmonitor  if values are less than 50, then configs for server, otherwise configs for client.
    private String value;
    private String remark;
    private Date updatetime;

    private ConfigCenter() {
    }

    public ConfigCenter(String servicename, String instanceid, ConfigCenterStatus status, int ability, ConfigCenterType type, String value, String remark) {
        this.servicename = servicename;
        this.instanceid = instanceid;
        this.status = status.toInteger();
        this.ability = ability;
        this.type = type.toInteger();
        this.value = value;
        this.remark = remark;
        this.updatetime = new Date(System.currentTimeMillis());
    }

    public ConfigCenter(MultiValueMap<String, String> mvm) {
        this.servicename = mvm.getFirst("servicename");
        this.instanceid = mvm.getFirst("instanceid");
        this.status = toInteger(mvm.getFirst("status"), ConfigCenterStatus.Normal.toInteger());
        this.status = toInteger(mvm.getFirst("ability"), UtxConstants.YES);
        // To return a default value for 'status', but throw exception for 'type'. Because, the default value of the former is suitable and the later's is not good.
        this.type = Integer.valueOf(mvm.getFirst("type"));
        this.value = mvm.getFirst("value");
        this.remark = mvm.getFirst("remark");
        this.updatetime = new Date(System.currentTimeMillis());
    }

    private int toInteger(String value, int defaultValue) {
        try {
            return Integer.valueOf(value, ConfigCenterStatus.Normal.toInteger());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getAbility() {
        return ability;
    }

    public void setAbility(int ability) {
        this.ability = ability;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Date updatetime) {
        this.updatetime = updatetime;
    }

    public void toJsonString() {
        new GsonBuilder().create().toJson(this);
    }
}
