package com.zlt.aps.monthplan.raw.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.raw.service.IRawWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RawWarningServiceImpl extends ServiceImpl<RawWarningRecordEntityMapper, RawWarningRecord> implements IRawWarningService {

    @Autowired
    private RawWarningConfigEntityMapper warningConfigMapper;

    @Autowired
    private RawWeekUsageEntityMapper rawWeekUsageEntityMapper;

    @Autowired
    private RawMaterialMonthDiffMapper rawMaterialMonthDiffMapper;

    @Autowired
    private RawMaterialOutboundRecordEntityMapper rawMaterialOutboundRecordMapper;

    @Autowired
    private RawMaterialRequirePlanEntityMapper rawMaterialRequirePlanMapper;

    /**
     * 执行用量偏差预警
     * @param factoryCode 工厂编码
     * @param year 年份
     * @param week 周次
     * @return 预警结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult executeUsageDeviationWarning(String factoryCode, Integer year, Integer week) {
        try {
            log.info("开始执行用量偏差预警，工厂：{}，年份：{}，周次：{}", factoryCode, year, week);

            // 1. 获取本周的原材料用量记录
            QueryWrapper<RawWeekUsage> usageWrapper = new QueryWrapper<>();
            usageWrapper.eq("FACTORY_CODE", factoryCode);
            usageWrapper.eq("YEAR", year);
            usageWrapper.eq("WEEK", week);
            List<RawWeekUsage> weekUsages = rawWeekUsageEntityMapper.selectList(usageWrapper);

            if (weekUsages.isEmpty()) {
                log.warn("未找到周用量数据，工厂：{}，年份：{}，周次：{}", factoryCode, year, week);
                return AjaxResult.error("未找到周用量数据");
            }

            // 2. 获取用量偏差预警配置
            QueryWrapper<RawWarningConfig> configWrapper = new QueryWrapper<>();
            configWrapper.eq("FACTORY_CODE", factoryCode);
            configWrapper.eq("WARNING_TYPE", "1"); // 用量偏差预警
            configWrapper.eq("ENABLED", 1);
            List<RawWarningConfig> warningConfigs = warningConfigMapper.selectList(configWrapper);

            // 转换为Map，方便查找
            Map<String, RawWarningConfig> configMap = warningConfigs.stream()
                    .collect(Collectors.toMap(RawWarningConfig::getMaterialCode, config -> config));

            // 3. 检查每个原材料的用量偏差
            List<RawWarningRecord> warningRecords = new ArrayList<>();
            int warningCount = 0;

            for (RawWeekUsage usage : weekUsages) {
                // 计算偏差
                usage.calculateDeviation();

                // 检查是否需要预警
                RawWarningConfig config = configMap.get(usage.getMaterialCode());
                if (usage.checkWarning(config)) {
                    // 创建预警记录
                    RawWarningRecord warningRecord = createUsageWarningRecord(usage, config);
                    warningRecords.add(warningRecord);
                    warningCount++;

                    // 更新用量记录的预警状态
                    usage.setHasWarning(1);
                    usage.setWarningLevel(config.getWarningLevel());
                    rawWeekUsageEntityMapper.updateById(usage);
                } else {
                    // 清除预警状态
                    usage.setHasWarning(0);
                    usage.setWarningLevel(null);
                    rawWeekUsageEntityMapper.updateById(usage);
                }
            }

            // 4. 保存预警记录
            if (!warningRecords.isEmpty()) {
                saveBatch(warningRecords);
            }

            log.info("用量偏差预警执行完成，工厂：{}，年份：{}，周次：{}，生成预警记录：{}条",
                    factoryCode, year, week, warningCount);

            return AjaxResult.success(String.format("用量偏差预警执行完成，生成预警记录：%d条", warningCount));

        } catch (Exception e) {
            log.error("执行用量偏差预警失败", e);
            return AjaxResult.error("执行用量偏差预警失败：" + e.getMessage());
        }
    }

    /**
     * 创建用量偏差预警记录
     */
    private RawWarningRecord createUsageWarningRecord(RawWeekUsage usage, RawWarningConfig config) {
        RawWarningRecord record = new RawWarningRecord();
        record.setFactoryCode(usage.getFactoryCode());
        record.setWarningType("1"); // 用量偏差预警
        record.setMaterialCode(usage.getMaterialCode());
        record.setMaterialName(usage.getMaterialName());
        record.setWarningLevel(config.getWarningLevel());
        record.setRelatedWeek(String.format("%d年第%02d周", usage.getYear(), usage.getWeek()));
        record.setStatus("0"); // 未处理
        record.setNotified(0); // 未通知

        // 设置预警标题和内容
        String title = String.format("原材料用量偏差预警 - %s", usage.getMaterialName());
        record.setWarningTitle(title);

        String content = String.format(
                "工厂：%s，原材料：%s（%s），%d年第%02d周用量偏差超限。\n" +
                        "计划用量：%s，实际用量：%s，偏差量：%s，偏差率：%.2f%%",
                usage.getFactoryCode(),
                usage.getMaterialName(),
                usage.getMaterialCode(),
                usage.getYear(),
                usage.getWeek(),
                usage.getPlanQty(),
                usage.getActualQty(),
                usage.getDeviationQty(),
                usage.getDeviationRate().multiply(new BigDecimal("100"))
        );
        record.setWarningContent(content);

        // 设置预警数据JSON
        Map<String, Object> warningData = new HashMap<>();
        warningData.put("year", usage.getYear());
        warningData.put("week", usage.getWeek());
        warningData.put("planQty", usage.getPlanQty());
        warningData.put("actualQty", usage.getActualQty());
        warningData.put("deviationQty", usage.getDeviationQty());
        warningData.put("deviationRate", usage.getDeviationRate());
        warningData.put("configUpper", config.getDeviationUpper());
        warningData.put("configLower", config.getDeviationLower());
        record.setWarningData(JSON.toJSONString(warningData));

        record.setCreateTime(new Date());
        record.setCreateBy("system");

        return record;
    }

    /**
     * 执行新材料预警
     * @param factoryCode 工厂编码
     * @param currentYear 当前年份
     * @param currentMonth 当前月份
     * @return 预警结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult executeNewMaterialWarning(String factoryCode, Integer currentYear, Integer currentMonth) {
        try {
            log.info("开始执行新材料预警，工厂：{}，当前年月：{}-{}", factoryCode, currentYear, currentMonth);

            // 1. 获取上个月的年份和月份
            LocalDate currentDate = LocalDate.of(currentYear, currentMonth, 1);
            LocalDate previousDate = currentDate.minusMonths(1);
            Integer previousYear = previousDate.getYear();
            Integer previousMonth = previousDate.getMonthValue();

            // 2. 查询原材料月计划差异数据
            QueryWrapper<RawMaterialMonthDiff> diffWrapper = new QueryWrapper<>();
            diffWrapper.eq("FACTORY_CODE", factoryCode);
            diffWrapper.eq("YEAR", currentYear);
            diffWrapper.eq("MONTH", currentMonth);
            List<RawMaterialMonthDiff> diffs = rawMaterialMonthDiffMapper.selectList(diffWrapper);

            if (diffs.isEmpty()) {
                log.info("未找到新材料预警数据，工厂：{}，年月：{}-{}", factoryCode, currentYear, currentMonth);
                return AjaxResult.success("未找到新材料预警数据");
            }

            // 3. 获取新材料预警配置
            QueryWrapper<RawWarningConfig> configWrapper = new QueryWrapper<>();
            configWrapper.eq("FACTORY_CODE", factoryCode);
            configWrapper.eq("WARNING_TYPE", "2"); // 新材料预警
            configWrapper.eq("ENABLED", 1);
            List<RawWarningConfig> warningConfigs = warningConfigMapper.selectList(configWrapper);

            // 获取所有需要预警的原材料编码
            Set<String> warningMaterialCodes = warningConfigs.stream()
                    .map(RawWarningConfig::getMaterialCode)
                    .collect(Collectors.toSet());

            // 如果配置为空，则对所有新材料都预警
            boolean warnAll = warningConfigs.isEmpty();

            // 4. 创建预警记录
            List<RawWarningRecord> warningRecords = new ArrayList<>();
            int warningCount = 0;

            // 按差异类型分组
            Map<String, List<RawMaterialMonthDiff>> diffByType = diffs.stream()
                    .collect(Collectors.groupingBy(RawMaterialMonthDiff::getDiffType));

            // 处理新增原材料
            List<RawMaterialMonthDiff> newMaterials = diffByType.getOrDefault("新增", Collections.emptyList());
            if (!newMaterials.isEmpty()) {
                List<RawWarningRecord> newWarnings = createNewMaterialWarnings(
                        factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                        newMaterials, warningMaterialCodes, warnAll, "新增"
                );
                warningRecords.addAll(newWarnings);
                warningCount += newWarnings.size();
            }

            // 处理减少原材料
            List<RawMaterialMonthDiff> removedMaterials = diffByType.getOrDefault("减少", Collections.emptyList());
            if (!removedMaterials.isEmpty()) {
                List<RawWarningRecord> removedWarnings = createNewMaterialWarnings(
                        factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                        removedMaterials, warningMaterialCodes, warnAll, "减少"
                );
                warningRecords.addAll(removedWarnings);
                warningCount += removedWarnings.size();
            }

            // 5. 保存预警记录
            if (!warningRecords.isEmpty()) {
                saveBatch(warningRecords);
            }

            log.info("新材料预警执行完成，工厂：{}，当前年月：{}-{}，生成预警记录：{}条",
                    factoryCode, currentYear, currentMonth, warningCount);

            return AjaxResult.success(String.format("新材料预警执行完成，生成预警记录：%d条", warningCount));

        } catch (Exception e) {
            log.error("执行新材料预警失败", e);
            return AjaxResult.error("执行新材料预警失败：" + e.getMessage());
        }
    }

    /**
     * 创建新材料预警记录
     */
    private List<RawWarningRecord> createNewMaterialWarnings(String factoryCode,
                                                             Integer currentYear, Integer currentMonth,
                                                             Integer previousYear, Integer previousMonth,
                                                             List<RawMaterialMonthDiff> diffs,
                                                             Set<String> warningMaterialCodes,
                                                             boolean warnAll,
                                                             String diffType) {
        List<RawWarningRecord> warnings = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月");

        String currentMonthStr = LocalDate.of(currentYear, currentMonth, 1).format(formatter);
        String previousMonthStr = LocalDate.of(previousYear, previousMonth, 1).format(formatter);

        for (RawMaterialMonthDiff diff : diffs) {
            // 检查是否需要预警（如果配置了特定材料，则只预警配置的材料）
            if (!warnAll && !warningMaterialCodes.contains(diff.getMaterialCode())) {
                continue;
            }

            RawWarningRecord record = new RawWarningRecord();
            record.setFactoryCode(factoryCode);
            record.setWarningType("2"); // 新材料预警
            record.setMaterialCode(diff.getMaterialCode());
            record.setMaterialName(diff.getMaterialName());
            record.setWarningLevel("2"); // 中等级别
            record.setRelatedMonth(currentMonthStr);
            record.setStatus("0"); // 未处理
            record.setNotified(0); // 未通知

            // 设置预警标题和内容
            String title = "新增".equals(diffType) ?
                    String.format("新增原材料预警 - %s", diff.getMaterialName()) :
                    String.format("减少原材料预警 - %s", diff.getMaterialName());
            record.setWarningTitle(title);

            String content = "新增".equals(diffType) ?
                    String.format(
                            "工厂：%s，检测到新增原材料。\n" +
                                    "原材料：%s（%s）\n" +
                                    "比较周期：%s（%d-%02d）→ %s（%d-%02d）\n" +
                                    "上个月用量：%s，本月计划：%s，差异量：%s",
                            factoryCode,
                            diff.getMaterialName(),
                            diff.getMaterialCode(),
                            previousMonthStr, previousYear, previousMonth,
                            currentMonthStr, currentYear, currentMonth,
                            diff.getPrevMonthQty(),
                            diff.getCurMonthQty(),
                            diff.getDiffQty()
                    ) :
                    String.format(
                            "工厂：%s，检测到减少原材料。\n" +
                                    "原材料：%s（%s）\n" +
                                    "比较周期：%s（%d-%02d）→ %s（%d-%02d）\n" +
                                    "上个月用量：%s，本月计划：%s，差异量：%s",
                            factoryCode,
                            diff.getMaterialName(),
                            diff.getMaterialCode(),
                            previousMonthStr, previousYear, previousMonth,
                            currentMonthStr, currentYear, currentMonth,
                            diff.getPrevMonthQty(),
                            diff.getCurMonthQty(),
                            diff.getDiffQty()
                    );
            record.setWarningContent(content);

            // 设置预警数据JSON
            Map<String, Object> warningData = new HashMap<>();
            warningData.put("diffType", diffType);
            warningData.put("currentYear", currentYear);
            warningData.put("currentMonth", currentMonth);
            warningData.put("previousYear", previousYear);
            warningData.put("previousMonth", previousMonth);
            warningData.put("prevMonthQty", diff.getPrevMonthQty());
            warningData.put("curMonthQty", diff.getCurMonthQty());
            warningData.put("diffQty", diff.getDiffQty());
            record.setWarningData(JSON.toJSONString(warningData));

            record.setCreateTime(new Date());
            record.setCreateBy("system");

            warnings.add(record);
        }

        return warnings;
    }

    /**
     * 同步周维度实际用量数据（从MES系统）
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param week        周次
     * @param month
     * @return 同步结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult syncWeekActualUsage(String factoryCode, Integer year, Integer week, Integer month) {
        try {
            log.info("开始同步周维度实际用量数据，工厂：{}，年份：{}，周次：{}", factoryCode, year, week);

            // 1. 获取周的开始和结束日期
            LocalDate weekStartDate = getWeekStartDate(year, month, week);
            LocalDate weekEndDate = getWeekEndDate(year, month, week);

            // 2. 从MES系统查询该周的实际出库量
            QueryWrapper<RawMaterialOutboundRecord> outboundWrapper = new QueryWrapper<>();
            outboundWrapper.eq("FACTORY_CODE", factoryCode);
            outboundWrapper.between("OUTBOUND_DATE", weekStartDate, weekEndDate);
            List<RawMaterialOutboundRecord> outboundRecords = rawMaterialOutboundRecordMapper.selectList(outboundWrapper);

            // 按原材料分组汇总实际用量
            Map<String, BigDecimal> actualUsageMap = outboundRecords.stream()
                    .collect(Collectors.groupingBy(
                            RawMaterialOutboundRecord::getMaterialCode,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    RawMaterialOutboundRecord::getOutboundQty,
                                    BigDecimal::add
                            )
                    ));

            // 3. 获取该周的计划用量数据
            QueryWrapper<RawWeekUsage> usageWrapper = new QueryWrapper<>();
            usageWrapper.eq("FACTORY_CODE", factoryCode);
            usageWrapper.eq("YEAR", year);
            usageWrapper.eq("MONTH", month);
            usageWrapper.eq("WEEK", week);
            List<RawWeekUsage> weekUsages = rawWeekUsageEntityMapper.selectList(usageWrapper);

            // 4. 更新实际用量
            for (RawWeekUsage usage : weekUsages) {
                BigDecimal actualQty = actualUsageMap.get(usage.getMaterialCode());
                if (actualQty != null) {
                    usage.setActualQty(actualQty);
                    // 重新计算偏差
                    usage.calculateDeviation();
                    rawWeekUsageEntityMapper.updateById(usage);
                }
            }

            log.info("同步周维度实际用量数据完成，工厂：{}，年份：{}，周次：{}，同步原材料数：{}",
                    factoryCode, year, week, actualUsageMap.size());

            return AjaxResult.success(String.format("同步完成，共处理%d种原材料", actualUsageMap.size()));

        } catch (Exception e) {
            log.error("同步周维度实际用量数据失败", e);
            return AjaxResult.error("同步失败：" + e.getMessage());
        }
    }

    /**
     * 根据年份和周次获取周开始日期
     */
    private LocalDate getWeekStartDate(int year, int month, int week) {
        // 假设第一周从1月1日开始
        LocalDate date = LocalDate.of(year, month, 1);
        // 调整到该周的第一天（周一）
        date = date.plusWeeks(week - 1);
        date = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        return date;
    }

    /**
     * 根据年份和周次获取周结束日期
     */
    private LocalDate getWeekEndDate(int year, int month, int week) {
        LocalDate startDate = getWeekStartDate(year, month, week);
        return startDate.plusDays(6);
    }

    /**
     * 查询预警记录
     * @param factoryCode 工厂编码
     * @param warningType 预警类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param status 处理状态
     * @return 预警记录列表
     */
    @Override
    public List<RawWarningRecord> queryWarningRecords(String factoryCode, String warningType,
                                                      Date startDate, Date endDate, String status) {
        QueryWrapper<RawWarningRecord> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(factoryCode)) {
            wrapper.eq("FACTORY_CODE", factoryCode);
        }
        if (StringUtils.hasText(warningType)) {
            wrapper.eq("WARNING_TYPE", warningType);
        }
        if (startDate != null) {
            wrapper.ge("CREATE_TIME", startDate);
        }
        if (endDate != null) {
            wrapper.le("CREATE_TIME", endDate);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("STATUS", status);
        }

        wrapper.orderByDesc("CREATE_TIME");

        return list(wrapper);
    }

    /**
     * 处理预警记录
     * @param id 预警记录ID
     * @param handler 处理人
     * @param opinion 处理意见
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult handleWarning(Long id, String handler, String opinion) {
        RawWarningRecord record = getById(id);
        if (record == null) {
            return AjaxResult.error("预警记录不存在");
        }

        record.setStatus("1"); // 已处理
        record.setHandler(handler);
        record.setHandleOpinion(opinion);
        record.setHandleTime(new Date());
        record.setUpdateTime(new Date());
        record.setUpdateBy(handler);

        updateById(record);

        return AjaxResult.success("处理成功");
    }

    /**
     * 统计预警信息
     * @param factoryCode 工厂编码
     * @param warningType 预警类型
     * @param days 最近天数
     * @return 统计结果
     */
    @Override
    public Map<String, Object> getWarningStatistics(String factoryCode, String warningType, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days != null ? days : 30);

        QueryWrapper<RawWarningRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("FACTORY_CODE", factoryCode);
        if (StringUtils.hasText(warningType)) {
            wrapper.eq("WARNING_TYPE", warningType);
        }
        wrapper.between("CREATE_TIME", startDate, endDate);

        // 按预警级别统计
        List<Map<String, Object>> levelStats = listMaps(
                wrapper.select("WARNING_LEVEL", "COUNT(*) as count")
                        .groupBy("WARNING_LEVEL")
        );

        // 按处理状态统计
        List<Map<String, Object>> statusStats = listMaps(
                wrapper.select("STATUS", "COUNT(*) as count")
                        .groupBy("STATUS")
        );

        // 最近预警
        wrapper.clear();
        wrapper.eq("FACTORY_CODE", factoryCode);
        if (StringUtils.hasText(warningType)) {
            wrapper.eq("WARNING_TYPE", warningType);
        }
        wrapper.orderByDesc("CREATE_TIME");
        wrapper.last("LIMIT 10");
        List<RawWarningRecord> recentWarnings = list(wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("levelStats", levelStats);
        result.put("statusStats", statusStats);
        result.put("recentWarnings", recentWarnings);
        result.put("totalCount", count(wrapper));

        return result;
    }
}