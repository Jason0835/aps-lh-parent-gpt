package com.zlt.aps.lh.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.AutoLhScheduleResultDTO;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ILhMoldChangePlanService.java
 * 描    述：ILhMoldChangePlanService模具变动单后端接口
 *@author zlt
 *@date 2025-02-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
public interface ILhMoldChangePlanService  extends IDocService<LhMoldChangePlan>{


    /**
     *  根据条件查询List
     * @param queryWrapper
     * @return
     */
    List<LhMoldChangePlan> selectList(QueryWrapper<LhMoldChangePlan> queryWrapper);


    /**
     * 生成换模计划
     * @param queryVO
     */
    void moldReplacementPlan(LhMoldChangePlan queryVO);

    /**
     * 生成换模计划
     * @param before
     * @param after
     * @return
     */
    LhMoldChangePlan genMoldChangePlan(LhScheduleResult before, LhScheduleResult after);

    /**
     * 批量插入
     * @param list
     */
    void insertList(List<LhMoldChangePlan> list);


    /**
     * 导入换模数据
     * @param list 导入集合
     * @param importLogId 日志ID
     * @param scheduleDate 导入日期
     * @return 返回结果
     */
    AjaxResult importMoldChangePlan(List<LhMoldChangePlan> list, Long importLogId, Date scheduleDate);

}
