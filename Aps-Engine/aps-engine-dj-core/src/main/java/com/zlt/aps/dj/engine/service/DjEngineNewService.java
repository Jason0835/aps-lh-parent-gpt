package com.zlt.aps.dj.engine.service;

import com.zlt.aps.dj.api.domain.entity.DjScheduleResult;

import java.util.Date;
import java.util.List;

/**
 * 垫胶自动排程新算法服务接口
 * 根据成型计划自动排产垫胶工序6个班的备库计划
 */
public interface DjEngineNewService {

    /**
     * 垫胶自动排程入口
     *
     * @param factoryCode  工厂编码
     * @param scheduleDate 排产日期
     * @return 排产结果列表
     */
    List<DjScheduleResult> autoDjSchedule(String factoryCode, Date scheduleDate);
}
