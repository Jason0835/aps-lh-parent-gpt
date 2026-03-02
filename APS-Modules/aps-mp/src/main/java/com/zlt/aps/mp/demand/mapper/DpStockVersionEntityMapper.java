package com.zlt.aps.mp.demand.mapper;

import com.zlt.aps.mp.api.domain.entity.DpStockVersion;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：DpStockVersionMapper.java
 * 描    述：成品库存Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@Mapper
public interface DpStockVersionEntityMapper extends CommBaseMapper<DpStockVersion> {

    /**
     * 直接查询去重后的month_plan_version列表
     */
    @Select("SELECT REQUIRE_VERSION " +
            " FROM T_DP_STOCK_VERSION " +
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

