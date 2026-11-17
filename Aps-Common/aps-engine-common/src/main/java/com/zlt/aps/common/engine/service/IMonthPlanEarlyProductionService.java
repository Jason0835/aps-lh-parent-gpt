package com.zlt.aps.common.engine.service;

import com.zlt.aps.common.engine.domain.dto.MonthPlanFirstDateDTO;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 查询窗口外允许提前范围内的首个正计划量日期，不处理工序准入和资源分配。
 *
 * @author APS
 * @date 2026/11/16
 */
public interface IMonthPlanEarlyProductionService {

    /**
     * 批量查询有效定稿月计划，按物料与产品状态返回首个正计划量日期。
     * <p>范围为窗口结束日后一天至结束日加提前天数，两端均包含；排程窗口固定为结束日前两日至结束日，窗口内有正日量的物料状态不返回。
     * 同月存在多条定稿版本时按更新时间、ID 倒序取首条，缺少有效版本的月份无结果。</p>
     *
     * @param factoryCode 工厂编码，不能为空白
     * @param windowEndDate 排程窗口结束自然日
     * @param advanceDays 调用方已解析的提前天数，本服务不设置工序默认值
     * @return 匹配列表；工厂或日期缺失、天数非正或无正计划时返回空列表
     */
    List<MonthPlanFirstDateDTO> listFirstPlanDates(String factoryCode, LocalDate windowEndDate, int advanceDays);

    /**
     * 使用调用方已加载的有效定稿计划快照计算日期，不查询数据库或修改输入。
     * <p>调用方负责版本有效性以及固定三天窗口和提前范围的月份覆盖；本方法按行所属年月读取原始 dayN，
     * 窗口内任一行有正日量时排除整组，否则多行未来正计划取最早日期，空白状态独立计算。</p>
     *
     * @param factoryCode 工厂编码，过滤其他工厂记录
     * @param windowEndDate 排程窗口结束自然日，不包含在允许提前范围内
     * @param advanceDays 调用方已解析的提前天数，截止日包含在范围内
     * @param monthPlans 已加载的有效定稿月计划，允许跨月、跨年
     * @return 匹配列表；参数缺失、天数非正或无正计划时返回空列表
     */
    List<MonthPlanFirstDateDTO> calculateFirstPlanDates(String factoryCode, LocalDate windowEndDate,
                                                      int advanceDays,
                                                      List<FactoryMonthPlanProductionFinalResult> monthPlans);
}
