package org.apache.servicecomb.saga.omega.transaction.repository.entity;

/**
 * MySQL执行计划实体信息
 * 
 * @author Gannalyo
 * @since 2018-08-06
 */
public class MySQLExplain extends SQLExplain {
	private String select_type;
	private String partitions;
	private String type;
	private String possible_keys;
	private String key;
	private int key_len;
	private String ref;
	private int rows;
	private String filtered;
	private String extra;

	public String getSelect_type() {
		return select_type;
	}

	public void setSelect_type(String select_type) {
		this.select_type = select_type;
	}

	public String getPartitions() {
		return partitions;
	}

	public void setPartitions(String partitions) {
		this.partitions = partitions;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPossible_keys() {
		return possible_keys;
	}

	public void setPossible_keys(String possible_keys) {
		this.possible_keys = possible_keys;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getKey_len() {
		return key_len;
	}

	public void setKey_len(int key_len) {
		this.key_len = key_len;
	}

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public String getFiltered() {
		return filtered;
	}

	public void setFiltered(String filtered) {
		this.filtered = filtered;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

}
