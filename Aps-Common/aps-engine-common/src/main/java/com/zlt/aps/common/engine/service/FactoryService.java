package com.zlt.aps.common.engine.service;

/**
 * 工厂服务接口
 * 
 * @Description
 */
public interface FactoryService {
	/**
	 * 获取当前所属厂别
	 * 
	 * @return
	 */
	String getFactoryCode();

	/**
	 * 获取当前所属分公司代号
	 * 
	 * @return
	 */
	String getCompanyCode();
}
