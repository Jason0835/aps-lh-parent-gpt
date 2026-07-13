package com.zlt.aps.nc.engine.service;

import java.util.Date;
import java.util.List;

import com.zlt.aps.nc.api.domain.entity.NcScheduleResult;

/**
 * 内衬自动排程新算法服务接口
 * 根据成型计划自动排产垫胶工序6个班的备库计划
 */
public interface NcEngineNewService {

    /**
     * 内衬自动排程入口
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @return 排产结果列表
     */
    List<NcScheduleResult> autoNcSchedule(String factoryCode, Date scheduleDate);
}
