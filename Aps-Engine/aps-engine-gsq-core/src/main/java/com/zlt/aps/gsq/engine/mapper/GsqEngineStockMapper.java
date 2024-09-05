package com.zlt.aps.gsq.engine.mapper;


import com.zlt.aps.gsq.engine.vo.GsqStockConsumeVo;
import com.zlt.aps.gsq.engine.vo.GsqStockVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GsqEngineStockMapper {

    /**
     * 查询钢丝圈胶对应的成型三班计划消耗量，以及8-12点实际消耗量
     * @param scheduleDate
     * @return
     */
    List<GsqStockConsumeVo> listCxPlanAndConsume(@Param("scheduleDate") String scheduleDate);

    /**
     * 查询指定日期的钢丝圈库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<GsqStockVo> listGsqStock(@Param("stockDate") String stockDate);
}
