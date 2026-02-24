package com.zlt.aps.maindata.mapper;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyMapper.java
 * 描    述：月均销量Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Mapper
public interface MpMonthlySaleQtyEntityMapper extends CommBaseMapper<MpMonthlySaleQty> {
  /**
   *  获取调整月均销量
   * @param createCondition 调整参数
   * @return 月均销量
   */
  List<MpMonthlySaleQty> findAdjustMonthlySaleQty(DpDemandPlan createCondition);
}
