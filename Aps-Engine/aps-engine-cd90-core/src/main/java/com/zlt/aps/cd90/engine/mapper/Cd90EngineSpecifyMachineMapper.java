package com.zlt.aps.cd90.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd90.engine.vo.Cd90SpecifyMachineVo;

/**
 * 90度裁断定点机台表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-14 11:40:19
 * @Version 1.0
 */
public interface Cd90EngineSpecifyMachineMapper {

	/**
	 * 加载90度裁断定点机台表
	 * 
	 * @param Cd90SpecifyMachineVo
	 * @return
	 */
	List<Cd90SpecifyMachineVo> selectCd90SpecifyMachineList(@Param("jobType") String jobType);
}
