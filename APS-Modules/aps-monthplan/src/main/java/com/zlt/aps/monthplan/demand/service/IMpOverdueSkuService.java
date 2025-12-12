package com.zlt.aps.monthplan.demand.service;

import java.util.Set;

import com.ruoyi.common.datasource.service.IBaseService;
import com.zlt.aps.monthplan.api.domain.entity.MpOverdueSku;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpOverdueSkuService.java
 * 描    述：IMpOverdueSkuService超期SKU后端接口
 *@author yelq
 *@date 2025-12-12
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
public interface IMpOverdueSkuService  extends IBaseService<MpOverdueSku>
{
    /**
     *  排除近12个月有周期性排产超期胎的SKU(超期SKU表.超期周期排产 = 1)，剩下的SKU则可生成到供应链订单池-周期排产储备
     */
    Set<String> excludeOverdueCycleProduction();
}
