package com.zlt.aps.maindata.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MdmSkuStructureRef;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmSkuStructureRefService.java
 * 描    述：IMdmSkuStructureRefServiceSKU与结构关系后端接口
 *@author zlt
 *@date 2025-12-06
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMdmSkuStructureRefService  extends IDocService<MdmSkuStructureRef> {

    /**
     * 更新结构到物料
     * @param queryVO
     * @return
     */
    AjaxResult updateStructureToMaterial(MdmSkuStructureRef queryVO);

}
