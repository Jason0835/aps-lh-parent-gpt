package com.zlt.aps.xwyy.engine.mapper;

import java.util.List;

import com.zlt.aps.xwyy.engine.vo.XwyyLossSettingVo;

/**
 * 纤维压延工序损耗率设置表数据mapper
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 17:25:51
 */
public interface XwyyEngineLossMapper {
	/**
	 * 获取损耗率的设置信息列表
	 * @return
	 */
	List<XwyyLossSettingVo> listLossRate();
}
