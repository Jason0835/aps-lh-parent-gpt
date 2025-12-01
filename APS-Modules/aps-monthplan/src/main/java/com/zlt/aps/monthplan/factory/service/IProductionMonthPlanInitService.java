package com.zlt.aps.monthplan.factory.service;


import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductionMonthPlanInitService.java
 * 描    述：IProductionMonthPlanInitService分厂月生产计划排产过程-计划初始化后端接口
 *@author zlt
 *@date 2025-03-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IProductionMonthPlanInitService{

    /**
     * 列表查询
     */
    List<ProductionMonthPlanInit> selectList(ProductionMonthPlanInit queryVO);
}
