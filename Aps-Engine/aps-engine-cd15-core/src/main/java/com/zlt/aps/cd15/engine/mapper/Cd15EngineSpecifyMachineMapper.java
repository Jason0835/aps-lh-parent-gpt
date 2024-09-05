package com.zlt.aps.cd15.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.cd15.engine.vo.Cd15SpecifyMachineVo;

/**
 * 15度裁断定点机台表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-11 11:40:19
 * @Version 1.0
 */
public interface Cd15EngineSpecifyMachineMapper {

	/**
	 * 加载15度裁断定点机台表
	 * 
	 * @return
	 */
	List<Cd15SpecifyMachineVo> selectCd15SpecifyMachineList(@Param("jobType") String jobType);
}
