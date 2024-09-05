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
     * 查询指定日期的胎圈库存量
     * @param stockDate  日期，格式：yyyy-mm-dd
     * @return
     */
    List<TqStockVo> listTqStock(@Param("stockDate") String stockDate);
}
