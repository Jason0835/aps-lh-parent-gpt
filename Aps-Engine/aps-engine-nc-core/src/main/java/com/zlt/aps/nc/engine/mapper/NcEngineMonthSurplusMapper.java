package com.zlt.aps.nc.engine.mapper;


import com.zlt.aps.nc.engine.vo.NcMonthSurplusVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NcEngineMonthSurplusMapper {


    /**
     * 查询内衬月度剩余量
     * @param year  年
     * @param month 月
     * @return
     */
    List<NcMonthSurplusVo> listNcMonthPlanSurplus(@Param("year") String year, @Param("month") String month);
}
