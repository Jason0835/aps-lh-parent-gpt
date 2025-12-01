package com.zlt.mix.sync.vo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MES数据库
 * 
 * @author hakimryan
 *
 */
@Component
public class MixDBConfig implements SyncDataDbConfig {
	/**
	 * 数据库连接配置名称
	 */
	@Value("${syncdata.kettle.datasource.mix.linkName}")
	private String linkName;
	/**
	 * 数据库主机名称
	 */
	@Value("${syncdata.kettle.datasource.mix.hostName}")
	private String hostName;
	/**
	 * 数据库名称
	 */
	@Value("${syncdata.kettle.datasource.mix.dBName}")
	private String dBName;
	/**
	 * 数据库用户名
	 */
	@Value("${syncdata.kettle.datasource.mix.userName}")
	private String userName;
	/**
	 * 数据库密码
	 */
	@Value("${syncdata.kettle.datasource.mix.password}")
	private String password;

	@Override
	public String getLinkName() {
		return linkName;
	}

	@Override
	public String getDbName() {
		return dBName;
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getHostName() {
		return hostName;
	}

}
