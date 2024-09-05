package com.zlt.aps.xwyy.engine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zlt.aps.xwyy.engine.vo.XwyySpecifyMachineVo;

/**
 * 纤维压延定点机台表数据mapper
 * 
 * @Description
 * @Author hakimrayn
 * @Date 2021-7-22 11:40:19
 * @Version 1.0
 */
public interface XwyyEngineSpecifyMachineMapper {

	/**
	 * 纤维压延定点机台表
	 * 
	 * @return
	 */
	List<XwyySpecifyMachineVo> selectXwyySpecifyMachineList(@Param("jobType") String jobType);
}
