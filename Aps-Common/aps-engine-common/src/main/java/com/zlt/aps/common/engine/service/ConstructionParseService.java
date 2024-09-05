package com.zlt.aps.common.engine.service;

import java.util.List;

import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;

/**
 * 施工表解析接口
 * 
 * @Description
 */
public interface ConstructionParseService {

	/**
	 * 根据条件获取指定层级的施工表数据
	 * 
	 * @param params         过滤条件
	 * @param isShowDisabled 是否显示废止项
	 * @return
	 */
	List<CxProductConstructionInfo> getPartsConstruction(CxProductConstructionInfo params, boolean isShowDisabled);
}
