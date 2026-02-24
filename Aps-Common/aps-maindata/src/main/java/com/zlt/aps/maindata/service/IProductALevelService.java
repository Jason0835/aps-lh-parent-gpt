package com.zlt.aps.maindata.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.ProductALevel;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductALevelService.java
 * 描    述：IProductALevelService基础数据-SAP-OEE率后端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
public interface IProductALevelService extends IDocService<ProductALevel> {

    List<ProductALevel> selectDocProductALevelList(ProductALevel productALevel);

    /**
     * 获取折损率
     *
     * @param productALevel
     * @return
     */
    List<ProductALevelVo> getProductALevelList(ProductALevel productALevel);

    /**
     * 不备货
     * @param ids 选中的数据
     * @param year 年
     * @param month 月
     * @return 结果
     */
    AjaxResult noStockUp(List<Long> ids, Integer year, Integer month);
}
