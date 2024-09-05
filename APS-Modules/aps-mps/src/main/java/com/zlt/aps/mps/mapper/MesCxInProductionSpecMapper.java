package com.zlt.aps.mps.mapper;

/**
 * 
 * @Description
 * @Author zlt
 * @Date 2022-2-24 13:15:08
 */
public interface MesCxInProductionSpecMapper {
	/**
	 * 统计需同步数据量
	 * 
	 * @param dataVersion 数据版本
	 * @return 数据量
	 */
	int countSyncData(String dataVersion);

	/**
	 * 同步生产规格
	 * 
	 * @param dataVersion 数据版本
	 */
	void mergeSpec(String dataVersion);
}
