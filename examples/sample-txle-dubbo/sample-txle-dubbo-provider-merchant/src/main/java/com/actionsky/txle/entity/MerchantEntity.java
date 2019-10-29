/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.entity;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
@Entity
@Table(name = "txle_sample_merchant")
public class MerchantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private double balance;

    @Version
    private int version;
    private Date createtime;

    public MerchantEntity() {
    }

    public MerchantEntity(String name, double balance, int version) {
        this.name = name;
        this.balance = balance;
        this.version = version;
        this.createtime = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public long getVersion() {
        return version;
    }

    protected void setVersion(int version) {
        this.version = version;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }
}
