package com.zlt.mix.schedule.engine.service.basicdata;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 引擎炼胶间隔时间相关Service
 */
public interface MixingTimeEngineService {

    /**
     * 加载胶料间隔时间，按胶料 + 机台分组
     */
    Map<String, Long> mapMixingIntervalTime(String mixArea);

    /**
     * 查询对应密炼区+胶料+机台的炼胶间隔时间
     *
     * @param mixArea     密炼区
     * @param glue        胶料
     * @param machineCode 机台
     * @return 炼胶间隔时间，如果无则为空
     */
    BigDecimal getIntervalTime(String mixArea, String glue, String machineCode);
}
