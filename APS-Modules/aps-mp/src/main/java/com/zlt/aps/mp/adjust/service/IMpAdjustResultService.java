package com.zlt.aps.mp.adjust.service;


import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustResultService.java
 * 描    述：IMpAdjustResultService调整-调整结果记录后端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpAdjustResultService  extends IDocService<MpAdjustResult>{

    /**
     * 通过ID,强制更新调整结果
     * @param entity 工厂
     */
    void forceUpdateById(MpAdjustResult entity);

    /**
     * 通过版本删除调整结果
     * @param factoryCode 工厂
     * @param year 年份
     * @param month 月份
     * @param version 调整版本
     */
    void deleteAdjustResultByVersion(String factoryCode,String year,String month,String version,String structureName);
    
    /**
     * 数据导入
     * @param fileBytes
     * @param importLog
     * @return
     */
    AjaxResult importData(byte[] fileBytes, ImportLog importLog);

    /**
     * 批量更新超欠产有效标识
     * @param list 调整结果列表
     */
    void updateValidFlagBatchById(List<MpAdjustResult> list);
}
