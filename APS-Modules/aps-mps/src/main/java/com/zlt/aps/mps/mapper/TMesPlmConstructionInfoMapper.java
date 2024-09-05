package com.zlt.aps.mps.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * PLM参数数据接口
 * 
 * @Description
 * @Author hakimryan
 * @Date 2021-10-8 17:27:16
 */
public interface TMesPlmConstructionInfoMapper {

	/**
	 * 将中间表PLM数据合并到业务表中
	 * 
	 * @param dataVersion 同步版本
	 */
	void mergeSql(@Param("dataVersion") String dataVersion);
}
