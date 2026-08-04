package com.zlt.aps.mp.adjust.mapper;

import com.zlt.aps.mp.api.domain.dto.StockCaptureDateDTO;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustResultMapper.java
 * 描    述：调整-调整结果记录Mapper接口
 *@author zlt
 *@date 2025-12-19
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Mapper
public interface MpAdjustResultEntityMapper extends CommBaseMapper<MpAdjustResult> {

    /**
     * 动态更新（只更新非空字段）
     * @param entity 实体（必须包含 id）
     * @return 影响行数
     */
    int forceUpdateById(MpAdjustResult entity);

    /**
     * 通过版本删除调整结果
     * @param factoryCode
     * @param year
     * @param month
     * @param version
     */
    void deleteAdjustResultByVersion(@Param("factoryCode") String factoryCode,
                                            @Param("year") String year,
                                            @Param("month") String month,
                                            @Param("version") String version,
                                            @Param("structureName") String structureName);

    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<MpAdjustResult> getVersionList(MpAdjustResult queryVO);

    /**
     * 查询调整版本列表
     * @param queryVO 查询参数
     * @return 调整版本列表
     */
    List<MpAdjustResult> getAdjustVersionList(MpAdjustResult queryVO);

    /**
     * 批量更新超欠产有效标识
     * @param list 调整结果列表
     */
    void updateValidFlagBatchById(@Param("list") List<MpAdjustResult> list);

    /**
     * 检查数据来源月是否存在 ADJ 前缀版本号的调整结果
     * 用于判断是否需要对调整结果表同步更新超欠产
     *
     * @param lastYear  数据来源月年份
     * @param lastMonth 数据来源月月份
     * @return 存在 ADJ 前缀版本号返回 true，否则 false
     */
    Boolean existsAdjVersionResult(@Param("lastYear") Integer lastYear,
                                   @Param("lastMonth") Integer lastMonth);

    /**
     * 计算上月超欠产并回填到当月调整结果表
     * 计算逻辑同定稿表 updateLastMonthOverProd，区别：
     *   1. 写入目标表为 T_MP_ADJUST_RESULT
     *   2. 数据来源取上月调整结果表 DAY_x
     *   3. 当月版本号从 T_MP_ADJUST_RESULT 取 MAX(VERSION)（ADJ前缀）
     *   4. 按 (分厂+物料+产品状态+VERSION=当月ADJ版本号) 维度匹配更新
     *
     * @param lastYear                 数据来源月年份
     * @param lastMonth                数据来源月月份
     * @param currentYear              写入目标月年份
     * @param currentMonth             写入目标月月份
     * @param startDate                数据来源月开始日期
     * @param endDate                  数据来源月结束日期
     * @param overdueThresholdParamCode 超欠产有效标志判定阈值参数编码
     * @param stockCaptureDateList     Java 层计算好的库存抓取日列表
     * @return 更新记录数
     */
    int updateLastMonthOverProdForAdjust(@Param("lastYear") Integer lastYear,
                                         @Param("lastMonth") Integer lastMonth,
                                         @Param("currentYear") Integer currentYear,
                                         @Param("currentMonth") Integer currentMonth,
                                         @Param("startDate") Date startDate,
                                         @Param("endDate") Date endDate,
                                         @Param("overdueThresholdParamCode") String overdueThresholdParamCode,
            @Param("stockCaptureDateList") List<StockCaptureDateDTO> stockCaptureDateList);

    /**
     * 定稿时补更新当月调整结果表的上月超欠产有效标识（只更新标识，不更新值）
     * 逻辑同定稿表 updateLastMonthOverProdFlag，区别：
     *   1. 写入目标表为 T_MP_ADJUST_RESULT
     *   2. 数据来源取上月调整结果表
     *   3. 当月版本号从 T_MP_ADJUST_RESULT 取 MAX(VERSION)（ADJ前缀）
     *
     * @param lastYear                 数据来源月年份
     * @param lastMonth                数据来源月月份
     * @param currentYear              写入目标月年份
     * @param currentMonth             写入目标月月份
     * @param startDate                数据来源月开始日期
     * @param endDate                  数据来源月结束日期
     * @param overdueThresholdParamCode 超欠产有效标志判定阈值参数编码
     * @param stockCaptureDateList     Java 层计算好的库存抓取日列表
     * @return 更新记录数
     */
    int updateLastMonthOverProdFlagForAdjust(@Param("lastYear") Integer lastYear,
                                             @Param("lastMonth") Integer lastMonth,
                                             @Param("currentYear") Integer currentYear,
                                             @Param("currentMonth") Integer currentMonth,
                                             @Param("startDate") Date startDate,
                                             @Param("endDate") Date endDate,
                                             @Param("overdueThresholdParamCode") String overdueThresholdParamCode,
                                             @Param("stockCaptureDateList") List<StockCaptureDateDTO> stockCaptureDateList);

}
