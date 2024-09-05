package com.zlt.aps.tq.engine.mapper;


import com.zlt.aps.tq.engine.vo.TqMonthSurplusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TqEngineMonthSurplusMapper {


    /**
     * 查询胎圈月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<TqMonthSurplusVo> listTqMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
