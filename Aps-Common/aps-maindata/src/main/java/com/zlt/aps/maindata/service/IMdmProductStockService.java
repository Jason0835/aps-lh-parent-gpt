package com.zlt.aps.maindata.service;

import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.aps.mp.api.domain.entity.MdmProductStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Set;

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
    List<MdmProductStock> findCurrentFinishStock(String factoryCode);

    /**
     * 根据物料编号获取成品库存
     *
     * @param materialCode 自定义工具栏…
     * @return 成品库存
     */
    List<MdmProductStock> getMpFinishedProductStockByMaterialCode(String materialCode);
    /**
     * 获取成品库存
     * @param factoryCode
     * @param skus
     * @return
     */
    List<MdmProductStock> findCurrentFinishStock(String factoryCode, Set<String> skus);
    
    /**
     * 库存冲减未扫描订单
     * 
     * @param finishedProductStocks 成品库存列表
     * @param notScanOrderList      未扫描订单列表
     */
    void reduceInventoryByNotScanOrder(List<MdmProductStock> finishedProductStocks, List<MdmOutbountOrdersNotScan> notScanOrderList);
}
