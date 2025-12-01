package com.zlt.aps.tc.engine.service;

import com.zlt.aps.tc.api.domain.entity.TcMachineInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TcEngineMachineService {


    /**
     * 获得胎侧代码和定点机台的map
     * @param jobType 作业类型，0-限制作业，1-不可作业
     * @return
     */
    Map<String, String> getSpecifyMachineMap(String jobType);

    /**
     * 获得口型板代码和定点机台的map
     * @return
     */
    Map<String, String> getMouthPlateMachineMap();

    /**
     * 获得已禁用口型板代码和定点机台的map
     * @return
     */
    Map<String, String> getDisableMouthPlateMachineMap();

    /**
     * 获得机台信息
     *
     * @return 机台信息
     */
    List<TcMachineInfo> listTcMachine();

    /**
     * 获取机台维修计划需扣减的生产定额
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    Map<String, BigDecimal> selectMachineSubQuota(String scheduleDate);
}
