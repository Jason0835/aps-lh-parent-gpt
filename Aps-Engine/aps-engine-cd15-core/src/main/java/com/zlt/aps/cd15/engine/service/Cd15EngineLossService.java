package com.zlt.aps.cd15.engine.service;

import java.util.Map;

/**
 * 损耗率服务接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 13:50:59
 */
public interface Cd15EngineLossService {

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:钢带编号#机台id，value：损耗率
	 */
	Map<String, Double> getLossRateMap();

	/**
	 * 获得损耗率
	 * 从损耗率表获取对应的损耗率，按以下优先级匹配：机台+物料编号 > 物料编号 > 机台 >默认值
	 *
	 * @param steelStripCode  钢带编号
	 * @param machineIds      机台id
	 * @param defaultLossRate 损耗率默认值
	 * @return
	 */
	double getLossRate(String steelStripCode, String machineIds, Map<String, Double> lossMap, double defaultLossRate);
}
