/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.bizdbinfo;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2020/03/17
 */
@Entity
@Table(name = "BusinessDBLatestDetail")
public class BusinessDBLatestDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long timestamp;
    private String node;
    private String dbschema;
    private String tablename;
    private String field;
    private String fieldtype;
    private int isprimarykey;
    private Date createtime;

    private BusinessDBLatestDetail() {
    }

    public BusinessDBLatestDetail(long timestamp, String node, String dbschema, String tablename, String field, String fieldtype, boolean isprimarykey) {
        this.timestamp = timestamp;
        this.node = node;
        this.dbschema = dbschema;
        this.tablename = tablename;
        this.field = field;
        this.fieldtype = fieldtype;
        this.isprimarykey = isprimarykey ? 1 : 0;
        this.createtime = new Date(System.currentTimeMillis());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getDbschema() {
        return dbschema;
    }

    public void setDbschema(String dbschema) {
        this.dbschema = dbschema;
    }

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getFieldtype() {
        return fieldtype;
    }

    public void setFieldtype(String fieldtype) {
        this.fieldtype = fieldtype;
    }

    public int getIsprimarykey() {
        return isprimarykey;
    }

    public void setIsprimarykey(int isprimarykey) {
        this.isprimarykey = isprimarykey;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }
}
