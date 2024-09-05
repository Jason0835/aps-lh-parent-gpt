package com.zlt.aps.xwyy.engine.mapper;

import java.util.List;

import com.zlt.aps.xwyy.engine.vo.XwyyMachineRollMappingVo;

/**
 * 纤维压延帘线大卷与机台的映射表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:40:19
 * @Version 1.0
 */
public interface XwyyEngineMachineRollMappingMapper {

	/**
	 * 加载帘线大卷与机台的映射表
	 * @return
	 */
	List<XwyyMachineRollMappingVo> selectXwyyMachineRollMappingList();
}
