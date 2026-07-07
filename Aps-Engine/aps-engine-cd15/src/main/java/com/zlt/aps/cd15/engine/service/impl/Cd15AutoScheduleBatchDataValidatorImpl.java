package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.model.Cd15BatchDataCheckResult;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleBatchDataValidator;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 斜裁自动排程批次级数据先行检查实现。
 *
 * <p>本检查只处理进入自动排程前必须同步拦截的公共数据问题；
 * 角度已配置但后续找不到机台时，由算法阶段写入未排结果。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleBatchDataValidatorImpl implements Cd15AutoScheduleBatchDataValidator {

    private static final int CONSTRUCTION_LAYERS = 3;
    private static final int CLASS_COUNT = 8;
    private static final String DATA_MISSING = "DATA_MISSING";
    private static final String ANGLE_WIDTH_CONFIG_MISSING = "ANGLE_WIDTH_CONFIG_MISSING";
    private static final String[] MAIN_LAYER_CODE_COLUMNS = {"BELT_CODE1", "BELT_CODE2", "BELT_CODE3"};
    private static final String[] MAIN_LAYER_CRAFT_COLUMNS = {"BELT_CRAFT1", "BELT_CRAFT2", "BELT_CRAFT3"};
    private static final String[] MAIN_LAYER_LENGTH_COLUMNS = {"BELT1_LENGTH", "BELT2_LENGTH", "BELT3_LENGTH"};

    private final Cd15EngineCxScheduleMapper cxScheduleMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;

    @Override
    public Cd15BatchDataCheckResult check(String factoryCode, LocalDate scheduleDate) {
        if (!StringUtils.hasText(factoryCode)) {
            return Cd15BatchDataCheckResult.builder()
                    .addError("工厂编码", DATA_MISSING, "自动排程工厂编码不能为空")
                    .build();
        }
        if (scheduleDate == null) {
            return Cd15BatchDataCheckResult.builder()
                    .addError("排程日期", DATA_MISSING, "自动排程排程日期不能为空")
                    .build();
        }

        Cd15BatchDataCheckResult.Builder builder = Cd15BatchDataCheckResult.builder();
        List<CxScheduleResult> formingSchedules = this.checkFormingSchedule(builder, factoryCode, scheduleDate);
        this.checkMachineInfo(builder, factoryCode);
        ConstructionCheckScope scope = this.checkConstructionInfo(builder, factoryCode, formingSchedules);
        this.checkCurlLength(builder, factoryCode, scope.getSteelStripCodes());
        this.checkAngleWidthMapping(builder, factoryCode, scope.getCuttingAngles());

        Cd15BatchDataCheckResult result = builder.build();
        if (result.isFailed()) {
            log.warn("[斜裁自动排程] 批次级数据先行检查失败, factoryCode={}, scheduleDate={}, errorCount={}, warningCount={}",
                    factoryCode, scheduleDate, result.getErrors().size(), result.getWarnings().size());
        } else {
            log.info("[斜裁自动排程] 批次级数据先行检查通过, factoryCode={}, scheduleDate={}",
                    factoryCode, scheduleDate);
        }
        return result;
    }

    /** 检查排程日成型计划是否存在。 */
    private List<CxScheduleResult> checkFormingSchedule(Cd15BatchDataCheckResult.Builder builder,
                                                        String factoryCode,
                                                        LocalDate scheduleDate) {
        List<CxScheduleResult> schedules = cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .eq(CxScheduleResult::getScheduleDate, Date.valueOf(scheduleDate)));
        if (schedules == null || schedules.isEmpty()) {
            builder.addError("成型计划", DATA_MISSING,
                    "未找到排程日 " + scheduleDate + " 的成型排程记录",
                    "请先在成型排程页面生成排程日 " + scheduleDate + " 的成型排程");
            return new ArrayList<>();
        }
        return schedules;
    }

    /** 检查至少存在一台启用斜裁机台。 */
    private void checkMachineInfo(Cd15BatchDataCheckResult.Builder builder, String factoryCode) {
        Long count = machineInfoMapper.selectCount(Wrappers.<Cd15MachineInfo>lambdaQuery()
                .eq(Cd15MachineInfo::getFactoryCode, factoryCode)
                .eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1));
        if (count == null || count == 0L) {
            builder.addError("机台档案", DATA_MISSING,
                    "未找到启用的斜裁机台",
                    "请在斜裁机台档案页面启用至少一台机台");
        }
    }

    /** 检查施工记录和施工层位字段。 */
    private ConstructionCheckScope checkConstructionInfo(Cd15BatchDataCheckResult.Builder builder,
                                                         String factoryCode,
                                                         List<CxScheduleResult> formingSchedules) {
        ConstructionCheckScope scope = new ConstructionCheckScope();
        if (formingSchedules == null || formingSchedules.isEmpty()) {
            return scope;
        }

        Set<String> embryoCodes = formingSchedules.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        formingSchedules.stream()
                .filter(schedule -> !StringUtils.hasText(schedule.getEmbryoCode()))
                .forEach(schedule -> builder.addError("成型计划", DATA_MISSING,
                        "成型计划存在胎胚代号为空的记录",
                        "请检查成型排程数据的胎胚代号"));

        Set<String> constructionVersions = formingSchedules.stream()
                .flatMap(schedule -> IntStream.rangeClosed(1, CLASS_COUNT)
                        .filter(classIndex -> this.hasPositivePlan(schedule, classIndex))
                        .mapToObj(classIndex -> this.readString(schedule, String.format("class%dRecipeNo", classIndex))))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> constructionPairs = new LinkedHashSet<>();
        formingSchedules.forEach(schedule -> this.collectConstructionPairs(builder, schedule, constructionPairs));

        if (embryoCodes.isEmpty() || constructionVersions.isEmpty()) {
            return scope;
        }
        List<MdmConstructionInfo> constructions = constructionMapper.selectList(
                Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions));
        Map<String, MdmConstructionInfo> constructionByKey = constructions.stream()
                .collect(Collectors.toMap(
                        item -> item.getConstructionCode() + "@" + item.getConstructionVersion(),
                        item -> item, (left, right) -> left, LinkedHashMap::new));

        constructionPairs.forEach(pair -> this.checkConstructionPair(builder, constructionByKey, pair, scope));
        return scope;
    }

    /** 收集有正需求班次的胎胚和施工版本配对。 */
    private void collectConstructionPairs(Cd15BatchDataCheckResult.Builder builder,
                                          CxScheduleResult schedule,
                                          Set<String> constructionPairs) {
        String embryoCode = schedule.getEmbryoCode();
        if (!StringUtils.hasText(embryoCode)) {
            return;
        }
        IntStream.rangeClosed(1, CLASS_COUNT)
                .filter(classIndex -> this.hasPositivePlan(schedule, classIndex))
                .forEach(classIndex -> {
                    String fieldName = String.format("CLASS%d_RECIPE_NO", classIndex);
                    String recipeNo = this.readString(schedule, String.format("class%dRecipeNo", classIndex));
                    if (!StringUtils.hasText(recipeNo)) {
                        builder.addError("成型计划", DATA_MISSING,
                                "胎胚 " + embryoCode + " 的 " + fieldName + " 施工版本为空",
                                "请检查成型排程数据各班次施工版本");
                    } else {
                        constructionPairs.add(embryoCode + "@" + recipeNo);
                    }
                });
    }

    /** 检查单个施工配对。 */
    private void checkConstructionPair(Cd15BatchDataCheckResult.Builder builder,
                                       Map<String, MdmConstructionInfo> constructionByKey,
                                       String pair,
                                       ConstructionCheckScope scope) {
        String[] parts = pair.split("@", 2);
        String constructionCode = parts[0];
        String constructionVersion = parts[1];
        MdmConstructionInfo construction = constructionByKey.get(pair);
        if (construction == null) {
            builder.addError("施工信息", DATA_MISSING,
                    "胎胚 " + constructionCode + " 施工版本 " + constructionVersion + " 未维护",
                    "请在施工信息页面维护对应胎胚和版本的施工资料");
            return;
        }
        this.checkConstructionFields(builder, construction, scope);
    }

    /** 校验施工主钢带、左右加强层和裁断角度字段。 */
    private void checkConstructionFields(Cd15BatchDataCheckResult.Builder builder,
                                         MdmConstructionInfo construction,
                                         ConstructionCheckScope scope) {
        String code = construction.getConstructionCode();
        String version = construction.getConstructionVersion();
        String prefix = "胎胚 " + code + " 施工版本 " + version + " ";
        String cuttingAngle = this.trim(construction.getBeltCuttingAngle());

        if (!StringUtils.hasText(construction.getCordSpec())) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + "大卷代码(CORD_SPEC)缺失",
                    "请在施工信息页面维护大卷代码");
        }
        if (!StringUtils.hasText(cuttingAngle)) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + "裁断角度(BELT_CUTTING_ANGLE)缺失",
                    "请在施工信息页面维护钢带裁断角度");
        }

        IntStream.rangeClosed(1, CONSTRUCTION_LAYERS)
                .forEach(layer -> this.checkMainLayer(builder, construction, scope, prefix, cuttingAngle, layer));
        this.checkReinforcement(builder, construction, scope, prefix, cuttingAngle,
                "左加强层", "BELT_CODE_LEFT_CODE", "BELT_CODE_LEFT_CRAFT", "BELT_CODE_LEFT_LENGTH",
                "beltCodeLeftCode", "beltCodeLeftCraft", "beltCodeLeftLength");
        this.checkReinforcement(builder, construction, scope, prefix, cuttingAngle,
                "右加强层", "BELT_CODE_RIGHT_CODE", "BELT_CODE_RIGHT_CRAFT", "BELT_CODE_RIGHT_LENGTH",
                "beltCodeRightCode", "beltCodeRightCraft", "beltCodeRightLength");
    }

    /** 校验1至3层主钢带字段。 */
    private void checkMainLayer(Cd15BatchDataCheckResult.Builder builder,
                                MdmConstructionInfo construction,
                                ConstructionCheckScope scope,
                                String prefix,
                                String cuttingAngle,
                                int layer) {
        String steelStripCode = this.readString(construction, "beltCode" + layer);
        if (!StringUtils.hasText(steelStripCode)) {
            return;
        }
        scope.addMaterial(steelStripCode, cuttingAngle);
        BigDecimal craftWidth = this.readBigDecimal(construction, "beltCraft" + layer);
        if (!this.isPositive(craftWidth)) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + "第 " + layer + " 层钢带 " + steelStripCode
                            + " 斜裁宽度缺失或非正(BELT_CRAFT" + layer + ")",
                    "请维护 BELT_CRAFT" + layer + " 且大于0");
        }
        BigDecimal unitConsume = this.readBigDecimal(construction, "belt" + layer + "Length");
        if (!this.isPositive(unitConsume)) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + "第 " + layer + " 层钢带 " + steelStripCode
                            + " 单耗缺失或非正(BELT" + layer + "_LENGTH)",
                    "请维护 BELT" + layer + "_LENGTH 且大于0");
        }
    }

    /** 校验左右加强层字段。 */
    private void checkReinforcement(Cd15BatchDataCheckResult.Builder builder,
                                    MdmConstructionInfo construction,
                                    ConstructionCheckScope scope,
                                    String prefix,
                                    String cuttingAngle,
                                    String layerName,
                                    String codeColumn,
                                    String craftColumn,
                                    String lengthColumn,
                                    String codeProperty,
                                    String craftProperty,
                                    String lengthProperty) {
        String steelStripCode = this.readString(construction, codeProperty);
        if (!StringUtils.hasText(steelStripCode)) {
            return;
        }
        scope.addMaterial(steelStripCode, cuttingAngle);
        BigDecimal craftWidth = this.readBigDecimal(construction, craftProperty);
        if (!this.isPositive(craftWidth)) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + layerName + "钢带 " + steelStripCode + " 斜裁宽度缺失或非正(" + craftColumn + ")",
                    "请维护 " + craftColumn + " 且大于0");
        }
        BigDecimal unitConsume = this.readBigDecimal(construction, lengthProperty);
        if (!this.isPositive(unitConsume)) {
            builder.addError("施工信息", DATA_MISSING,
                    prefix + layerName + "钢带 " + steelStripCode + " 单耗缺失或非正(" + lengthColumn + ")",
                    "请维护 " + lengthColumn + " 且大于0");
        }
        log.debug("[斜裁自动排程] 已纳入加强层检查, column={}, steelStripCode={}", codeColumn, steelStripCode);
    }

    /** 检查施工材料使用到的卷曲长度配置。 */
    private void checkCurlLength(Cd15BatchDataCheckResult.Builder builder,
                                 String factoryCode,
                                 Set<String> steelStripCodes) {
        if (steelStripCodes == null || steelStripCodes.isEmpty()) {
            return;
        }
        Map<String, Cd15CurlLength> curlBySteel = curlLengthMapper.selectList(
                        Wrappers.<Cd15CurlLength>lambdaQuery()
                                .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                                .in(Cd15CurlLength::getSteelStripCode, steelStripCodes))
                .stream()
                .collect(Collectors.toMap(Cd15CurlLength::getSteelStripCode,
                        item -> item, (left, right) -> left, LinkedHashMap::new));
        steelStripCodes.forEach(steelStripCode -> {
            Cd15CurlLength curl = curlBySteel.get(steelStripCode);
            if (curl == null) {
                builder.addError("卷曲长度", DATA_MISSING,
                        "钢带 " + steelStripCode + " 卷曲长度未维护",
                        "请在斜裁卷曲长度配置页面维护钢带 " + steelStripCode + " 的卷曲长度");
                return;
            }
            if (curl.getCurlLength() == null || curl.getCurlLength() <= 0D) {
                builder.addError("卷曲长度", DATA_MISSING,
                        "钢带 " + steelStripCode + " 卷曲长度非正",
                        "请在斜裁卷曲长度配置页面维护钢带 " + steelStripCode + " 的卷曲长度且大于0");
            }
        });
    }

    /** 检查出现正需求材料的裁断角度是否有有效最大宽度配置。 */
    private void checkAngleWidthMapping(Cd15BatchDataCheckResult.Builder builder,
                                        String factoryCode,
                                        Set<String> cuttingAngles) {
        if (cuttingAngles == null || cuttingAngles.isEmpty()) {
            return;
        }
        Map<String, Cd15AngleWidthMapping> mappingByAngle = angleWidthMappingMapper.selectList(
                        Wrappers.<Cd15AngleWidthMapping>lambdaQuery()
                                .eq(Cd15AngleWidthMapping::getFactoryCode, factoryCode)
                                .in(Cd15AngleWidthMapping::getCutAngle, cuttingAngles))
                .stream()
                .collect(Collectors.toMap(item -> this.trim(item.getCutAngle()),
                        item -> item, (left, right) -> left, LinkedHashMap::new));
        cuttingAngles.forEach(angle -> {
            Cd15AngleWidthMapping mapping = mappingByAngle.get(angle);
            if (mapping == null || !this.isPositive(mapping.getClothWidthMax())) {
                builder.addError("角度宽度配置", ANGLE_WIDTH_CONFIG_MISSING,
                        "裁断角度 " + angle + " 未维护有效角度宽度配置(CLOTH_WIDTH_MAX)",
                        "请在角度宽度对应关系页面维护角度 " + angle + " 的最大宽度且大于0");
            }
        });
    }

    private boolean hasPositivePlan(CxScheduleResult schedule, int classIndex) {
        BigDecimal planQty = this.readBigDecimal(schedule, String.format("class%dPlanQty", classIndex));
        return this.isPositive(planQty);
    }

    private String readString(Object source, String fieldName) {
        Object value = this.readValue(source, fieldName);
        return value == null ? null : value.toString().trim();
    }

    private BigDecimal readBigDecimal(Object source, String fieldName) {
        Object value = this.readValue(source, fieldName);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value != null && StringUtils.hasText(value.toString())) {
            try {
                return new BigDecimal(value.toString().trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private Object readValue(Object source, String fieldName) {
        if (source == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        String methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method method = source.getClass().getMethod(methodName);
            return method.invoke(source);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取字段失败: " + source.getClass().getSimpleName() + "." + fieldName, exception);
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 批次检查过程中收集需继续校验的材料集合。 */
    private static class ConstructionCheckScope {
        private final Set<String> steelStripCodes = new LinkedHashSet<>();
        private final Set<String> cuttingAngles = new LinkedHashSet<>();

        void addMaterial(String steelStripCode, String cuttingAngle) {
            if (StringUtils.hasText(steelStripCode)) {
                this.steelStripCodes.add(steelStripCode.trim());
            }
            if (StringUtils.hasText(cuttingAngle)) {
                this.cuttingAngles.add(cuttingAngle.trim());
            }
        }

        Set<String> getSteelStripCodes() {
            return steelStripCodes;
        }

        Set<String> getCuttingAngles() {
            return cuttingAngles;
        }
    }
}