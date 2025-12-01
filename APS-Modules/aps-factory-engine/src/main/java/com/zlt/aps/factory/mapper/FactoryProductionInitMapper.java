package com.zlt.aps.factory.mapper;

import com.zlt.aps.factory.domain.vo.MonthPlanManufacturingRequirementVo;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMonthPlanInitMapper.java
 * 描    述：分厂月生产计划排产过程-计划初始化Mapper接口
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
public interface FactoryProductionInitMapper extends CommBaseMapper<ProductionMonthPlanInit> {
    /**
     * 更新排产初始化计划的排产顺序值
     *
     * @param productionSequenceList
     * @return
     */
    int updateProductionSequence(@Param("sequenceList") List<MonthPlanManufacturingRequirementVo> productionSequenceList);
}
