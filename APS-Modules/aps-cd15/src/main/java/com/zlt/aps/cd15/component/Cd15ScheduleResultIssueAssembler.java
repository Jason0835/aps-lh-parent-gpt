package com.zlt.aps.cd15.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultIssue;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.mapper.Cd15ShiftConfigMapper;
import com.zlt.aps.common.core.constant.ApsConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/** 按启用班次把斜裁主结果展开为 MES 下发数据。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Cd15ScheduleResultIssueAssembler {

    private final Cd15ShiftConfigMapper shiftConfigMapper;

    /**
     * 装配 MES 下发列表。
     *
     * @param sourceList 待发布结果
     * @param scheduleDate 排程日期
     * @param factoryCode 工厂编码
     * @param publishTraceId 发布追踪号
     * @return 按班次展开的数据
     */
    public List<Cd15ScheduleResultIssue> assemble(
            List<Cd15ScheduleResult> sourceList,
            Date scheduleDate,
            String factoryCode,
            String publishTraceId) {
        if (sourceList == null || sourceList.isEmpty()
                || scheduleDate == null) {
            return Collections.emptyList();
        }
        List<Cd15ShiftConfig> shiftConfigs = this.listEnabledConfigs(factoryCode);
        if (shiftConfigs.isEmpty()) {
            log.warn("斜裁排程发布未找到启用班次, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        LocalDate localScheduleDate = this.toLocalDate(scheduleDate);
        List<Cd15ScheduleResultIssue> issues =
                new ArrayList<>(sourceList.size() * shiftConfigs.size());
        sourceList.forEach(source -> shiftConfigs.forEach(config -> {
            Cd15ScheduleResultIssue issue = this.convert(
                    source, config, localScheduleDate, publishTraceId);
            if (issue != null) {
                issues.add(issue);
            }
        }));
        return issues;
    }

    /** 查询启用班次并稳定排序。 */
    private List<Cd15ShiftConfig> listEnabledConfigs(String factoryCode) {
        return shiftConfigMapper.selectList(
                        new LambdaQueryWrapper<Cd15ShiftConfig>()
                                .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                                .eq(Cd15ShiftConfig::getIsActive, 1))
                .stream()
                .sorted(Comparator
                        .comparing(Cd15ShiftConfig::getScheduleDay,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Cd15ShiftConfig::getDayShiftOrder,
                                Comparator.nullsLast(Integer::compareTo)))
                .collect(java.util.stream.Collectors.toList());
    }

    /** 把一条结果的一个 CLASS 班次转换为下发数据。 */
    private Cd15ScheduleResultIssue convert(
            Cd15ScheduleResult source,
            Cd15ShiftConfig config,
            LocalDate scheduleDate,
            String publishTraceId) {
        String classField = config.getClassField();
        if (classField == null || classField.trim().isEmpty()
                || config.getScheduleDay() == null) {
            return null;
        }
        String fieldPrefix = classField.trim().toLowerCase();
        ClassFieldValues values = this.readClassValues(source, fieldPrefix);
        if (values == null) {
            return null;
        }
        LocalDate shiftDate = scheduleDate.plusDays(
                config.getScheduleDay() - 2L);
        Date fallbackDate = Date.from(shiftDate.atStartOfDay(
                ZoneId.systemDefault()).toInstant());

        Cd15ScheduleResultIssue issue = new Cd15ScheduleResultIssue();
        issue.setCd15BatchNo(source.getCd15BatchNo());
        issue.setOrderNo(source.getOrderNo());
        issue.setGroupNo(source.getGroupNo());
        issue.setScheduleDate(values.scheduleDate == null
                ? fallbackDate : values.scheduleDate);
        issue.setMachineCode(source.getMachineCode());
        issue.setSteelStripCode(source.getSteelStripCode());
        issue.setBigRollCode(source.getBigRollCode());
        issue.setStorageLaneCode(source.getStorageLaneCode());
        issue.setCuttingAngle(source.getCuttingAngle());
        issue.setCutMode(source.getCutMode());
        issue.setShiftName(config.getShiftName());
        issue.setClassField(fieldPrefix);
        issue.setScheduleDay(config.getScheduleDay());
        issue.setDayShiftOrder(config.getDayShiftOrder());
        issue.setPlanQty(values.planQty);
        issue.setCxPlanQty(values.cxPlanQty);
        issue.setFinishQty(values.finishQty);
        issue.setProduceOrder(values.produceOrder);
        issue.setFinishRate(values.finishRate);
        issue.setAnalysis(values.analysis);
        issue.setAnalysisInput(values.analysisInput);
        issue.setCraftWidth(source.getCraftWidth());
        issue.setUnitConsumeMillimeter(source.getUnitConsumeMillimeter());
        issue.setCurlLength(source.getCurlLength());
        issue.setCordWidth(source.getCordWidth());
        issue.setSourceType(source.getSourceType());
        issue.setProductionStatus(source.getProductionStatus());
        issue.setClearExistingPlan(values.clearExistingPlan);
        issue.setFactoryCode(source.getFactoryCode());
        issue.setPublishTraceId(publishTraceId);
        return issue;
    }

    /**
     * 读取 CLASS 动态字段。曾成功发布且转为待发布的结果会把空计划展开为0，
     * 使 MES 能清除转机台、调量或滚动前的旧班次计划。
     */
    private ClassFieldValues readClassValues(
            Cd15ScheduleResult source, String fieldPrefix) {
        try {
            ClassFieldValues values = new ClassFieldValues();
            values.scheduleDate = (Date) source.getFieldValueByFieldName(
                    fieldPrefix + "ScheduleDate");
            values.cxPlanQty = (Double) source.getFieldValueByFieldName(
                    fieldPrefix + "CxPlanQty");
            values.planQty = (Double) source.getFieldValueByFieldName(
                    fieldPrefix + "PlanQty");
            values.finishQty = (Double) source.getFieldValueByFieldName(
                    fieldPrefix + "FinishQty");
            values.produceOrder = (Integer) source.getFieldValueByFieldName(
                    fieldPrefix + "ProduceOrder");
            values.finishRate = (Double) source.getFieldValueByFieldName(
                    fieldPrefix + "FinishRate");
            values.analysis = (String) source.getFieldValueByFieldName(
                    fieldPrefix + "Analysis");
            values.analysisInput = (String) source.getFieldValueByFieldName(
                    fieldPrefix + "AnalysisInput");
            boolean previouslyPublished =
                    source.getPublishSuccessCount() != null
                            && source.getPublishSuccessCount() > 0;
            boolean pendingRepublish = ApsConstant.WAIT_RELEASING.equals(
                    source.getReleaseStatus());
            if (values.planQty == null && values.cxPlanQty == null
                    && !(previouslyPublished && pendingRepublish)) {
                return null;
            }
            if (values.planQty == null
                    && previouslyPublished && pendingRepublish) {
                values.planQty = 0D;
                values.clearExistingPlan = true;
            } else {
                values.clearExistingPlan = false;
            }
            return values;
        } catch (IllegalArgumentException exception) {
            log.warn("读取斜裁排程班次字段失败, resultId={}, classField={}, reason={}",
                    source.getId(), fieldPrefix, exception.getMessage());
            return null;
        }
    }

    /** 转换日期并兼容 java.sql.Date。 */
    private LocalDate toLocalDate(Date value) {
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** CLASS 动态字段值。 */
    private static class ClassFieldValues {
        private Date scheduleDate;
        private Double cxPlanQty;
        private Double planQty;
        private Double finishQty;
        private Integer produceOrder;
        private Double finishRate;
        private String analysis;
        private String analysisInput;
        private Boolean clearExistingPlan;
    }
}
