package com.zlt.aps.mdm.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmSkuConstructionRefService.java
 * 描    述：IMdmSkuConstructionRefServiceSKU与施工（示方书）关系后端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMdmSkuConstructionRefService  extends IDocService<MdmSkuConstructionRef>{

    /**
     * 更新胎胚描述到物料表
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    AjaxResult updateMainMaterialDescToMaterialInfo(MdmSkuConstructionRef queryVO);
}
