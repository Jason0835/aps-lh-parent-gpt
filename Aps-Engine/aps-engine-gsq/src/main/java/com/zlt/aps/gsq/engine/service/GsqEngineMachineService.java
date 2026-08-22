package com.zlt.aps.gsq.engine.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;

public interface GsqEngineMachineService {
    /**
     * 加载有效钢丝圈机台
     * @return
     */
    List<GsqMachineInfo> listGsqMachine();
    /**
     * 获得钢丝圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获取上一天规格已排产机台列表
     * @param scheduleDate
     * @return
     */
    Map<String, String> getLastDayPlanMachine(Date scheduleDate);
}
