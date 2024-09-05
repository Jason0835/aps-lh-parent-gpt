package com.zlt.aps.gdyy.engine.mapper;

import java.util.List;

import com.zlt.aps.gdyy.engine.vo.GdyyLossSettingVo;

/**
 * 90度裁断工序损耗率设置表数据mapper
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 15:55:51
 */
public interface GdyyEngineLossMapper {
	/**
	 * 获取损耗率的设置信息列表
	 * @return
	 */
	List<GdyyLossSettingVo> listLossRate();
}
