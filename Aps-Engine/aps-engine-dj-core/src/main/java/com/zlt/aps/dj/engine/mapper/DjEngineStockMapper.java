package com.zlt.aps.dj.engine.mapper;


import org.apache.ibatis.annotations.Param;

import com.zlt.aps.dj.engine.vo.DjStockConsumeVo;
import com.zlt.aps.dj.engine.vo.DjStockVo;

import java.util.List;

public interface DjEngineStockMapper {

    /**
     * 查询垫胶胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<DjStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询昨日早班计划量
     * @param scheduleDate
     * @return
     */
    List<DjStockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定日期的垫胶库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<DjStockVo> listNcStock(@Param("stockDate") String stockDate);
}
