package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.tc.engine.vo.TcGlueOrderVo;
import com.zlt.aps.tc.engine.vo.TcStockConsumeVo;
import com.zlt.aps.tc.engine.vo.TcStockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TcEngineStockMapper {

    /**
     * 查询胎侧胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<TcStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询昨日早班计划量
     * @param scheduleDate
     * @return
     */
    List<TcStockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") String scheduleDate);
    /**
     * 查询指定日期的胎侧库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<TcStockVo> listTcStock(@Param("stockDate") String stockDate);

    /**
     * 查询昨日早班计划量
     *
     * @param scheduleDate
     * @return
     */
    List<TcGlueOrderVo> listLastDayMidPlan4Glue(@Param("scheduleDate") String scheduleDate);
}
