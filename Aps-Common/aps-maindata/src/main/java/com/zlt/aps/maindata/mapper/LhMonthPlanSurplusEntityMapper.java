package com.zlt.aps.maindata.mapper;

import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：LhMonthPlanSurplusMapper.java
 * 描    述：月度计划外胎汇总Mapper接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface LhMonthPlanSurplusEntityMapper extends CommBaseMapper<LhMonthPlanSurplus> {


    /**
     * 根据分厂编码、规格代码以及年和月份查询月剩余数据
     * @param factoryCode 分厂编码
     * @param specCodes 规格代码集合
     * @param year 年份
     * @param month 月份
     * @return 月剩余数据列表
     */
    List<LhMonthPlanSurplus> queryByFactoryAndSpecCodes(@Param("factoryCode") String factoryCode,
                                                         @Param("specCodes") List<String> specCodes,
                                                         @Param("year") Integer year,
                                                         @Param("month") Integer month);

    /**
     * 更新月度计划量、月剩余量
     */
    int updateMonthPlanQty(Collection<LhMonthPlanSurplus> list);

    /**
     * 查询对应月度外胎汇总记录
     */
    List<LhMonthPlanSurplus> selectParamList(List<LhMonthPlanSurplus> list);
}
