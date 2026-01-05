package com.zlt.aps.itf.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.itf.vo.CxMonthPlanIssue;
import com.zlt.aps.itf.vo.MonthPlanIssue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanIssueMapper.java
 * 描    述：月计划下发Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-24
 */
@Mapper
public interface MonthPlanIssueEntityMapper extends BaseMapper<MonthPlanIssue> {

    /**
     * 批量新增
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertMonthPlanIssue(@Param("list") List<MonthPlanIssue> list);

    /**
     * 根据工单号批量更新
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpdateMonthPlanIssue(@Param("list") List<MonthPlanIssue> list);

    /**
     * 批量新增
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchInsertCxMonthPlanIssue(@Param("list") List<CxMonthPlanIssue> list);

    /**
     * 根据工单号批量更新
     *
     * @param list 数据列表
     * @return 影响行数
     */
    int batchUpdateCxMonthPlanIssue(@Param("list") List<CxMonthPlanIssue> list);
}
