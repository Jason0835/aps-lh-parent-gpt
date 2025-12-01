package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.tm.engine.vo.TmGlueOrderVo;
import com.zlt.aps.tm.engine.vo.TmStockConsumeVo;
import com.zlt.aps.tm.engine.vo.TmStockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TmEngineStockMapper {

    /**
     * 查询胎面胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<TmStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询昨日早班计划量
     * @param scheduleDate
     * @return
     */
    List<TmStockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") String scheduleDate);


    /**
     * 查询昨日早班计划量
     *
     * @param scheduleDate
     * @return
     */
    List<TmGlueOrderVo> listLastDayMidPlan4Glue(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定日期的胎面库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<TmStockVo> listTmStock(@Param("stockDate") String stockDate);
}
