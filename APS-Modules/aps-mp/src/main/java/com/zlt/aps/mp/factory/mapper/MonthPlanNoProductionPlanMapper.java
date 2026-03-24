package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanNoProductionPlanMapper.java
 * 描    述：分厂月生产计划排产过程-未排产计划Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-14
 */
@Mapper
public interface MonthPlanNoProductionPlanMapper extends CommBaseMapper<MonthPlanNoProductionPlan> {

    /**
     * 查询导出列表
     * @param query 查询条件
     * @return 导出数据列表
     */
    List<MonthPlanNoProductionPlan> selectExportList(@Param("query") MonthPlanNoProductionPlan query);

    /**
     * 查询列表（按SKU汇总）
     * @param query 查询条件
     * @return 数据列表
     */
    List<MonthPlanNoProductionPlan> selectList(@Param("query") MonthPlanNoProductionPlan query);
}
