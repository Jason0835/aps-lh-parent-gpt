package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import com.zlt.aps.cd15.engine.vo.Cd15LossSettingVo;

/**
 * 15度裁断工序损耗率设置表数据mapper
 * @Description
 * @Author hakimryan
 * @Date 2021-8-10 13:55:51
 */
public interface Cd15EngineLossMapper {
	/**
	 * 获取损耗率的设置信息列表
	 * @return
	 */
	List<Cd15LossSettingVo> listLossRate();
}
