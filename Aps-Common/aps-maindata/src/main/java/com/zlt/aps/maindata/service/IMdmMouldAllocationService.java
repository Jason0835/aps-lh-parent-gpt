package com.zlt.aps.maindata.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MdmMouldAllocation;
import com.zlt.aps.mp.api.domain.vo.PeriodInfo;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMouldAllocationService.java
 * 描    述：IMdmMouldAllocationService模具分配比例(同结构/不同结构)后端接口
 *@author zlt
 *@date 2025-12-14
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMdmMouldAllocationService  extends IDocService<MdmMouldAllocation>{

    /**
     * 复制模具分配比例
     */
    AjaxResult copy(PeriodInfo vo);

}
