package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionRecordMapper.java
 * 描    述：不排产记录Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-07
 */
@Mapper
public interface FactoryNoProductionRecordMapper extends CommBaseMapper<MonthPlanNoProductionRecord> {
    /**
     * 删除不排产记录
     *
     * @param factoryCode      分厂编码
     * @param year             年份
     * @param month            月份
     * @param monthPlanVersion 需求版本
     * @return
     */
    int deletedNoProductionRecord(@Param("factoryCode") String factoryCode,
                                  @Param("year") Integer year,
                                  @Param("month") Integer month,
                                  @Param("monthPlanVersion") String monthPlanVersion,
                                  @Param("productionVersion") String productionVersion);
}
