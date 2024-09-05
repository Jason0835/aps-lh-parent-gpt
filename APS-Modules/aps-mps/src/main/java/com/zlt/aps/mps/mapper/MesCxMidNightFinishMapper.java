package com.zlt.aps.mps.mapper;

/**
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 10:59:08
 */
public interface MesCxMidNightFinishMapper {
	/**
	 * 统计需同步数据量
	 * 
	 * @param dataVersion 数据版本
	 * @return 数据量
	 */
	int countSyncData(String dataVersion);

	/**
	 * 同步完成量
	 * 
	 * @param dataVersion 数据版本
	 */
	void mergeFinishQty(String dataVersion);
}
