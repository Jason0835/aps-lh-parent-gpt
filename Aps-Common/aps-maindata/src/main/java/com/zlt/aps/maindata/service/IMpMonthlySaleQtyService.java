package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMonthlySaleQtyService.java
 * 描    述：IMpMonthlySaleQtyService月均销量后端接口
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
public interface IMpMonthlySaleQtyService extends IDocService<MpMonthlySaleQty> {

    /**
     * 生成月均销量
     *
     * @param mpMonthlySaleQty 参数
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult genMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty);

    /**
     * 查询SCM发货明细，根据SKU+区域汇总发货量，写入历史销售记录表
     * @param factoryCode 分厂编号
     * @param nowDate 当前日期
     * @param lastYear 上个月对应年份
     * @param lastMonth 上个月
     * @return 结果
     */
    AjaxResult genMpHistorySaleRecord(String factoryCode, Date nowDate, int lastYear, String lastMonth);

    /**
     * 查询当前月均销量
     * @return 查询当前月均销量
     */
    List<MpMonthlySaleQty> findCurrentMonthlySaleQty(String factoryCode, Set<String> skus);
    /**
     * 根据物料编码获取月均销量
     * @param supplyOrderPool 参数
     * @return  月均销量
     */
    MpMonthlySaleQty getMpMonthlySaleQtyByMaterialCode(SupplyOrderPool supplyOrderPool);
    /**
     * 根据编码获取月均销量
     * @return
     */
    Map<String, Integer> findCurrentMonthlySaleQty(String factoryCode);
    /**
     * 获取调整月均销量
     * @return 月均销量
     */
    Map<String, Integer> findAdjustMonthlySaleQty(DpDemandPlan createCondition, Set<String> eligibleSkus);
}
