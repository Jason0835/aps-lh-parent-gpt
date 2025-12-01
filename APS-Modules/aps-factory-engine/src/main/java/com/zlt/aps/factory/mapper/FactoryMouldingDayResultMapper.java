package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultMapper.java
 * 描    述：分厂月生产计划排产过程-模具排产结果汇总Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-13
 */
@Mapper
public interface FactoryMouldingDayResultMapper extends CommBaseMapper<MonthPlanMouldingDayResult> {

}
