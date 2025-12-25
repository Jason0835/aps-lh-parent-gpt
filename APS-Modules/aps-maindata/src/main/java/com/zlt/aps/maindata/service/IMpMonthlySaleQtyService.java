package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
import com.zlt.bill.common.service.IDocService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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
     * 查询当前月均销量
     * @return 查询当前月均销量
     */
    List<MpMonthlySaleQty> findCurrentMonthlySaleQty();
    /**
     * 根据物料编码获取月均销量
     * @param materialCode
     * @return
     */
    MpMonthlySaleQty getMpMonthlySaleQtyByMaterialCode(String materialCode);
    /**
     * 根据编码获取月均销量
     * @return
     */
    Map<String,Long> findMonthlySaleQtyGroupByMaterialCode();
}
