package com.zlt.aps.mp.adjust.service;


import com.zlt.aps.mp.api.domain.entity.MpAdjustMaterialLog;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustMaterialLogService.java
 * 描    述：IMpAdjustMaterialLogServiceS2-0808.调整-调整日志（未调整及已调整）后端接口
 *@author zlt
 *@date 2026-02-09
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpAdjustMaterialLogService  extends IDocService<MpAdjustMaterialLog>{

    /**
     * 通过版本删除调整过程日志
     * @param factoryCode 工厂
     * @param year 年份
     * @param month 月份
     * @param version 调整版本
     */
    void deleteAdjustProcLogByVersion(String factoryCode,String year,String month,String version);
}
