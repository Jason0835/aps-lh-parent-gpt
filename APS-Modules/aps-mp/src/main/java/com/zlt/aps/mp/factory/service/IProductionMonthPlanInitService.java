package com.zlt.aps.mp.factory.service;


import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductionMonthPlanInitService.java
 * 描    述：IProductionMonthPlanInitService工厂月计划初始化-业务接口定义
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251205
 */
public interface IProductionMonthPlanInitService {

    /**
     * 根据查询条件，获取工厂月计划排产版本对应的初始化信息
     *
     * @param condition
     * @return
     */
    List<ProductionMonthPlanInit> getDataList(ProductionMonthPlanInit condition);
}
