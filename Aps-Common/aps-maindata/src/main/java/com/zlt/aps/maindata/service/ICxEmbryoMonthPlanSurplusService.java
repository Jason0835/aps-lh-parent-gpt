package com.zlt.aps.maindata.service;



import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.zlt.aps.mp.api.domain.entity.CxEmbryoMonthPlanSurplus;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.bill.common.service.IDocService;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxEmbryoMonthPlanSurplusService.java
 * 描    述：ICxEmbryoMonthPlanSurplusService成型工序胎胚计划量汇总表后端接口
 *@author zlt
 *@date 2025-03-07
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ICxEmbryoMonthPlanSurplusService extends IDocService<CxEmbryoMonthPlanSurplus>{

    /**
     * 列表查询
     * @param queryVO 查询实体
     * @return List<CxEmbryoMonthPlanSurplus> 查询结果
     */
    List<CxEmbryoMonthPlanSurplus> selectList(CxEmbryoMonthPlanSurplus queryVO);

    /**
     * 构建查询条件
     * @param queryWrapper 查询对象
     * @param queryVO 查询实体
     */
    public void builderCondition(QueryWrapper<CxEmbryoMonthPlanSurplus> queryWrapper, CxEmbryoMonthPlanSurplus queryVO);


    /**
     * 批量保存或更新
     * @param lhMonthPlanSurpluses 保存或更新对象
     */
    void batchSaveOrUpdate(ArrayList<LhMonthPlanSurplus> lhMonthPlanSurpluses);
}
