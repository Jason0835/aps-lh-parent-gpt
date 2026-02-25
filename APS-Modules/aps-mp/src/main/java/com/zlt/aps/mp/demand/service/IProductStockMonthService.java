package com.zlt.aps.mp.demand.service;


import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.ProductStockMonth;
import com.zlt.aps.mp.api.domain.vo.MonthPlanSaleRequirePlanVo;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductStockMonthService.java
 * 描    述：IProductStockMonthService物料月库存信息后端接口
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
public interface IProductStockMonthService {
    /**
     * 根据分厂、年份、月份获取大于0的月度库存信息
     *
     * @param condition
     * @return
     */
    List<ProductStockMonth> getMothStock(MonthPlanSaleRequirePlanVo condition);

    /**
     * 列表查询
     */
    List<ProductStockMonth> selectList(ProductStockMonth queryVO);

    /**
     * 导入数据
     */
    AjaxResult doImportData(List<ProductStockMonth> list, boolean updateSupport, long importLogId);

    
    void importDataAsync(List<ProductStockMonth> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes);
}
