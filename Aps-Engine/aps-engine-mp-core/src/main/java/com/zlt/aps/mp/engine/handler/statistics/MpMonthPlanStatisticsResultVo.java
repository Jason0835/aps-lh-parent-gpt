package com.zlt.aps.mp.engine.handler.statistics;

import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import lombok.Data;

import java.util.Map;
/**
 * 日排产结果Vo对象
 *
 * @author ZLT
 * @date 20260210
 */
@Data
public class MpMonthPlanStatisticsResultVo extends MpMonthPlanStatistics {

    private Map<Integer, MpDayProductionStatisticsDetailVo> dayStatisticsDetailMap;

}