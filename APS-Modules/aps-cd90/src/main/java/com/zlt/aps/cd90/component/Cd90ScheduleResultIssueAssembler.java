package com.zlt.aps.cd90.component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.mapper.Cd90ShiftConfigMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mp.api.domain.entity.Cd90ScheduleResultIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final String OUTPUT_TYPE_FIBER = "纤维";
    private static final int EMBRYO_DESC_MAX_LENGTH = 900;

    @Resource
    private Cd90ShiftConfigMapper cd90ShiftConfigMapper;
    @Resource
    private Cd90EngineConstructionMapper constructionMapper;

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
        Map<String, String> embryoDescByCloth = this.loadEmbryoDescriptions(
                sourceList, factoryCode);
        List<Cd90ScheduleResultIssue> result = new ArrayList<>(sourceList.size() * enabledConfigs.size());
        for (Cd90ScheduleResult source : sourceList) {
            for (Cd90ShiftConfig config : enabledConfigs) {
                Cd90ScheduleResultIssue issue = this.convert(source, config,
                        scheduleLocalDate, publishTraceId,
                        embryoDescByCloth.get(source.getClothCode()));
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
        return this.cd90ShiftConfigMapper.selectList(wrapper);
    }

    /**
     * 单条 source × 单个班次配置 → issue。
     * 班次排班日期 = scheduleDate + (SCHEDULE_DAY - 2) 天：
     * SCHEDULE_DAY=1 → scheduleDate - 1（T 日）
     * SCHEDULE_DAY=2 → scheduleDate（T+1 日）
     * SCHEDULE_DAY=3 → scheduleDate + 1（T+2 日）
     */
    private Cd90ScheduleResultIssue convert(Cd90ScheduleResult source, Cd90ShiftConfig config,
                                            LocalDate scheduleLocalDate, String publishTraceId,
                                            String embryoSpecDesc) {
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
        issue.setCxBatchNo(source.getCxBatchNo());
        issue.setOrderNo(source.getOrderNo());
        issue.setScheduleDate(values.scheduleDate != null ? values.scheduleDate : shiftJavaDate);
        issue.setMachineCode(source.getMachineCode());
        issue.setClothCode(source.getClothCode());
        issue.setOutputType(OUTPUT_TYPE_FIBER);
        issue.setOutputCode(source.getClothCode());
        issue.setOutputMaterialCode(source.getClothCode());
        issue.setEmbryoSpecDesc(embryoSpecDesc);
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
        issue.setStockQty(source.getStockQty());
        issue.setSupplyTime(source.getSupplyTime());
        issue.setCxClass1Plan(source.getClass1CxPlanQty());
        issue.setCxClass2Plan(source.getClass2CxPlanQty());
        issue.setCxClass3Plan(source.getClass3CxPlanQty());
        issue.setCxClass4Plan(source.getClass4CxPlanQty());
        issue.setRemark(source.getRemark());
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
            values.scheduleDate = (Date) source.getFieldValueByFieldName(
                    prefix + "ScheduleDate");
            values.cxPlanQty = (Double) source.getFieldValueByFieldName(
                    prefix + "CxPlanQty");
            values.planQty = (Double) source.getFieldValueByFieldName(
                    prefix + "PlanQty");
            values.finishQty = (Double) source.getFieldValueByFieldName(
                    prefix + "FinishQty");
            values.produceOrder = (Integer) source.getFieldValueByFieldName(
                    prefix + "ProduceOrder");
            values.finishRate = (Double) source.getFieldValueByFieldName(
                    prefix + "FinishRate");
            values.analysis = (String) source.getFieldValueByFieldName(
                    prefix + "Analysis");
            values.analysisInput = (String) source.getFieldValueByFieldName(
                    prefix + "AnalysisInput");
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

    /**
     * 按帘布编码汇总施工信息中的胎胚描述。
     *
     * @param sourceList 待发布排程结果
     * @param factoryCode 工厂编码
     * @return 帘布编码对应的去重胎胚描述
     */
    private Map<String, String> loadEmbryoDescriptions(
            List<Cd90ScheduleResult> sourceList, String factoryCode) {
        Set<String> clothCodes = sourceList.stream()
                .map(Cd90ScheduleResult::getClothCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (clothCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<MdmConstructionInfo> wrapper =
                new LambdaQueryWrapper<>();
        wrapper.eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                .and(condition -> condition
                        .in(MdmConstructionInfo::getTireFabricCode1, clothCodes)
                        .or().in(MdmConstructionInfo::getTireFabricCode2, clothCodes)
                        .or().in(MdmConstructionInfo::getTireFabricCode3, clothCodes));
        List<MdmConstructionInfo> constructions =
                this.constructionMapper.selectList(wrapper);
        if (constructions == null || constructions.isEmpty()) {
            return Collections.emptyMap();
        }
        return clothCodes.stream().collect(Collectors.toMap(
                Function.identity(),
                clothCode -> this.joinEmbryoDescriptions(
                        constructions, clothCode),
                (first, second) -> first,
                LinkedHashMap::new));
    }

    /** 汇总包含指定帘布的胎胚描述，并限制在 MES 字段长度内。 */
    private String joinEmbryoDescriptions(
            List<MdmConstructionInfo> constructions, String clothCode) {
        String value = constructions.stream()
                .filter(construction -> this.containsCloth(
                        construction, clothCode))
                .map(MdmConstructionInfo::getEmbryoDesc)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("/"));
        return value.length() <= EMBRYO_DESC_MAX_LENGTH
                ? value : value.substring(0, EMBRYO_DESC_MAX_LENGTH);
    }

    /** 判断施工信息是否包含指定帘布。 */
    private boolean containsCloth(
            MdmConstructionInfo construction, String clothCode) {
        return Objects.equals(clothCode, construction.getTireFabricCode1())
                || Objects.equals(clothCode, construction.getTireFabricCode2())
                || Objects.equals(clothCode,
                construction.getTireFabricCode3());
    }

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
