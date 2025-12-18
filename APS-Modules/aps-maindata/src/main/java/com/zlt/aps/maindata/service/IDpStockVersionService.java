package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpStockVersion;
import com.zlt.aps.monthplan.api.domain.entity.MpFinishedProductStock;
import com.zlt.bill.common.service.IDocService;

import java.util.List;
import java.util.Map;

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
public interface IDpStockVersionService extends IDocService<DpStockVersion> {

    /**
     * 查询MES实时成品库存列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<DpStockVersion> list4Mes(DpStockVersion queryVO);
    /**
     * 将分配时的成品库存记录到库存版本表中(以需求版本号的维度)；
     * @param createCondition 需求参数
     * @param monthPlanVersion 需求版本号
     * @param finishedProductStockMap 成品库存记录
     */
    void insertBatchData(DpDemandPlan createCondition,String monthPlanVersion, Map<String, List<MpFinishedProductStock>> finishedProductStockMap);
}
