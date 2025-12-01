package com.zlt.aps.maindata.service;

import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmModelInfoService.java
 * 描    述：IMdmModelInfoService模具信息后端接口
 *@author zlt
 *@date 2025-02-24
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMdmModelInfoService  extends IDocService<MdmModelInfo>{

    /**
     * 赋值寸口
     * @param list 列表
     */
    void setProSize(List<MdmModelInfo> list);
}
