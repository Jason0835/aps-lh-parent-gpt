package com.zlt.aps.cd90.engine.mapper;

import java.util.List;

import com.zlt.aps.cd90.engine.vo.Cd90MachineRollMappingVo;

/**
 * 90度裁断钢压大卷与机台的映射表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:40:19
 * @Version 1.0
 */
public interface Cd90EngineMachineRollMappingMapper {

	/**
	 * 加载90度裁断帘布大卷与机台的映射表
	 * @return
	 */
	List<Cd90MachineRollMappingVo> selectCd90MachineRollMappingList();
}
