package com.zlt.aps.gsq.engine.service;

import java.util.Map;

public interface GsqEngineMachineService {
    /**
     * 获得钢丝圈代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得钢丝圈代码和缠绕盘map (key = 规格尺寸~排列方式 )
     * @return
     */
    Map<String, String> getTwiningDiscMachineMap();

    /**
     * 获得钢丝圈代码和缠绕盘（value = 规格尺寸~排列方式）map
     * @Param scheduleDate 排程日期
     * @return
     */
    Map<String, String> getTwiningDiscMap(String scheduleDate);
}
