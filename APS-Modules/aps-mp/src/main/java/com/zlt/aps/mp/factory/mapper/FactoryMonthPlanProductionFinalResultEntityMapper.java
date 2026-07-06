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
     * 匹配维度：按 (分厂+物料编码+产品状态) 三字段维度匹配回填
     * 有效标志判定：|超欠产值|(绝对值)大于阈值参数则置否('0')，否则置是('1')；
     * 月底余量为空时按0处理，超欠产 = 0 - 已完成量，统一走阈值判定
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
     * 查询上月定稿存在但当月定稿缺失的超欠产记录（用于 Java 层补充工单号后批量插入）
     * <p>
     * 场景：上月定稿表中按 (分厂+物料编码+产品状态) 维度存在，但当月定稿表中按相同维度匹配不到的记录，
     * 需要新增一条到当月定稿表，把上月超欠产值及有效标志写入。
     * </p>
     * 规则：
     *   1. 数据来源：上月定稿表 f5 LEFT JOIN T_MDM_MONTH_SURPLUS / T_LH_DAY_FINISH_QTY，
     *      按 (分厂+物料+产品状态) 维度聚合计算超欠产 = 计划量 - 完成量
     *      计划量 = 从 STOCK_CAPTURE_DATE 日号到月底累加 DAY_x（为空时累加 DAY_1~DAY_31 全部）
     *   2. 过滤1：当月定稿表按相同维度已存在的记录不插入（exist.ID IS NULL）
     *   3. 过滤2：超欠产值 = 0 的记录不插入（OVERDUE_QTY &lt;&gt; 0）
     *   4. 过滤3：当月无定稿版本的工厂不插入（T_MP_PROC_VERSION 无 IS_FINAL='1' 记录，INNER JOIN 自动排除）
     *   5. 字段映射：
     *      - 年月取当月（currentYear、currentMonth、YEAR_MONTH=year*100+month）
     *      - 需求版本号(MONTH_PLAN_VERSION)、排产版本号(PRODUCTION_VERSION) 取自 T_MP_PROC_VERSION
     *        当月定稿版本（IS_FINAL='1'）；最新需求版本号(LAST_MONTH_PLAN_VERSION) 置 NULL
     *      - PRODUCTION_NO 工单号不在此查询赋值，由 Java 层按当月最大值+1 递增生成后填充
     *      - DAY_1~DAY_31 全部置 NULL（不复制上月数据、不赋值）
     *      - 产量/调整相关字段置 0：HEIGHT_PRODUCTION_QTY、TOTAL_QTY、ORIGINAL_TOTAL_QTY、
     *        MID_PRODUCTION_QTY、CYCLE_PRODUCTION_QTY、CONVENTION_PRODUCTION_QTY、
     *        POSTPONE_PRODUCTION_QTY、TRIAL_PRODUCTION_QTY、DIFFERENCE_QTY、
     *        ADJUST_QTY1~4、BEGIN_DAY、END_DAY
     *      - 其他业务字段（产品品类、物料、产品状态、胎胚号、品牌、规格、施工阶段等）从上月定稿复制
     *      - LAST_MONTH_OVERDUE_QTY / LAST_MONTH_VALID_FLAG 按超欠产值和阈值参数判定写入
     *
     * @param lastYear                 上月年份
     * @param lastMonth                上月月份
     * @param currentYear              当月年份
     * @param currentMonth             当月月份
     * @param startDate                上月开始日期（用于 STOCK_CAPTURE_DATE 为空时回退）
     * @param endDate                  上月结束日期（月底边界）
     * @param overdueThresholdParamCode 超欠产有效标志判定阈值参数编码
     * @return 待插入的记录列表（不含工单号，需 Java 层填充）
     */
    List<FactoryMonthPlanProductionFinalResult> selectMissingLastMonthOverProd(@Param("lastYear") Integer lastYear,
                                                                               @Param("lastMonth") Integer lastMonth,
                                                                               @Param("currentYear") Integer currentYear,
                                                                               @Param("currentMonth") Integer currentMonth,
                                                                               @Param("startDate") Date startDate,
                                                                               @Param("endDate") Date endDate,
                                                                               @Param("overdueThresholdParamCode") String overdueThresholdParamCode);

    /**
     * 查询当月定稿表 PRODUCTION_NO 工单号最大值（用于 Java 层递增生成新工单号）
     * <p>
     * 工单号格式：MP + yyMMdd + 2位批次号 + 5位序列号 = 15位，如 MP2607020100257
     * 按 (分厂+年+月) 维度取最大值；返回最大工单号字符串，Java 层解析数字部分 +1 递增
     * </p>
     *
     * @param factoryCode  分厂编号
     * @param currentYear  当月年份
     * @param currentMonth 当月月份
     * @return 当月定稿表该分厂 PRODUCTION_NO 最大值；无记录时返回 null
     */
    String selectMaxProductionNo(@Param("factoryCode") String factoryCode,
                                 @Param("currentYear") Integer currentYear,
                                 @Param("currentMonth") Integer currentMonth);

    /**
     * 定稿时补更新上月定稿记录的上月超欠产有效标识（只更新标识，不更新值）
     * 计算逻辑同 {@link #updateLastMonthOverProd}，仅 SET 子句移除 LAST_MONTH_OVERDUE_QTY，
     * 保留 LAST_MONTH_VALID_FLAG 的阈值判定，确保标识判定结果与定时任务一致。
     * 匹配维度：按 (分厂+物料编码+产品状态) 三字段维度匹配回填。
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
