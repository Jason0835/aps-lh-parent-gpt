package com.zlt.aps.mp.engine.mapper;

import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

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
 * @date 20251209
 */
@Mapper
public interface FactoryProductionInitMapper extends CommBaseMapper<ProductionMonthPlanInit> {

}
