package com.zlt.mix.sync.vo;

/**
 * 同步数据库配置接口
 * 
 * @author hakimryan
 *
 */
public interface SyncDataDbConfig {
	/**
	 * 
	 * 数据库配置名称
	 * @return
	 */
	String getLinkName();
	
	/**
	 * 数据库主机名称
	 * @return
	 */
	String getHostName();
	
	/**
	 * 数据库名称
	 * 
	 * @return
	 */
	String getDbName();

	/**
	 * 数据库用户名
	 * 
	 * @return
	 */
	String getUserName();

	/**
	 * 数据库密码
	 * 
	 * @return
	 */
	String getPassword();
}
