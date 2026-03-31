package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.mp.api.domain.entity.DpShippedNotScanVersion;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DpShippedNotScanVersionEntityMapper extends CommBaseMapper<DpShippedNotScanVersion> {

    @Select("SELECT REQUIRE_VERSION " +
            " FROM T_DP_SHIPPED_NOT_SCAN_VERSION " +
            " WHERE FACTORY_CODE = #{factoryCode} " +
            " AND `YEAR` = #{year} " +
            " AND `MONTH` = #{month} " +
            " AND IS_DELETE = #{isDelete} " +
            " GROUP BY REQUIRE_VERSION " +
            " ORDER BY MAX(CREATE_TIME) DESC ")
    List<String> selectDistinctMonthPlanVersion(@Param("factoryCode") String factoryCode,
                                                @Param("year") Integer year,
                                                @Param("month") Integer month,
                                                @Param("isDelete") Integer isDelete);
}
