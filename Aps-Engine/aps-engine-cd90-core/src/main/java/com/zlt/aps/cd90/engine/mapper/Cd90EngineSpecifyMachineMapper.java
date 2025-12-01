package com.zlt.aps.cd90.engine.mapper;

import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.engine.vo.Cd90SpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

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

    /**
     * 查询90度裁断机台表
     *
     * @return 结果
     */
    List<Cd90MachineInfo> listCd90Machine();
    
    /**
     * 查询上一排产日规格与机台的排产情况（用于判断续做）
     * @param scheduleDate
     * @return
     */
    List<Cd90SpecifyMachineVo> listLastDayPlanMachine(@Param("scheduleDate") Date scheduleDate);
}
