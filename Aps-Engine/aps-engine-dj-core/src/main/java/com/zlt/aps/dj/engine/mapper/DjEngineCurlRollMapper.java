package com.zlt.aps.dj.engine.mapper;

import java.util.List;

import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;

/**
 * 垫胶卷曲设置mapper
 */
public interface DjEngineCurlRollMapper {

	/**
	 * 查询垫胶卷曲设置列表
	 * 
	 * @return
	 */
	List<DjCurlRoll> getNcCurlRollList();
}
