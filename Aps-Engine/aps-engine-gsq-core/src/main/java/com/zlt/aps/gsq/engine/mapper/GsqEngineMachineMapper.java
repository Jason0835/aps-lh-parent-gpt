package com.zlt.aps.gsq.engine.mapper;


import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.vo.GsqSpecifyMachineVo;
import com.zlt.aps.gsq.engine.vo.GsqTwiningDiscMachineVo;
import com.zlt.aps.gsq.engine.vo.GsqTwiningDiscVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface GsqEngineMachineMapper {

    /**
     * 查询钢丝圈定点机台信息
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    List<GsqSpecifyMachineVo> listGsqSpecifyMachine(@Param("jobType") String jobType);

    /**
     * 查询缠绕盘和机台map (key = 规格尺寸~排列方式 )
     * @return
     */
    List<GsqTwiningDiscMachineVo> listGsqTwiningDiscMachine();

    /**
     * 查询和当日成型排程对应的 胎圈与 尺寸+排列 的关系
     * @param scheduleDate 排程日期
     * @return
     */
    List<GsqTwiningDiscVo> listGsqTwiningDisc(String scheduleDate);
    
    /**
     * 获取钢丝圈机台
     * @return
     */
    List<GsqMachineInfo> listGsqMachine();
    
    /**
     * 获取上一天规格已排产机台列表
     * @param scheduleDate
     * @return
     */
    List<GsqSpecifyMachineVo> listLastDayPlanMachine(@Param("scheduleDate")Date scheduleDate);
}
