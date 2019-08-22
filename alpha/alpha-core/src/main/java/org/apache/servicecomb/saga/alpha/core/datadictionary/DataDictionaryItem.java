package org.apache.servicecomb.saga.alpha.core.datadictionary;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/7/11
 */
@Entity
@Table(name = "DataDictionaryItem")
public class DataDictionaryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ddcode;
    private String name;
    private String code;
    private String value;
    private int showorder;
    private int visible;
    private String remark;
    private Date createtime;

    private DataDictionaryItem() {
    }

    public DataDictionaryItem(String ddcode, String name, String code, String value, int showorder, int visible, String remark) {
        this.ddcode = ddcode;
        this.name = name;
        this.code = code;
        this.value = value;
        this.showorder = showorder;
        this.visible = visible;
        this.remark = remark;
        this.createtime = new Date(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDdcode() {
        return ddcode;
    }

    public void setDdcode(String ddcode) {
        this.ddcode = ddcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getShoworder() {
        return showorder;
    }

    public void setShoworder(int showorder) {
        this.showorder = showorder;
    }

    public int getVisible() {
        return visible;
    }

    public void setVisible(int visible) {
        this.visible = visible;
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
}
