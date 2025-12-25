package com.zlt.aps.factory.mapper;

import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayDetail;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanMouldingDayResultMapper.java
 * 描    述：S2-0602.排产结果-生产计划排产结果明细对象-Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-25
 */
@Mapper
public interface FactoryMouldingDayResultDetailMapper extends CommBaseMapper<FactoryMonthPlanMouldDayDetail> {

}
