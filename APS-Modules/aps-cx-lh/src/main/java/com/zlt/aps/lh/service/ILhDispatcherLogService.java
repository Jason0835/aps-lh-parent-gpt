package com.zlt.aps.lh.service;


import com.zlt.aps.lh.api.domain.entity.LhDispatcherLog;
import com.zlt.aps.lh.api.domain.vo.LhDispatcherLogVo;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhDispatcherLogService.java
 * 描    述：ILhDispatcherLogService硫化调度员排程操作日志后端接口
 *@author zlt
 *@date 2025-03-21
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ILhDispatcherLogService{

    /**
     * 列表查询
     */
    List<LhDispatcherLog> selectList(LhDispatcherLog queryVO);

    /**
     * 插入
     * @param entity
     */
    void insert(LhDispatcherLog entity);

    /**
     * 查询是否有变更列表
     *
     * @param queryVO        查询参数
     * @param scheduleIdList 排程ID列表
     * @return 结果
     */
    List<LhDispatcherLogVo> selectIsChangeList(LhDispatcherLog queryVO, List<Long> scheduleIdList);
}
