package com.zlt.aps.dj.engine.mapper;


import org.apache.ibatis.annotations.Param;

import com.zlt.aps.dj.engine.vo.DjMonthSurplusVo;

import java.util.List;

public interface DjEngineMonthSurplusMapper {


    /**
     * 查询垫胶月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<DjMonthSurplusVo> listDjMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
