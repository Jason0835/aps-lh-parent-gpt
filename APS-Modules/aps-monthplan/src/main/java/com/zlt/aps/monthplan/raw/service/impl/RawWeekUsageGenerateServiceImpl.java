package com.zlt.aps.monthplan.raw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.RawWeekUsageEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
@Slf4j
public class RawWeekUsageGenerateServiceImpl {

    @Autowired
    private FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    @Autowired
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Autowired
    private RawWeekUsageEntityMapper rawWeekUsageEntityMapper;


    /**
     * 生成周维度原材料用量记录
     * @param factoryCode 工厂编码
     * @param year 年份
     * @param month 月份
     * @return 生成结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateWeekUsage(String factoryCode, Integer year, Integer month) {
        try {
            log.info("开始生成周维度原材料用量记录，工厂：{}，年份：{}，月份：{}", factoryCode, year, month);

            // 1. 获取该月的生产计划
            QueryWrapper<FactoryMonthPlanProdFinal> planWrapper = new QueryWrapper<>();
            planWrapper.eq("FACTORY_CODE", factoryCode);
            planWrapper.eq("YEAR", year);
            planWrapper.eq("MONTH", month);
            List<FactoryMonthPlanProdFinal> monthPlans = factoryMonthPlanProdFinalMapper.selectList(planWrapper);

            if (monthPlans.isEmpty()) {
                log.warn("未找到月生产计划，工厂：{}，年份：{}，月份：{}", factoryCode, year, month);
                return AjaxResult.error(String.format("未找到月生产计划，工厂：%s，年份：%d，月份：%d", factoryCode, year, month));
            }

            // 2. 按周分组生产计划
            Map<Integer, List<FactoryMonthPlanProdFinal>> plansByWeek = groupPlansByWeek(monthPlans, year, month);

            // 3. 删除旧的周用量记录
            deleteExistingWeekUsage(factoryCode, year, month);

            // 4. 计算并保存每周的原材料用量
            int totalRecords = 0;
            for (Map.Entry<Integer, List<FactoryMonthPlanProdFinal>> entry : plansByWeek.entrySet()) {
                Integer week = entry.getKey();
                List<FactoryMonthPlanProdFinal> weekPlans = entry.getValue();

                // 计算该周的原材料计划用量
                Map<String, BigDecimal> weekMaterialUsage = calculateWeekMaterialUsage(weekPlans);

                // 保存到数据库
                int records = saveWeekUsage(factoryCode, year, month, week, weekMaterialUsage);
                totalRecords += records;
            }

            log.info("周维度原材料用量记录生成完成，工厂：{}，年份：{}，月份：{}，总记录数：{}",
                    factoryCode, year, month, totalRecords);

            return AjaxResult.success(String.format("周维度原材料用量记录生成完成，共生成%d条记录", totalRecords));

        } catch (Exception e) {
            log.error("生成周维度原材料用量记录失败", e);
            return AjaxResult.error("生成周维度原材料用量记录失败：" + e.getMessage());
        }
    }

    /**
     * 按周分组生产计划
     */
    private Map<Integer, List<FactoryMonthPlanProdFinal>> groupPlansByWeek(
            List<FactoryMonthPlanProdFinal> monthPlans, int year, int month) {

        Map<Integer, List<FactoryMonthPlanProdFinal>> plansByWeek = new HashMap<>();

        // 获取月份的天数
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());
        int daysInMonth = monthEnd.getDayOfMonth();

        // 遍历每天的生产计划
        for (FactoryMonthPlanProdFinal plan : monthPlans) {
            // 遍历该计划的每一天
            for (int day = 1; day <= daysInMonth; day++) {
                int dailyQty = getDailyProductionQty(plan, day);
                if (dailyQty > 0) {
                    // 计算这一天属于第几周
                    LocalDate date = LocalDate.of(year, month, day);
                    int week = getWeekOfMonth(date);

                    // 添加到对应周的列表
                    plansByWeek.computeIfAbsent(week, k -> new ArrayList<>())
                            .add(createPlanCopyForDay(plan, dailyQty));
                }
            }
        }

        return plansByWeek;
    }

    /**
     * 获取指定日期的生产数量（null视为0）
     */
    private Integer getDailyProductionQty(FactoryMonthPlanProdFinal plan, int day) {
        if (plan == null) return 0;

        switch (day) {
            case 1: return plan.getDay1() != null ? plan.getDay1() : 0;
            case 2: return plan.getDay2() != null ? plan.getDay2() : 0;
            case 3: return plan.getDay3() != null ? plan.getDay3() : 0;
            case 4: return plan.getDay4() != null ? plan.getDay4() : 0;
            case 5: return plan.getDay5() != null ? plan.getDay5() : 0;
            case 6: return plan.getDay6() != null ? plan.getDay6() : 0;
            case 7: return plan.getDay7() != null ? plan.getDay7() : 0;
            case 8: return plan.getDay8() != null ? plan.getDay8() : 0;
            case 9: return plan.getDay9() != null ? plan.getDay9() : 0;
            case 10: return plan.getDay10() != null ? plan.getDay10() : 0;
            case 11: return plan.getDay11() != null ? plan.getDay11() : 0;
            case 12: return plan.getDay12() != null ? plan.getDay12() : 0;
            case 13: return plan.getDay13() != null ? plan.getDay13() : 0;
            case 14: return plan.getDay14() != null ? plan.getDay14() : 0;
            case 15: return plan.getDay15() != null ? plan.getDay15() : 0;
            case 16: return plan.getDay16() != null ? plan.getDay16() : 0;
            case 17: return plan.getDay17() != null ? plan.getDay17() : 0;
            case 18: return plan.getDay18() != null ? plan.getDay18() : 0;
            case 19: return plan.getDay19() != null ? plan.getDay19() : 0;
            case 20: return plan.getDay20() != null ? plan.getDay20() : 0;
            case 21: return plan.getDay21() != null ? plan.getDay21() : 0;
            case 22: return plan.getDay22() != null ? plan.getDay22() : 0;
            case 23: return plan.getDay23() != null ? plan.getDay23() : 0;
            case 24: return plan.getDay24() != null ? plan.getDay24() : 0;
            case 25: return plan.getDay25() != null ? plan.getDay25() : 0;
            case 26: return plan.getDay26() != null ? plan.getDay26() : 0;
            case 27: return plan.getDay27() != null ? plan.getDay27() : 0;
            case 28: return plan.getDay28() != null ? plan.getDay28() : 0;
            case 29: return plan.getDay29() != null ? plan.getDay29() : 0;
            case 30: return plan.getDay30() != null ? plan.getDay30() : 0;
            case 31: return plan.getDay31() != null ? plan.getDay31() : 0;
            default: return 0;
        }
    }

    /**
     * 计算月份中的第几周（按自然周，周一开始）
     */
    private int getWeekOfMonth(LocalDate date) {
        // 使用ISO周标准
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4); // 周一开始，最少4天
        int weekOfMonth = date.get(weekFields.weekOfMonth());

        // 调整：第一周可能跨月，所以第0周应该算作第1周
        return Math.max(weekOfMonth, 1);
    }

    /**
     * 创建计划的副本，用于指定日期的生产
     */
    private FactoryMonthPlanProdFinal createPlanCopyForDay(FactoryMonthPlanProdFinal plan, Integer dailyQty) {
        // 创建一个新的计划对象，只包含必要信息
        FactoryMonthPlanProdFinal copy = new FactoryMonthPlanProdFinal();
        copy.setFactoryCode(plan.getFactoryCode());
        copy.setMainMaterialDesc(plan.getMainMaterialDesc());
        copy.setMaterialCode(plan.getMaterialCode());
        // 使用当天的产量
        copy.setTotalQty(dailyQty);
        return copy;
    }

    /**
     * 计算一周的原材料用量
     */
    private Map<String, BigDecimal> calculateWeekMaterialUsage(List<FactoryMonthPlanProdFinal> weekPlans) {
        // 按胎胚代码分组生产数量
        Map<String, Integer> embryoProductionMap = new HashMap<>();

        for (FactoryMonthPlanProdFinal plan : weekPlans) {
            String embryoCode = plan.getMainMaterialDesc();
            int productionQty = plan.getTotalQty();

            if (productionQty > 0) {
                embryoProductionMap.merge(embryoCode, productionQty, Integer::sum);
            }
        }

        // 计算所有原材料的用量
        Map<String, BigDecimal> totalMaterialUsage = new HashMap<>();

        for (Map.Entry<String, Integer> entry : embryoProductionMap.entrySet()) {
            String embryoCode = entry.getKey();
            int productionQty = entry.getValue();

            // 获取该胎胚的BOM详情
            List<MdmMaterialConsumeDetail> bomDetails = getBomDetails(embryoCode);

            for (MdmMaterialConsumeDetail detail : bomDetails) {
                if (detail.isValid()) {
                    String materialCode = detail.getChildMaterialCode();
                    String materialName = detail.getChildMaterialName();
                    String materialKey = materialCode + "|" + materialName;
                    BigDecimal dosage = detail.getDosage();

                    // 计算原材料用量
                    BigDecimal materialQty = BigDecimal.valueOf(productionQty).multiply(dosage);

                    totalMaterialUsage.merge(materialKey, materialQty, BigDecimal::add);
                }
            }
        }

        return totalMaterialUsage;
    }

    /**
     * 删除已存在的周用量记录
     */
    private void deleteExistingWeekUsage(String factoryCode, Integer year, Integer month) {
        QueryWrapper<RawWeekUsage> wrapper = new QueryWrapper<>();
        wrapper.eq("FACTORY_CODE", factoryCode);
        wrapper.eq("YEAR", year);

        // 删除该月份相关周次的记录
        // 先找出该月的所有周次
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        List<Integer> weeksInMonth = new ArrayList<>();
        LocalDate current = monthStart;
        while (!current.isAfter(monthEnd)) {
            int week = getWeekOfMonth(current);
            if (!weeksInMonth.contains(week)) {
                weeksInMonth.add(week);
            }
            current = current.plusDays(1);
        }

        if (!weeksInMonth.isEmpty()) {
            wrapper.in("WEEK", weeksInMonth);
        }

        rawWeekUsageEntityMapper.delete(wrapper);
        log.info("删除旧的周用量记录，工厂：{}，年份：{}，月份：{}，涉及周次：{}",
                factoryCode, year, month, weeksInMonth);
    }

    /**
     * 保存周用量记录
     */
    private int saveWeekUsage(String factoryCode, Integer year, Integer month,
                              Integer week, Map<String, BigDecimal> materialUsage) {
        int records = 0;

        // 获取周的开始和结束日期
        LocalDate[] weekDates = getWeekDates(year, month, week);
        LocalDate weekStartDate = weekDates[0];
        LocalDate weekEndDate = weekDates[1];

        for (Map.Entry<String, BigDecimal> entry : materialUsage.entrySet()) {
            String materialKey = entry.getKey();
            BigDecimal planQty = entry.getValue();

            String[] parts = materialKey.split("\\|");
            String materialCode = parts[0];
            String materialName = parts.length > 1 ? parts[1] : "";

            // 创建周用量记录
            RawWeekUsage weekUsage = new RawWeekUsage();
            weekUsage.setFactoryCode(factoryCode);
            weekUsage.setYear(year);
            weekUsage.setMonth(month);
            weekUsage.setWeek(week);
            weekUsage.setMaterialCode(materialCode);
            weekUsage.setMaterialName(materialName);
            weekUsage.setPlanQty(planQty);
            // 初始化为0，后续从MES同步
            weekUsage.setActualQty(BigDecimal.ZERO);
            weekUsage.setHasWarning(0);
            weekUsage.setStartDate(java.sql.Date.valueOf(weekStartDate));
            weekUsage.setEndDate(java.sql.Date.valueOf(weekEndDate));

            // 计算初始偏差（实际用量为0）
            weekUsage.calculateDeviation();

            // 设置创建信息
            weekUsage.setCreateTime(new Date());
            weekUsage.setCreateBy("system");

            rawWeekUsageEntityMapper.insert(weekUsage);
            records++;
        }

        log.info("保存周用量记录，工厂：{}，年份：{}，月份：{}，周次：{}，原材料种类：{}",
                factoryCode, year, month, week, materialUsage.size());

        return records;
    }

    /**
     * 获取指定年月的第几周的日期范围
     */
    private LocalDate[] getWeekDates(int year, int month, int week) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth());

        // 查找该月的第几周
        int currentWeek = 0;
        LocalDate weekStart = null;
        LocalDate current = monthStart;

        while (!current.isAfter(monthEnd)) {
            int weekOfMonth = getWeekOfMonth(current);
            if (weekOfMonth == week) {
                if (weekStart == null) {
                    weekStart = current;
                    currentWeek = weekOfMonth;
                }
            }

            // 如果找到了下一周的开始，就结束
            if (weekStart != null && weekOfMonth > week) {
                break;
            }

            current = current.plusDays(1);
        }

        // 如果找到了周的开始日期
        if (weekStart != null) {
            LocalDate weekEnd = current.minusDays(1);
            return new LocalDate[]{weekStart, weekEnd};
        }

        // 默认返回
        LocalDate defaultStart = LocalDate.of(year, month, 1);
        LocalDate defaultEnd = defaultStart.plusDays(6);
        return new LocalDate[]{defaultStart, defaultEnd};
    }

    /**
     * 批量生成周维度用量记录（用于月度计划生成时自动调用）
     * @param factoryCode 工厂编码
     * @param year 年份
     * @param month 月份
     * @return 生成结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateWeekUsageForMonth(String factoryCode, Integer year, Integer month) {
        return generateWeekUsage(factoryCode, year, month);
    }

    /**
     * 重新计算并更新周用量记录（当生产计划变更时调用）
     * @param factoryCode 工厂编码
     * @param year 年份
     * @param month 月份
     * @param week 周次
     * @return 更新结果
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult recalculateWeekUsage(String factoryCode, Integer year, Integer month, Integer week) {
        try {
            log.info("重新计算周用量记录，工厂：{}，年份：{}，月份：{}，周次：{}",
                    factoryCode, year, month, week);

            // 1. 获取该周的生产计划
            QueryWrapper<FactoryMonthPlanProdFinal> planWrapper = new QueryWrapper<>();
            planWrapper.eq("FACTORY_CODE", factoryCode);
            planWrapper.eq("YEAR", year);
            planWrapper.eq("MONTH", month);
            List<FactoryMonthPlanProdFinal> monthPlans = factoryMonthPlanProdFinalMapper.selectList(planWrapper);

            if (monthPlans.isEmpty()) {
                return AjaxResult.error("未找到生产计划数据");
            }

            // 2. 过滤出该周的生产计划
            List<FactoryMonthPlanProdFinal> weekPlans = filterPlansByWeek(monthPlans, year, month, week);

            // 3. 计算该周的原材料计划用量
            Map<String, BigDecimal> weekMaterialUsage = calculateWeekMaterialUsage(weekPlans);

            // 4. 删除该周旧的用量记录
            QueryWrapper<RawWeekUsage> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("FACTORY_CODE", factoryCode);
            deleteWrapper.eq("YEAR", year);
            deleteWrapper.eq("WEEK", week);
            rawWeekUsageEntityMapper.delete(deleteWrapper);

            // 5. 保存新的用量记录
            int records = saveWeekUsage(factoryCode, year, month, week, weekMaterialUsage);

            log.info("周用量记录重新计算完成，工厂：{}，年份：{}，月份：{}，周次：{}，更新记录数：{}",
                    factoryCode, year, month, week, records);

            return AjaxResult.success(String.format("周用量记录重新计算完成，更新%d条记录", records));

        } catch (Exception e) {
            log.error("重新计算周用量记录失败", e);
            return AjaxResult.error("重新计算周用量记录失败：" + e.getMessage());
        }
    }

    /**
     * 过滤出指定周次的生产计划
     */
    private List<FactoryMonthPlanProdFinal> filterPlansByWeek(
            List<FactoryMonthPlanProdFinal> monthPlans, int year, int month, int week) {

        List<FactoryMonthPlanProdFinal> weekPlans = new ArrayList<>();

        for (FactoryMonthPlanProdFinal plan : monthPlans) {
            // 遍历该计划的每一天
            for (int day = 1; day <= 31; day++) {
                Integer dailyQty = getDailyProductionQty(plan, day);
                if (dailyQty != null && dailyQty > 0) {
                    try {
                        LocalDate date = LocalDate.of(year, month, day);
                        int weekOfMonth = getWeekOfMonth(date);

                        if (weekOfMonth == week) {
                            weekPlans.add(createPlanCopyForDay(plan, dailyQty));
                        }
                    } catch (Exception e) {
                        // 跳过无效日期（如2月30日）
                        continue;
                    }
                }
            }
        }

        return weekPlans;
    }

    /**
     * 获取周用量统计数据
     * @param factoryCode 工厂编码
     * @param year 年份
     * @param month 月份
     * @param week 周次
     * @return 统计数据
     */
    public Map<String, Object> getWeekUsageStatistics(String factoryCode, Integer year, Integer month, Integer week) {
        QueryWrapper<RawWeekUsage> wrapper = new QueryWrapper<>();
        wrapper.eq("FACTORY_CODE", factoryCode);
        wrapper.eq("YEAR", year);
        wrapper.eq("WEEK", week);
        List<RawWeekUsage> weekUsages = rawWeekUsageEntityMapper.selectList(wrapper);

        Map<String, Object> statistics = new HashMap<>();

        // 总计划用量
        BigDecimal totalPlanQty = weekUsages.stream()
                .map(RawWeekUsage::getPlanQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总实际用量
        BigDecimal totalActualQty = weekUsages.stream()
                .map(RawWeekUsage::getActualQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 总偏差量
        BigDecimal totalDeviationQty = weekUsages.stream()
                .map(RawWeekUsage::getDeviationQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 有预警的记录数
        long warningCount = weekUsages.stream()
                .filter(usage -> usage.getHasWarning() != null && usage.getHasWarning() == 1)
                .count();

        // 原材料种类数
        int materialCount = (int) weekUsages.stream()
                .map(RawWeekUsage::getMaterialCode)
                .distinct()
                .count();

        statistics.put("totalPlanQty", totalPlanQty);
        statistics.put("totalActualQty", totalActualQty);
        statistics.put("totalDeviationQty", totalDeviationQty);
        statistics.put("warningCount", warningCount);
        statistics.put("materialCount", materialCount);
        statistics.put("totalRecords", weekUsages.size());

        return statistics;
    }

    /**
     * 获取BOM结构详情
     */
    private List<MdmMaterialConsumeDetail> getBomDetails(String embryoCode) {
        QueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("EMBRYO_CODE", embryoCode);
        //queryWrapper.eq("EMBRYO_VERSION", "1");
        return mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
    }

}