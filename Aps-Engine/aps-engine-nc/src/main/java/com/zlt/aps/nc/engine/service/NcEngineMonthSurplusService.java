package com.zlt.aps.nc.engine.service;

import com.zlt.aps.nc.engine.vo.NcMonthSurplusVo;

import java.util.Map;

/**
 * 内衬月度汇总service
 */
public interface NcEngineMonthSurplusService {

    /**
     * 获得月度计划剩余量、完成量
     * @param scheduleDate 排程日期
     * @return
     */
    Map<String, NcMonthSurplusVo> getMonthSurplus(String scheduleDate);
}
