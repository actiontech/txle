package org.apache.servicecomb.saga.omega.transaction.repository.entity;

/**
 * SQL执行计划实体信息
 * 
 * @author Gannalyo
 * @since 2018-08-06
 */
public class SQLExplain {
	private long id;
	private String table;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getTable() {
		return table;
	}

	public void setTable(String table) {
		this.table = table;
	}

}
