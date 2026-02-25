package com.zlt.aps.maindata.service;

import com.zlt.aps.mp.api.domain.entity.MdmMonCycleSchStruConf;
import com.zlt.aps.mp.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMonCycleSchStruConfService.java
 * 描    述：IMdmMonCycleSchStruConfService月周期排产结构配置后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-09
 */
public interface IMdmMonCycleSchStruConfService extends IDocService<MdmMonCycleSchStruConf> {
  /**
   *  查询当前周期性排产结构配置
   * @return 当前周期性排产结构配置
   */
  List<MdmMonCycleSchStruConf> findCurrentCycleSchStruConf(SupplyOrderPool supplyOrderPool);
}
