package com.zlt.aps.tm.engine.service;

import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TmEngineMachineService {
    /**
     * 获得胎面代码和定点机台的map
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
     * 获得已禁用的口型板代码和定点机台的map
     * @return
     */
    Map<String, String> getDisableMouthPlateMachineMap();

    /**
     * 获得机台信息
     *
     * @return 结果
     */
    List<TmMachineInfo> listTmMachine();

    /**
     * 获取机台维修计划需扣减的生产定额
     * K：GenerageMapKeyUtils.createMapKey(机台ID, 停机班次)，V：机台需扣减的生产定额
     *
     * @param scheduleDate 排程日期
     * @return 结果
     */
    Map<String, BigDecimal> selectMachineSubQuota(String scheduleDate);
}
