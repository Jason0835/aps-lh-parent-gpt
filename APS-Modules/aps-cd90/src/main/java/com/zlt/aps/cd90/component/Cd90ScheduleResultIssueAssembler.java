package com.zlt.aps.cd90.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.mapper.Cd90ShiftConfigMapper;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直裁排程结果下发 MES 装配组件。
 *
 * <p>按 t_cd90_shift_config 启用班次配置动态展开：
 * 每条 Cd90ScheduleResult × 每个启用班次 → 一条 Cd90ScheduleResultIssue。
 * 班次排班日期由 SCHEDULE_DAY 推导：day1 = scheduleDate - 1, day2 = scheduleDate, day3 = scheduleDate + 1。
 * 不硬编码 CLASS1~CLASS6，支持 CLASS7/CLASS8 启用后自动参与下发。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class Cd90ScheduleResultIssueAssembler {

    @Resource
    private Cd90ShiftConfigMapper cd90ShiftConfigMapper;

    /**
     * 装配下发列表。
     *
     * @param sourceList 待下发的直裁排程结果
     * @param scheduleDate 排程日期（T+1 日）
     * @param factoryCode 工厂编码（用于过滤班次配置）
     * @param publishTraceId 发布追踪 ID，贯穿 MES 调用
     * @return 展开后的下发列表
     */
    public List<Cd90ScheduleResultIssue> assemble(List<Cd90ScheduleResult> sourceList,
                                                  Date scheduleDate,
                                                  String factoryCode,
                                                  String publishTraceId) {
        if (sourceList == null || sourceList.isEmpty() || scheduleDate == null) {
            return Collections.emptyList();
        }

        List<Cd90ShiftConfig> enabledConfigs = listEnabledConfigs(factoryCode);
        if (enabledConfigs.isEmpty()) {
            log.warn("直裁排程发布: 工厂 {} 未启用任何班次配置，无法装配下发列表", factoryCode);
            return Collections.emptyList();
        }

        LocalDate scheduleLocalDate = toLocalDate(scheduleDate);
        List<Cd90ScheduleResultIssue> result = new ArrayList<>(sourceList.size() * enabledConfigs.size());
        for (Cd90ScheduleResult source : sourceList) {
            for (Cd90ShiftConfig config : enabledConfigs) {
                Cd90ScheduleResultIssue issue = convert(source, config, scheduleLocalDate, publishTraceId);
                if (issue != null) {
                    result.add(issue);
                }
            }
        }
        return result;
    }

    /**
     * 查询启用的班次配置，按 SCHEDULE_DAY 升序、DAY_SHIFT_ORDER 升序排序，保证 day1/day2/day3 顺序稳定。
     */
    private List<Cd90ShiftConfig> listEnabledConfigs(String factoryCode) {
        LambdaQueryWrapper<Cd90ShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                .eq(Cd90ShiftConfig::getIsActive, 1)
                .orderByAsc(Cd90ShiftConfig::getScheduleDay)
                .orderByAsc(Cd90ShiftConfig::getDayShiftOrder);
        return cd90ShiftConfigMapper.selectList(wrapper);
    }

    /**
     * 单条 source × 单个班次配置 → issue。
     * 班次排班日期 = scheduleDate + (SCHEDULE_DAY - 2) 天：
     * SCHEDULE_DAY=1 → scheduleDate - 1（T 日）
     * SCHEDULE_DAY=2 → scheduleDate（T+1 日）
     * SCHEDULE_DAY=3 → scheduleDate + 1（T+2 日）
     */
    private Cd90ScheduleResultIssue convert(Cd90ScheduleResult source, Cd90ShiftConfig config,
                                            LocalDate scheduleLocalDate, String publishTraceId) {
        String classField = config.getClassField();
        if (classField == null || classField.isEmpty()) {
            return null;
        }
        String prefix = normalizeClassPrefix(classField);
        ClassFieldValues values = readClassValues(source, prefix);
        if (values == null) {
            return null;
        }

        LocalDate shiftDate = scheduleLocalDate.plusDays(config.getScheduleDay() - 2L);
        Date shiftJavaDate = Date.from(shiftDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Cd90ScheduleResultIssue issue = new Cd90ScheduleResultIssue();
        issue.setCd90BatchNo(source.getBatchNo());
        issue.setOrderNo(source.getOrderNo());
        issue.setScheduleDate(values.scheduleDate != null ? values.scheduleDate : shiftJavaDate);
        issue.setMachineCode(source.getMachineCode());
        issue.setClothCode(source.getClothCode());
        issue.setBigRollCode(source.getBigRollCode());
        issue.setStorageLaneCode(source.getStorageLaneCode());
        issue.setShiftName(config.getShiftName());
        issue.setClassField(classField.toLowerCase());
        issue.setScheduleDay(config.getScheduleDay());
        issue.setDayShiftOrder(config.getDayShiftOrder());
        issue.setPlanQty(values.planQty);
        issue.setCxPlanQty(values.cxPlanQty);
        issue.setFinishQty(values.finishQty);
        issue.setProduceOrder(values.produceOrder);
        issue.setFinishRate(values.finishRate);
        issue.setAnalysis(values.analysis);
        issue.setAnalysisInput(values.analysisInput);
        issue.setUnitConsume(source.getUnitConsume());
        issue.setFactoryCode(source.getFactoryCode());
        issue.setPublishTraceId(publishTraceId);
        return issue;
    }

    /**
     * 把班次配置中的 CLASS_FIELD（形如 "class1"、"CLASS1"、"Class1"）统一为小写前缀 "class1"。
     */
    private String normalizeClassPrefix(String classField) {
        return classField.toLowerCase();
    }

    /**
     * 通过反射按前缀读取 Cd90ScheduleResult 对应班次字段。
     * 例 prefix="class1" → class1ScheduleDate / class1CxPlanQty / class1PlanQty / class1FinishQty / class1ProduceOrder / class1FinishRate / class1Analysis / class1AnalysisInput。
     * 任一计划量字段为 null 时返回 null（表示该班次无计划量，不下发）。
     */
    private ClassFieldValues readClassValues(Cd90ScheduleResult source, String prefix) {
        try {
            ClassFieldValues values = new ClassFieldValues();
            values.scheduleDate = (Date) getFieldValue(source, prefix + "ScheduleDate");
            values.cxPlanQty = (Double) getFieldValue(source, prefix + "CxPlanQty");
            values.planQty = (Double) getFieldValue(source, prefix + "PlanQty");
            values.finishQty = (Double) getFieldValue(source, prefix + "FinishQty");
            values.produceOrder = (Integer) getFieldValue(source, prefix + "ProduceOrder");
            values.finishRate = (Double) getFieldValue(source, prefix + "FinishRate");
            values.analysis = (String) getFieldValue(source, prefix + "Analysis");
            values.analysisInput = (String) getFieldValue(source, prefix + "AnalysisInput");
            if (values.planQty == null && values.cxPlanQty == null) {
                return null;
            }
            return values;
        } catch (Exception e) {
            log.warn("读取 Cd90ScheduleResult 班次字段失败, prefix={}, id={}, err={}",
                    prefix, source.getId(), e.getMessage());
            return null;
        }
    }

    private Object getFieldValue(Cd90ScheduleResult source, String fieldName) throws Exception {
        Field field = fieldCache.get(fieldName);
        if (field == null && !fieldCache.containsKey(fieldName)) {
            try {
                Field f = Cd90ScheduleResult.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                field = f;
            } catch (NoSuchFieldException e) {
                log.warn("Cd90ScheduleResult 字段不存在: {}", fieldName);
            }
            fieldCache.put(fieldName, field);
        }
        if (field == null) {
            return null;
        }
        return field.get(source);
    }

    /** 班次字段反射缓存，避免每次 assemble 重复 getDeclaredField。key=fieldName，value=Field 或 null（表示字段不存在） */
    private final Map<String, Field> fieldCache = new HashMap<>();

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static class ClassFieldValues {
        Date scheduleDate;
        Double cxPlanQty;
        Double planQty;
        Double finishQty;
        Integer produceOrder;
        Double finishRate;
        String analysis;
        String analysisInput;
    }
}
