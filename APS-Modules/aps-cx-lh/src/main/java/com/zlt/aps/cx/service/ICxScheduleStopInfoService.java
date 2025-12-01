package com.zlt.aps.cx.service;



import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleStopInfo;
import com.zlt.aps.cxlh.cx.api.domain.vo.LhAlgorithmScheduleResultDto;
import com.zlt.bill.common.service.IDocService;
/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxScheduleStopInfoService.java
 * 描    述：ICxScheduleStopInfoService成型机台自动停排信息后端接口
 *@author zlt
 *@date 2025-03-11
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ICxScheduleStopInfoService  extends IDocService<CxScheduleStopInfo>{

    /**
     * 创建成型机台自动停排信息
     * @param item 任务对象
     * @return 成型机台自动停排信息
     */
    public CxScheduleStopInfo createCxScheduleStopInfo(LhAlgorithmScheduleResultDto item);
}
