package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.tm.engine.vo.TmMouthPlateMachineVo;
import com.zlt.aps.tm.engine.vo.TmSpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface TmEngineMachineMapper {

    /**
     * 查询胎面定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<TmSpecifyMachineVo> listTmSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询胎面口型板信息
     * @return
     */
    List<TmMouthPlateMachineVo> listTmMouthPlateMachine();

}
