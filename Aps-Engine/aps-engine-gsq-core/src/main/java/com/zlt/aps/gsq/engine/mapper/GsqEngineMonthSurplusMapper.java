package com.zlt.aps.gsq.engine.mapper;


import com.zlt.aps.gsq.engine.vo.GsqMonthSurplusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GsqEngineMonthSurplusMapper {


    /**
     * 查询钢丝圈月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<GsqMonthSurplusVo> listGsqMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
