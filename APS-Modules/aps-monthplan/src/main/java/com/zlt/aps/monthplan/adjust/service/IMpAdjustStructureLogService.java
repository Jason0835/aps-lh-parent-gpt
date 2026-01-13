package com.zlt.aps.monthplan.adjust.service;


import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureLog;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureLogService.java
 * 描    述：IMpAdjustStructureLogService调整-操作日志后端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpAdjustStructureLogService  extends IDocService<MpAdjustStructureLog>{

    /**
     * 通过版本删除调整日志
     * @param factoryCode 工厂
     * @param year 年份
     * @param month 月份
     * @param version 调整版本
     */
    void deleteAdjustLogByVersion(String factoryCode,String year,String month,String version);
}
