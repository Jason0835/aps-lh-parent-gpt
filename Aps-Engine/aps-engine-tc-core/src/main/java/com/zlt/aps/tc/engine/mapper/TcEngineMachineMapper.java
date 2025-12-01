package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;
import com.zlt.aps.tc.api.domain.entity.TcMachineMaintenance;
import com.zlt.aps.tc.engine.vo.TcMouthPlateMachineVo;
import com.zlt.aps.tc.engine.vo.TcSpecifyMachineVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TcEngineMachineMapper {

    /**
     * 查询胎侧定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<TcSpecifyMachineVo> listTcSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询胎侧口型板信息
     * @return
     */
    List<TcMouthPlateMachineVo> listTcMouthPlateMachine(@Param("status") String status);

    List<TcMachineInfo> listTcMachine();

    /**
     * 查询机台维护计划
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    List<TcMachineMaintenance> selectMachineMaintenance(String scheduleDate);
}
