package com.zlt.aps.tq.engine.mapper;


import com.zlt.aps.tq.engine.vo.TqStockConsumeVo;
import com.zlt.aps.tq.engine.vo.TqStockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TqEngineStockMapper {

    /**
     * 查询胎圈胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<TqStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询当天早班(D日早班)计划量
     * @param scheduleDate
     * @param factoryCode 分厂编码（按工厂过滤）
     * @return
     */
    List<TqStockConsumeVo> listTodayMorningPlan(@Param("scheduleDate") String scheduleDate, @Param("factoryCode") String factoryCode);

    /**
     * 查询指定日期的胎圈库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @param factoryCode 分厂编码（按工厂过滤库存）
     * @return
     */
    List<TqStockVo> listTqStock(@Param("stockDate") String stockDate, @Param("factoryCode") String factoryCode);

    /**
     * 查询昨日早班半制品计划量（昨日排程的CLASS1_PLAN_QTY对应昨日早班生产量）
     * @param scheduleDate 排程日期，格式：yyyy-mm-dd
     * @return 昨日早班半制品计划量列表
     */
    List<TqStockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") String scheduleDate);
}
