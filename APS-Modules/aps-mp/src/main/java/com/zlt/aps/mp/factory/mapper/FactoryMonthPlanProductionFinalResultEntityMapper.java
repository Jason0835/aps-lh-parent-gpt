package com.zlt.aps.mp.factory.mapper;

import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.vo.FactoryMonthPlanFinalAdjustVo;
import com.zlt.core.dao.basemapper.CommBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryMonthPlanProductionFinalResultMapper.java
 * 描    述：工厂月生产计划-最终排产计划定稿Mapper接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-23
 */
@Mapper
public interface FactoryMonthPlanProductionFinalResultEntityMapper extends CommBaseMapper<FactoryMonthPlanProductionFinalResult> {
    /**
     * 查询版本列表
     * @param queryVO 查询参数
     * @return 结果
     */
    List<FactoryMonthPlanProductionFinalResult> getVersionList(FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 查询最终排产计划定稿列表-调整使用
     * @param queryVO 查询条件
     * @return 结果
     */
    List<FactoryMonthPlanFinalAdjustVo> list4Adjust(FactoryMonthPlanProductionFinalResult queryVO);

    /**
     * 计算上月超欠产并回填到当月定稿表
     * 新公式：上月超欠产 = 定稿需求版本对应的月计划月底余量 - (库存抓取日 ~ 月底)的硫化日完成量
     * 数据来源：
     *   1. 月计划月底余量(PLAN_SURPLUS_QTY)、库存抓取日(STOCK_CAPTURE_DATE)：取自 T_MDM_MONTH_SURPLUS，
     *      按 (分厂+物料+年+月+需求版本号 MONTH_PLAN_VERSION=REQUIRE_VERSION) 匹配
     *   2. 已完成量：取自 T_LH_DAY_FINISH_QTY，日期范围 = IFNULL(STOCK_CAPTURE_DATE, startDate) ~ endDate
     * 有效标志判定：|超欠产值|(绝对值)大于阈值参数则置否('0')，否则置是('1')；
     * 无月底余量记录时值置NULL、标志置否('0')
     *
     * @param lastYear                 上月年份
     * @param lastMonth                上月月份
     * @param currentYear              当月年份
     * @param currentMonth             当月月份
     * @param startDate                上月开始日期（用于 STOCK_CAPTURE_DATE 为空时回退）
     * @param endDate                  上月结束日期（月底边界）
     * @param overdueThresholdParamCode 超欠产有效标志判定阈值参数编码
     * @return 更新记录数
     */
    int updateLastMonthOverProd(@Param("lastYear") Integer lastYear,
                                @Param("lastMonth") Integer lastMonth,
                                @Param("currentYear") Integer currentYear,
                                @Param("currentMonth") Integer currentMonth,
                                @Param("startDate") Date startDate,
                                @Param("endDate") Date endDate,
                                @Param("overdueThresholdParamCode") String overdueThresholdParamCode);

    /**
     * 定稿时补更新上月定稿记录的上月超欠产有效标识（只更新标识，不更新值）
     * 计算逻辑同 {@link #updateLastMonthOverProd}，仅 SET 子句移除 LAST_MONTH_OVERDUE_QTY，
     * 保留 LAST_MONTH_VALID_FLAG 的阈值判定，确保标识判定结果与定时任务一致。
     * 场景：次月定稿时（如6.25定稿7月），用上月（6月）数据补更新上月（6月）定稿记录的标识，
     * 用于覆盖月初定时任务（6.1用5月数据）写入的旧标识，使标识反映最新的上月完成情况。
     *
     * @param lastYear                 数据来源月份年份（与写入目标月相同，即定稿月的上月）
     * @param lastMonth                数据来源月份月份
     * @param currentYear              写入目标月份年份（与数据来源月相同）
     * @param currentMonth             写入目标月份月份
     * @param startDate                数据来源月开始日期（用于 STOCK_CAPTURE_DATE 为空时回退）
     * @param endDate                  数据来源月结束日期（月底边界）
     * @param overdueThresholdParamCode 超欠产有效标志判定阈值参数编码
     * @return 更新记录数
     */
    int updateLastMonthOverProdFlag(@Param("lastYear") Integer lastYear,
                                    @Param("lastMonth") Integer lastMonth,
                                    @Param("currentYear") Integer currentYear,
                                    @Param("currentMonth") Integer currentMonth,
                                    @Param("startDate") Date startDate,
                                    @Param("endDate") Date endDate,
                                    @Param("overdueThresholdParamCode") String overdueThresholdParamCode);
}
