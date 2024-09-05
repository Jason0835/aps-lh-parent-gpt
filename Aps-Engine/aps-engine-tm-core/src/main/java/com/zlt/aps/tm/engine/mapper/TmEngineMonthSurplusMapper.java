package com.zlt.aps.tm.engine.mapper;


import com.zlt.aps.tm.engine.vo.TmMonthSurplusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface TmEngineMonthSurplusMapper {


    /**
     * 查询胎面月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<TmMonthSurplusVo> listTmMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
