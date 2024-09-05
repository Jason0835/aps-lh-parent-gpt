package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.engine.vo.Cd15MachineRollMappingVo;

/**
 * 15度裁断钢压大卷与机台的映射表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-11 11:40:19
 * @Version 1.0
 */
public interface Cd15EngineMachineRollMappingMapper {

	/**
	 * 加载15度裁断钢压大卷与机台的映射表
	 * @Author hakimryan
	 * @Description
	 * @Date 2021-7-11 11:38:13
	 * @return
	 */
	List<Cd15MachineRollMappingVo> selectCd15MachineRollMappingList();
}
