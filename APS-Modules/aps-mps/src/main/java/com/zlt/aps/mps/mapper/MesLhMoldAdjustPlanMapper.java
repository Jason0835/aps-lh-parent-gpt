package com.zlt.aps.mps.mapper;

public interface MesLhMoldAdjustPlanMapper {
	/**
	 * 判断是否有数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	int checkHasData(String dataVersion);

	/**
	 * 合并数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	int deleteData(String dataVersion);

	/**
	 * 合并数据
	 * 
	 * @param dataVersion 数据版本
	 * @return
	 */
	int mergeData(String dataVersion);
}
