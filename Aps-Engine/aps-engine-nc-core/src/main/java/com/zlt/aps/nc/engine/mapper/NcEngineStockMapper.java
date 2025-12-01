package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.nc.engine.vo.NcStockConsumeVo;
import com.zlt.aps.nc.engine.vo.NcStockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NcEngineStockMapper {

    /**
     * 查询内衬胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<NcStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询昨日早班计划量
     * @param scheduleDate
     * @return
     */
    List<NcStockConsumeVo> listLastDayMidPlan(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定日期的内衬库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<NcStockVo> listNcStock(@Param("stockDate") String stockDate);
}
