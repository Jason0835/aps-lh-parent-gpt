package com.zlt.aps.lh.service;


import com.zlt.aps.lh.api.domain.entity.MesLhScheduleResult;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMesLhScheduleResultService.java
 * 描    述：IMesLhScheduleResultService硫化排程下发接口后端接口
 *@author zlt
 *@date 2025-03-18
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface IMesLhScheduleResultService{

    /**
     * 列表查询
     */
    List<MesLhScheduleResult> selectList(MesLhScheduleResult queryVO);
}
