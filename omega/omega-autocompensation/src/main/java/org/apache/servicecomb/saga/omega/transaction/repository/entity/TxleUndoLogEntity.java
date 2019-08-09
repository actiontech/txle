package org.apache.servicecomb.saga.omega.transaction.repository.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.alibaba.fastjson.JSON;

/**
 * An entity mapping to database table 'txle_undo_log'.
 * recommendation: it's better to give lower names to fields, if not, it will bring trouble to you on variety of databases. 
 * 
 * @author Gannalyo
 * @since 2018-07-30
 */
@Entity
@Table(name = "txle_undo_log")
public class TxleUndoLogEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private String globaltxid;
	private String localtxid;

	private String executesql;
	private String compensatesql;
	private String originalinfo;

	private int status;
	private String server;

	private Date createtime;
	private Date lastmodifytime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public String getExecutesql() {
		return executesql;
	}

	public void setExecutesql(String executesql) {
		this.executesql = executesql;
	}

	public String getCompensatesql() {
		return compensatesql;
	}

	public void setCompensatesql(String compensatesql) {
		this.compensatesql = compensatesql;
	}

	public String getOriginalinfo() {
		return originalinfo;
	}

	public void setOriginalinfo(String originalinfo) {
		this.originalinfo = originalinfo;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public Date getCreatetime() {
		return createtime;
	}

	public void setCreatetime(Date createtime) {
		this.createtime = createtime;
	}

	public Date getLastmodifytime() {
		return lastmodifytime;
	}

	public void setLastmodifytime(Date lastmodifytime) {
		this.lastmodifytime = lastmodifytime;
	}

	public TxleUndoLogEntity() {
	}

	public TxleUndoLogEntity(String globalTxId, String localTxId, String executeSql, String compensateSql, String originalInfo, int status,
							 String server, Date createTime, Date lastModifyTime) {
		this.globaltxid = globalTxId;
		this.localtxid = localTxId;
		this.executesql = executeSql;
		this.compensatesql = compensateSql;
		this.originalinfo = originalInfo;
		this.status = status;
		this.server = server;
		this.createtime = createTime;
		this.lastmodifytime = lastModifyTime;
	}

	public String entityToString() {
		return JSON.toJSONString(this);
	}

}