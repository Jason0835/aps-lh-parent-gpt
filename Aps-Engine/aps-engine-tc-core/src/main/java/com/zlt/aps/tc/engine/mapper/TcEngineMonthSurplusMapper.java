package com.zlt.aps.tc.engine.mapper;


import com.zlt.aps.tc.engine.vo.TcMonthSurplusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TcEngineMonthSurplusMapper {


    /**
     * 查询胎侧月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<TcMonthSurplusVo> listTcMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
