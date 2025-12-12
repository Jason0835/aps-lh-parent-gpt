package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmFinishStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmFinishStockService.java
 * 描    述：IMdmFinishStockService成品库存后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
public interface IMdmFinishStockService extends IDocService<MdmFinishStock> {

    /**
     * 查询MES实时成品库存列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MdmFinishStock> list4Mes(MdmFinishStock queryVO);
    /**
     *  排除近12个月有周期性排产超期胎的SKU(超期SKU表.超期周期排产 = 1)，剩下的SKU则可生成到供应链订单池-周期排产储备
     * @return
     */
    List<MdmFinishStock> findExcludeExceedTwelveMonth();
}
