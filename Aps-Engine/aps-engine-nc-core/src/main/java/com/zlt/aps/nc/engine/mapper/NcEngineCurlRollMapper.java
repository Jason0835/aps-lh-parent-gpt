package com.zlt.aps.nc.engine.mapper;

import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;

/**
 * 内衬卷曲设置mapper
 */
public interface NcEngineCurlRollMapper {

	/**
	 * 查询内衬卷曲设置列表
	 * 
	 * @return
	 */
	List<NcCurlRoll> getNcCurlRollList();
}
