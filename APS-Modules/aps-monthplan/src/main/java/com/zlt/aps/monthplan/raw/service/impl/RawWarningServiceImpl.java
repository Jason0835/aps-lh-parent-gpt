package com.zlt.aps.monthplan.raw.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.raw.service.IRawWarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
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

    // 批量处理大小
    private static final int BATCH_SIZE = 500;
    // 多语言键值前缀
    private static final String WARNING_PREFIX = "raw.warning.";
    // 日期格式化器
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月");
    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月第W周");

    /**
     * 执行用量偏差预警
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param week        周次
     * @param month      月份
     * @return 预警结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult executeUsageDeviationWarning(String factoryCode, Integer year, Integer week, Integer month) {
        try {
            log.info(StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "start.usage.deviation"),
                    factoryCode, year, week
            ));

            // 1. 批量获取本周的原材料用量记录
            List<RawWeekUsage> weekUsages = getWeekUsages(factoryCode, year, month, week);

            if (weekUsages.isEmpty()) {
                String message = StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "no.week.usage.data"),
                        factoryCode, year, week
                );
                log.warn(message);
                return AjaxResult.error(message);
            }

            // 2. 批量获取用量偏差预警配置
            Map<String, RawWarningConfig> configMap = getUsageWarningConfigs(factoryCode);

            // 3. 检查每个原材料的用量偏差
            List<RawWarningRecord> warningRecords = new ArrayList<>();
            List<RawWeekUsage> usagesToUpdate = new ArrayList<>();
            int warningCount = 0;

            for (RawWeekUsage usage : weekUsages) {
                // 计算偏差
                usage.calculateDeviation();

                // 检查是否需要预警
                RawWarningConfig config = configMap.get(usage.getMaterialCode());
                if (config != null && usage.checkWarning(config)) {
                    // 创建预警记录
                    RawWarningRecord warningRecord = createUsageWarningRecord(usage, config);
                    warningRecords.add(warningRecord);
                    warningCount++;

                    // 更新用量记录的预警状态
                    updateUsageWarningStatus(usage, config.getWarningLevel(), 1);
                } else {
                    // 清除预警状态
                    updateUsageWarningStatus(usage, null, 0);
                }
                usagesToUpdate.add(usage);
            }

            // 4. 批量保存预警记录
            if (!warningRecords.isEmpty()) {
                saveBatchWarningRecords(warningRecords);
            }

            // 5. 批量更新用量记录
            if (!usagesToUpdate.isEmpty()) {
                batchUpdateWeekUsages(usagesToUpdate);
            }

            String logMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.complete"),
                    factoryCode, year, week, warningCount
            );
            log.info(logMessage);

            String resultMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.result"),
                    warningCount
            );
            return AjaxResult.success(resultMessage);

        } catch (Exception e) {
            log.error(I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.error"), e);
            String errorMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(errorMessage);
        }
    }

    /**
     * 批量获取周用量记录
     */
    private List<RawWeekUsage> getWeekUsages(String factoryCode, Integer year, Integer month, Integer week) {
        QueryWrapper<RawWeekUsage> usageWrapper = new QueryWrapper<>();
        usageWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("WEEK", week);
        return rawWeekUsageEntityMapper.selectList(usageWrapper);
    }

    /**
     * 批量获取用量偏差预警配置
     */
    private Map<String, RawWarningConfig> getUsageWarningConfigs(String factoryCode) {
        QueryWrapper<RawWarningConfig> configWrapper = new QueryWrapper<>();
        configWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("WARNING_TYPE", "1") // 用量偏差预警
                .eq("ENABLED", 1);
        List<RawWarningConfig> warningConfigs = warningConfigMapper.selectList(configWrapper);

        return warningConfigs.stream()
                .collect(Collectors.toMap(RawWarningConfig::getMaterialCode, Function.identity()));
    }

    /**
     * 更新用量记录的预警状态
     */
    private void updateUsageWarningStatus(RawWeekUsage usage, String warningLevel, Integer hasWarning) {
        usage.setHasWarning(hasWarning);
        if (warningLevel != null) {
            usage.setWarningLevel(warningLevel);
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
        record.setMaterialDesc(usage.getMaterialDesc());
        record.setRelatedMonth(StringUtils.format(
                I18nUtil.getMessage("common.year.month.format"),
                usage.getYear(), usage.getMonth()
        ));
        record.setWarningLevel(config.getWarningLevel());
        record.setRelatedWeek(StringUtils.format(
                I18nUtil.getMessage("common.year.month.week.format"),
                usage.getYear(), usage.getMonth(), usage.getWeek()
        ));
        record.setStatus("0"); // 未处理
        record.setNotified(0); // 未通知

        // 设置预警标题和内容
        String title = StringUtils.format(
                I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.title"),
                usage.getMaterialDesc()
        );
        record.setWarningTitle(title);

        String content = StringUtils.format(
                I18nUtil.getMessage(WARNING_PREFIX + "usage.deviation.content"),
                usage.getFactoryCode(),
                usage.getMaterialDesc(),
                usage.getMaterialCode(),
                usage.getYear(),
                usage.getMonth(),
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
     * 批量保存预警记录
     */
    private void saveBatchWarningRecords(List<RawWarningRecord> warningRecords) {
        // 分批保存
        List<List<RawWarningRecord>> batches = splitIntoBatches(warningRecords, BATCH_SIZE);
        batches.forEach(batch -> {
            if (!CollectionUtils.isEmpty(batch)) {
                // 使用自定义批量插入方法
                rawWeekUsageEntityMapper.batchInsert(batch);
            }
        });
    }

    /**
     * 批量更新周用量记录
     */
    private void batchUpdateWeekUsages(List<RawWeekUsage> usagesToUpdate) {
        // 分批更新
        List<List<RawWeekUsage>> batches = splitIntoBatches(usagesToUpdate, BATCH_SIZE);
        batches.forEach(batch -> {
            if (!CollectionUtils.isEmpty(batch)) {
                // 使用自定义批量插入方法
                rawWeekUsageEntityMapper.batchUpdate(batch);
            }
        });
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
            log.info(StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "start.new.material"),
                    factoryCode, currentYear, currentMonth
            ));

            // 1. 获取上个月的年份和月份
            LocalDate currentDate = LocalDate.of(currentYear, currentMonth, 1);
            LocalDate previousDate = currentDate.minusMonths(1);
            Integer previousYear = previousDate.getYear();
            Integer previousMonth = previousDate.getMonthValue();

            // 2. 批量查询原材料月计划差异数据
            List<RawMaterialMonthDiff> diffs = getMaterialMonthDiffs(factoryCode, currentYear, currentMonth);

            if (diffs.isEmpty()) {
                String message = StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "no.new.material.data"),
                        factoryCode, currentYear, currentMonth
                );
                log.info(message);
                return AjaxResult.success(message);
            }

            // 3. 获取新材料预警配置
            List<RawWarningConfig> warningConfigs = getNewMaterialWarningConfigs(factoryCode);
            Set<String> warningMaterialCodes = extractWarningMaterialCodes(warningConfigs);
            boolean warnAll = CollectionUtils.isEmpty(warningConfigs);

            // 4. 创建预警记录
            List<RawWarningRecord> warningRecords = createNewMaterialWarnings(
                    factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                    diffs, warningMaterialCodes, warnAll
            );

            // 5. 批量保存预警记录
            if (!warningRecords.isEmpty()) {
                saveBatchWarningRecords(warningRecords);
            }

            String logMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "new.material.complete"),
                    factoryCode, currentYear, currentMonth, warningRecords.size()
            );
            log.info(logMessage);

            String resultMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "new.material.result"),
                    warningRecords.size()
            );
            return AjaxResult.success(resultMessage);

        } catch (Exception e) {
            log.error(I18nUtil.getMessage(WARNING_PREFIX + "new.material.error"), e);
            String errorMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "new.material.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(errorMessage);
        }
    }

    /**
     * 批量查询原材料月计划差异数据
     */
    private List<RawMaterialMonthDiff> getMaterialMonthDiffs(String factoryCode, Integer year, Integer month) {
        QueryWrapper<RawMaterialMonthDiff> diffWrapper = new QueryWrapper<>();
        diffWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month);
        return rawMaterialMonthDiffMapper.selectList(diffWrapper);
    }

    /**
     * 获取新材料预警配置
     */
    private List<RawWarningConfig> getNewMaterialWarningConfigs(String factoryCode) {
        QueryWrapper<RawWarningConfig> configWrapper = new QueryWrapper<>();
        configWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("WARNING_TYPE", "2") // 新材料预警
                .eq("ENABLED", 1);
        return warningConfigMapper.selectList(configWrapper);
    }

    /**
     * 提取预警材料编码
     */
    private Set<String> extractWarningMaterialCodes(List<RawWarningConfig> warningConfigs) {
        if (CollectionUtils.isEmpty(warningConfigs)) {
            return Collections.emptySet();
        }
        return warningConfigs.stream()
                .map(RawWarningConfig::getMaterialCode)
                .collect(Collectors.toSet());
    }

    /**
     * 创建新材料预警记录
     */
    private List<RawWarningRecord> createNewMaterialWarnings(String factoryCode,
                                                             Integer currentYear, Integer currentMonth,
                                                             Integer previousYear, Integer previousMonth,
                                                             List<RawMaterialMonthDiff> diffs,
                                                             Set<String> warningMaterialCodes,
                                                             boolean warnAll) {
        List<RawWarningRecord> warnings = new ArrayList<>();

        // 按差异类型分组
        Map<String, List<RawMaterialMonthDiff>> diffByType = diffs.stream()
                .collect(Collectors.groupingBy(RawMaterialMonthDiff::getDiffType));

        // 处理新增原材料
        List<RawMaterialMonthDiff> newMaterials = diffByType.getOrDefault(
                I18nUtil.getMessage(WARNING_PREFIX + "diff.type.new"),
                Collections.emptyList()
        );
        warnings.addAll(createMaterialWarningsByType(
                factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                newMaterials, warningMaterialCodes, warnAll, true
        ));

        // 处理减少原材料
        List<RawMaterialMonthDiff> removedMaterials = diffByType.getOrDefault(
                I18nUtil.getMessage(WARNING_PREFIX + "diff.type.decrease"),
                Collections.emptyList()
        );
        warnings.addAll(createMaterialWarningsByType(
                factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                removedMaterials, warningMaterialCodes, warnAll, false
        ));

        return warnings;
    }

    /**
     * 按类型创建材料预警记录
     */
    private List<RawWarningRecord> createMaterialWarningsByType(String factoryCode,
                                                                Integer currentYear, Integer currentMonth,
                                                                Integer previousYear, Integer previousMonth,
                                                                List<RawMaterialMonthDiff> diffs,
                                                                Set<String> warningMaterialCodes,
                                                                boolean warnAll,
                                                                boolean isNewMaterial) {
        if (CollectionUtils.isEmpty(diffs)) {
            return Collections.emptyList();
        }

        List<RawWarningRecord> warnings = new ArrayList<>();
        String currentMonthStr = LocalDate.of(currentYear, currentMonth, 1).format(MONTH_FORMATTER);
        String previousMonthStr = LocalDate.of(previousYear, previousMonth, 1).format(MONTH_FORMATTER);

        for (RawMaterialMonthDiff diff : diffs) {
            // 检查是否需要预警
            if (!warnAll && !warningMaterialCodes.contains(diff.getMaterialCode())) {
                continue;
            }

            warnings.add(createSingleMaterialWarningRecord(
                    factoryCode, currentYear, currentMonth, previousYear, previousMonth,
                    diff, currentMonthStr, previousMonthStr, isNewMaterial
            ));
        }

        return warnings;
    }

    /**
     * 创建单个材料预警记录
     */
    private RawWarningRecord createSingleMaterialWarningRecord(String factoryCode,
                                                               Integer currentYear, Integer currentMonth,
                                                               Integer previousYear, Integer previousMonth,
                                                               RawMaterialMonthDiff diff,
                                                               String currentMonthStr,
                                                               String previousMonthStr,
                                                               boolean isNewMaterial) {
        RawWarningRecord record = new RawWarningRecord();
        record.setFactoryCode(factoryCode);
        record.setWarningType("2"); // 新材料预警
        record.setMaterialCode(diff.getMaterialCode());
        record.setMaterialDesc(diff.getMaterialDesc());
        record.setWarningLevel("2"); // 中等级别
        record.setRelatedMonth(currentMonthStr);
        record.setStatus("0"); // 未处理
        record.setNotified(0); // 未通知

        // 设置预警标题和内容
        String title = isNewMaterial ?
                StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "new.material.title"),
                        diff.getMaterialDesc()
                ) :
                StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "decrease.material.title"),
                        diff.getMaterialDesc()
                );
        record.setWarningTitle(title);

        String content = isNewMaterial ?
                StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "new.material.content"),
                        factoryCode,
                        diff.getMaterialDesc(),
                        diff.getMaterialCode(),
                        previousMonthStr, previousYear, previousMonth,
                        currentMonthStr, currentYear, currentMonth,
                        diff.getPrevMonthQty(),
                        diff.getCurMonthQty(),
                        diff.getDiffQty()
                ) :
                StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "decrease.material.content"),
                        factoryCode,
                        diff.getMaterialDesc(),
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
        warningData.put("diffType", isNewMaterial ? "new" : "decrease");
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

        return record;
    }

    /**
     * 同步周维度实际用量数据（从MES系统）
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param week        周次
     * @param month      月份
     * @return 同步结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult syncWeekActualUsage(String factoryCode, Integer year, Integer week, Integer month) {
        try {
            log.info(StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "start.sync.usage"),
                    factoryCode, year, week
            ));

            // 1. 获取周的开始和结束日期
            LocalDate weekStartDate = getWeekStartDate(year, month, week);
            LocalDate weekEndDate = getWeekEndDate(year, month, week);

            // 2. 从MES系统批量查询该周的实际出库量
            Map<String, BigDecimal> actualUsageMap = getActualUsageMap(factoryCode, weekStartDate, weekEndDate);

            // 3. 批量获取该周的计划用量数据
            List<RawWeekUsage> weekUsages = getWeekUsages(factoryCode, year, month, week);

            if (weekUsages.isEmpty()) {
                String message = StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "no.plan.usage.data"),
                        factoryCode, year, week
                );
                log.warn(message);
                return AjaxResult.error(message);
            }

            // 4. 批量更新实际用量
            List<RawWeekUsage> updatedUsages = updateWeekUsagesActualQty(weekUsages, actualUsageMap);

            // 5. 批量更新数据库
            if (!updatedUsages.isEmpty()) {
                batchUpdateWeekUsages(updatedUsages);
            }

            String logMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "sync.usage.complete"),
                    factoryCode, year, week, actualUsageMap.size()
            );
            log.info(logMessage);

            String resultMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "sync.usage.result"),
                    actualUsageMap.size()
            );
            return AjaxResult.success(resultMessage);

        } catch (Exception e) {
            log.error(I18nUtil.getMessage(WARNING_PREFIX + "sync.usage.error"), e);
            String errorMessage = StringUtils.format(
                    I18nUtil.getMessage(WARNING_PREFIX + "sync.usage.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(errorMessage);
        }
    }

    /**
     * 获取实际用量Map
     */
    private Map<String, BigDecimal> getActualUsageMap(String factoryCode, LocalDate weekStartDate, LocalDate weekEndDate) {
        QueryWrapper<RawMaterialOutboundRecord> outboundWrapper = new QueryWrapper<>();
        outboundWrapper.eq("FACTORY_CODE", factoryCode)
                .between("OUTBOUND_DATE", weekStartDate, weekEndDate);
        List<RawMaterialOutboundRecord> outboundRecords = rawMaterialOutboundRecordMapper.selectList(outboundWrapper);

        return outboundRecords.stream()
                .collect(Collectors.groupingBy(
                        RawMaterialOutboundRecord::getMaterialCode,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                RawMaterialOutboundRecord::getOutboundQty,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * 更新周用量记录的实际用量
     */
    private List<RawWeekUsage> updateWeekUsagesActualQty(List<RawWeekUsage> weekUsages,
                                                         Map<String, BigDecimal> actualUsageMap) {
        List<RawWeekUsage> updatedUsages = new ArrayList<>();

        for (RawWeekUsage usage : weekUsages) {
            BigDecimal actualQty = actualUsageMap.get(usage.getMaterialCode());
            if (actualQty != null) {
                usage.setActualQty(actualQty);
                usage.setRemark(StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "usage.updated.remark"),
                        usage.getActualQty()
                ));
            } else {
                usage.setActualQty(BigDecimal.ZERO);
                usage.setRemark(I18nUtil.getMessage(WARNING_PREFIX + "usage.no.data.remark"));
                log.warn(StringUtils.format(
                        I18nUtil.getMessage(WARNING_PREFIX + "usage.no.data.warning"),
                        usage.getFactoryCode(), usage.getYear(), usage.getWeek(), usage.getMaterialCode()
                ));
            }
            // 重新计算偏差
            usage.calculateDeviation();
            updatedUsages.add(usage);
        }

        return updatedUsages;
    }

    /**
     * 根据年份和周次获取周开始日期
     */
    private LocalDate getWeekStartDate(int year, int month, int week) {
        LocalDate date = LocalDate.of(year, month, 1);
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
     * 将列表分成多个批次
     */
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }
}