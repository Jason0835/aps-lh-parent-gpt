package com.zlt.mix.sync.vo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MES数据库配置
 * 
 * @author hakimryan
 *
 */
@Component
public class MesDBConfig implements SyncDataDbConfig {
	/**
	 * 数据库连接配置名称
	 */
	@Value("${syncdata.kettle.datasource.mes.linkName}")
	private String linkName;
	/**
	 * 数据库主机名称
	 */
	@Value("${syncdata.kettle.datasource.mes.hostName}")
	private String hostName;
	/**
	 * 数据库名称
	 */
	@Value("${syncdata.kettle.datasource.mes.dBName}")
	private String dBName;
	/**
	 * 数据库用户名
	 */
	@Value("${syncdata.kettle.datasource.mes.userName}")
	private String userName;
	/**
	 * 数据库密码
	 */
	@Value("${syncdata.kettle.datasource.mes.password}")
	private String password;

	@Override
	public String getLinkName() {
		return this.linkName;
	}

	@Override
	public String getDbName() {
		return this.dBName;
	}

	@Override
	public String getUserName() {
		return this.userName;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getHostName() {
		return this.hostName;
	}
}
