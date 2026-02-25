package com.zlt.aps.maindata.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zlt.aps.mp.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.mp.api.domain.vo.PlanOrderSortConfigurationVo;

import java.util.List;
import java.util.Map;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IPlanOrderSortConfigurationService.java
 * 描    述：IPlanOrderSortConfigurationService业务排序配置后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
public interface IPlanOrderSortConfigurationService extends IService<PlanOrderSortConfiguration> {
    /**
     * 获取库存对冲的排序顺序配置
     * 包含第一、第二顺序
     *
     * @return
     */
    List<PlanOrderSortConfiguration> getStockHedgingConfiguration();

    /**
     * 获取分厂排程-排产顺序配置
     *
     * @param factoryCode 分厂编码
     * @return
     */
    List<PlanOrderSortConfiguration> getProductionConfiguration(String factoryCode);

    /**
     * 获取库存对冲的第一顺序
     *
     * @return
     */
    List<PlanOrderSortConfiguration> getStockHedgingFirstSortConfiguration();

    /**
     * 获取库存对冲的第二顺序
     *
     * @return
     */
    List<PlanOrderSortConfiguration> getStockHedgingSecondSortConfiguration();

    /**
     * 获取库存对冲页面数据
     *
     * @return
     */
    Map<Integer, List<PlanOrderSortConfiguration>> getStockHedgingConfigurationList();

    /**
     * 获取月份排产配置页面数据
     *
     * @return
     */
    Map<Integer, List<PlanOrderSortConfiguration>> getPlanOrderSortConfigurationList();

    /**
     * 保存库存对冲顺序配置
     *
     * @param planOrderSortConfigurationVo
     */
    void saveStockHedgingConfiguration(PlanOrderSortConfigurationVo planOrderSortConfigurationVo);

    /**
     * 保存月份排产顺序配置
     *
     * @param planOrderSortConfigurationVo
     */
    void savePlanOrderConfiguration(PlanOrderSortConfigurationVo planOrderSortConfigurationVo);
}
