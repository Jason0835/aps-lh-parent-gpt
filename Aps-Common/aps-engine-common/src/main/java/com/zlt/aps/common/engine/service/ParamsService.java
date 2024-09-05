package com.zlt.aps.common.engine.service;
/**
 * 各工序系统参数获取
 * @Description
 */
public interface ParamsService {
	/**
	 * 成型参数
	 * 
	 * @param code
	 * @return
	 */
	String getCxParam(String code);

	/**
	 * 硫化参数
	 * 
	 * @param code
	 * @return
	 */
	String getLhParam(String code);
	
	/**
	 * 内衬参数
	 * 
	 * @param code
	 * @return
	 */
	String getNcParam(String code);

	/**
	 * 胎圈参数
	 * 
	 * @param code
	 * @return
	 */
	String getTqParam(String code);

	/**
	 * 胎侧参数
	 * 
	 * @param code
	 * @return
	 */
	String getTcParam(String code);

	/**
	 * 钢丝圈参数
	 * 
	 * @param code
	 * @return
	 */
	String getGsqParam(String code);

	/**
	 * 胎面参数
	 * 
	 * @param code
	 * @return
	 */
	String getTmParam(String code);

	/**
	 * 15度裁断参数
	 * 
	 * @param code
	 * @return
	 */
	String getCd15Param(String code);

	/**
	 * 90度裁断参数
	 * 
	 * @param code
	 * @return
	 */
	String getCd90Param(String code);

	/**
	 * 钢带压延参数
	 * 
	 * @param code
	 * @return
	 */
	String getGdyyParam(String code);

	/**
	 * 纤维压延参数
	 * 
	 * @param code
	 * @return
	 */
	String getXwyyParam(String code);
}
