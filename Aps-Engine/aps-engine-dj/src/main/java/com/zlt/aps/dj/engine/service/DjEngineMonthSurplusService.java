package com.zlt.aps.dj.engine.service;

import java.util.Map;

import com.zlt.aps.dj.engine.vo.DjMonthSurplusVo;

/**
 * 垫胶月度汇总service
 */
public interface DjEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, DjMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
