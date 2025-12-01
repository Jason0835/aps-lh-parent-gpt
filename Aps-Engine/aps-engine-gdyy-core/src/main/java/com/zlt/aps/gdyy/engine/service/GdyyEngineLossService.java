package com.zlt.aps.gdyy.engine.service;

import java.util.Map;

/**
 * 90度裁断损耗率服务接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 15:50:59
 */
public interface GdyyEngineLossService {

	/**
	 * 
	 * 获取损耗率设定信息map
	 * 
	 * @return key:大卷编号，value：损耗率
	 */
	Map<String, Double> getLossRateMap();

	/**
	 * 获得损耗率 从损耗率表获取对应的损耗率，按以下优先级匹配：物料编号 > 默认值
	 *
	 * @param machineIds      机器编号
	 * @param bigRollCode     大卷编号
	 * @param defaultLossRate 损耗率默认值
	 * @return
	 */
	double getLossRate(String bigRollCode, String machineIds, Map<String, Double> lossMap, double defaultLossRate);
}
