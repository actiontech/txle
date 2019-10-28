/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import javax.persistence.*;

@Entity
@Table(name = "TableField")
public class TableFieldEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tablename;
    private String field;
    private String fieldname;
    private String type;
    private int fieldlength;
    private int decimalpoint;
    // true/false，default false
    private String notnull;
    // true/false，default false
    private String display;
    private int showorder;
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getFieldname() {
        return fieldname;
    }

    public void setFieldname(String fieldname) {
        this.fieldname = fieldname;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getFieldlength() {
        return fieldlength;
    }

    public void setFieldlength(int fieldlength) {
        this.fieldlength = fieldlength;
    }

    public int getDecimalpoint() {
        return decimalpoint;
    }

    public void setDecimalpoint(int decimalpoint) {
        this.decimalpoint = decimalpoint;
    }

    public String getNotnull() {
        return notnull;
    }

    public void setNotnull(String notnull) {
        this.notnull = notnull;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public int getShoworder() {
        return showorder;
    }

    public void setShoworder(int showorder) {
        this.showorder = showorder;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
