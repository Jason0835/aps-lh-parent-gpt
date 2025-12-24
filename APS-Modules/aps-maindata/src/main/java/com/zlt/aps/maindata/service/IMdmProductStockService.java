package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmProductStockService.java
 * 描    述：IMdmProductStockService成品库存后端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-22
 */
public interface IMdmProductStockService extends IDocService<MdmProductStock> {

    /**
     * 查询成品库存表，汇总计算超期12个月的库存数、超期6个月的库存数、超期3个月的库存数
     *
     * @return
     */
    List<MdmProductStock> findCurrentFinishStock();

    /**
     * 根据物料编号获取成品库存
     *
     * @param materialCode 自定义工具栏…
     * @return 成品库存
     */
    List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode);
}
