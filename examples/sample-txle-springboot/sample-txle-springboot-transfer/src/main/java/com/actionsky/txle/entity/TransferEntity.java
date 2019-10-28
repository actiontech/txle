/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.entity;

import com.actionsky.txle.constant.TransferStatusEnum;
import com.actionsky.txle.constant.PayWayEnum;
import com.google.gson.GsonBuilder;

import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Entity
@Table(name = "txle_sample_transfer")
public class TransferEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long userid;
    private long merchantid;
    private double amount;
    private int payway;
    private int status;
    @Version
    private int version;
    private Date createtime;

    public TransferEntity() {
    }

    public TransferEntity(long userid, long merchantid, double amount, PayWayEnum payway, TransferStatusEnum payStatus) {
        this.userid = userid;
        this.merchantid = merchantid;
        this.amount = amount;
        this.payway = payway.toInteger();
        this.status = payStatus.toInteger();
        // 强制去除毫秒，因为mysql5.6版本后会针对毫秒数进行四舍五入，而java全舍，导致毫秒数大于500时的时间相差1秒
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            this.createtime = sdf.parse(sdf.format(new Date()));
        } catch (ParseException e) {
            this.createtime = new Date();
        }
    }

    public TransferEntity(long userid, long merchantid, double amount, PayWayEnum payway, TransferStatusEnum payStatus, Date createtime) {
        this.userid = userid;
        this.merchantid = merchantid;
        this.amount = amount;
        this.payway = payway.toInteger();
        this.status = payStatus.toInteger();
        this.createtime = createtime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public long getMerchantId() {
        return merchantid;
    }

    public void setMerchantId(long merchantid) {
        this.merchantid = merchantid;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getPayway() {
        return payway;
    }

    public void setPayway(int payway) {
        this.payway = payway;
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

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public String toJsonString() {
        return new GsonBuilder().create().toJson(this);
    }
}
