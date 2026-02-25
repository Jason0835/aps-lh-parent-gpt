package com.zlt.aps.mp.adjust.service;


import com.zlt.aps.mp.api.domain.dto.MpRollAdjustContextDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureOut;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpAdjustStructureOutService.java
 * 描    述：IMpAdjustStructureOutService调整-结构调整记录后端接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMpAdjustStructureOutService  extends IDocService<MpAdjustStructureOut>{

    /**
     * 查询结构列表
     * @param contextDTO
     * @return
     */
    List<MpAdjustStructureOut> selectMpAdjustStructureOutList(MpRollAdjustContextDTO contextDTO);

}
