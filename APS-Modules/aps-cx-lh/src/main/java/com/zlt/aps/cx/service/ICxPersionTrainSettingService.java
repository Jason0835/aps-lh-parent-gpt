package com.zlt.aps.cx.service;



import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxPersionTrainSetting;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ICxPersionTrainSettingService.java
 * 描    述：ICxPersionTrainSettingService成型工序开机档数后端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ICxPersionTrainSettingService  extends IDocService<CxPersionTrainSetting>{

    /**
     * 查询列表
     */
    List<CxPersionTrainSetting> selectList(CxPersionTrainSetting queryVO);

    /**
     * 详情
     */
    CxPersionTrainSetting getInfo(Long billId);

    /**
     * 列表校验唯一并保存
     */
    AjaxResult saveList(List<CxPersionTrainSetting> list);
}
