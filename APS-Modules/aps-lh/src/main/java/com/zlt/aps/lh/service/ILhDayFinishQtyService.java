package com.zlt.aps.lh.service;


import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhDayFinishQtyService.java
 * 描    述：ILhDayFinishQtyService硫化排程日完成量后端接口
 *@author zlt
 *@date 2025-02-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ILhDayFinishQtyService  extends IDocService<LhDayFinishQty>{

    /**
     * 导入数据，并保存记录
     *
     * @param list        要导入数据
     * @param importLogId 导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importFinishQty(List<LhDayFinishQty> list, Long importLogId);

    /**
     * 更新月计划的硫化剩余量
     */
    void updateMonthPlanSurplus(String factoryCode,Date scheduleDate);
}
