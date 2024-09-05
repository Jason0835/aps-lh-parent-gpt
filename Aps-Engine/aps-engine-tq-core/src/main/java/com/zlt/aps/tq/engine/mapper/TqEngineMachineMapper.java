package com.zlt.aps.tq.engine.mapper;


import com.zlt.aps.tq.engine.vo.TqMouthPlateMachineVo;
import com.zlt.aps.tq.engine.vo.TqSpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TqEngineMachineMapper {

    /**
     * 查询胎圈定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<TqSpecifyMachineVo> listTqSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询胎圈口型板信息
     * @return
     */
    List<TqMouthPlateMachineVo> listTqMouthPlateMachine();

}
