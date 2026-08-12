package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.algorithm.Cd90ShiftWindowResolver;
import com.zlt.aps.cd90.engine.constant.Cd90AutoScheduleParamCode;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleParamsMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCurlLengthMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineInfoMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStorageLaneMapper;
import com.zlt.aps.cd90.engine.model.Cd90BatchDataCheckResult;
import com.zlt.aps.cd90.engine.model.Cd90ShiftDescriptor;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleBatchDataValidator;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 直裁自动排程批次级数据先行检查实现。
 * <p>
 * 检查口径与 Cd90AutoScheduleInputServiceImpl / Cd90MachineResourceServiceImpl 对齐：
 * 成型计划按 scheduleDate-1 至 scheduleDate+3 的日期范围查询；
 * 机台档案按 factoryCode + status=启用 查询；
 * 施工信息按 CONSTRUCTION_CODE + CONSTRUCTION_VERSION 配对查询，版本来源为成型各班 CLASSn_RECIPE_NO；
 * 卷曲长度按 factoryCode + CLOTH_CODE 查询，CURL_LENGTH 必须 > 0。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleBatchDataValidatorImpl implements Cd90AutoScheduleBatchDataValidator {

    private static final int CONSTRUCTION_LAYERS = 3;
    private static final int ACTIVE = 1;
    private static final String[] CLASS_RECIPE_FIELDS = {
            "CLASS1_RECIPE_NO", "CLASS2_RECIPE_NO", "CLASS3_RECIPE_NO", "CLASS4_RECIPE_NO",
            "CLASS5_RECIPE_NO", "CLASS6_RECIPE_NO", "CLASS7_RECIPE_NO", "CLASS8_RECIPE_NO"
    };

    private final Cd90EngineCxScheduleMapper cxScheduleMapper;
    private final Cd90EngineConstructionMapper constructionMapper;
    private final Cd90EngineCurlLengthMapper curlLengthMapper;
    private final Cd90EngineMachineInfoMapper machineInfoMapper;
    private final Cd90EngineStorageLaneMapper storageLaneMapper;
    private final Cd90AutoScheduleParamsMapper paramsMapper;
    private final Cd90AutoScheduleShiftMapper shiftConfigMapper;
    private final Cd90ShiftWindowResolver shiftWindowResolver;

    @Override
    public Cd90BatchDataCheckResult check(String factoryCode, LocalDate scheduleDate) {
        if (!StringUtils.hasText(factoryCode)) {
            return Cd90BatchDataCheckResult.builder()
                    .addError("工厂编码", "DATA_MISSING", "自动排程工厂编码不能为空")
                    .build();
        }
        if (scheduleDate == null) {
            return Cd90BatchDataCheckResult.builder()
                    .addError("排程日期", "DATA_MISSING", "自动排程排程日期不能为空")
                    .build();
        }

        Cd90BatchDataCheckResult.Builder builder = Cd90BatchDataCheckResult.builder();
        List<CxScheduleResult> formingSchedules = checkFormingSchedule(builder, factoryCode, scheduleDate);
        checkMachineInfo(builder, factoryCode);
        Set<String> clothCodes = checkConstructionInfo(builder, factoryCode, formingSchedules);
        checkCurlLength(builder, factoryCode, clothCodes);
        this.checkStorageLaneLimit(builder, factoryCode);

        Cd90BatchDataCheckResult result = builder.build();
        if (result.isFailed()) {
            log.warn("[直裁自动排程] 批次级数据先行检查失败, factoryCode={}, scheduleDate={}, errorCount={}, warningCount={}",
                    factoryCode, scheduleDate, result.getErrors().size(), result.getWarnings().size());
            for (Cd90BatchDataCheckResult.CheckError error : result.getErrors()) {
                log.warn("[直裁自动排程] 批次级检查错误, field={}, reasonCode={}, message={}, suggestion={}",
                        error.getField(), error.getReasonCode(), error.getMessage(), error.getSuggestion());
            }
            for (Cd90BatchDataCheckResult.CheckError warning : result.getWarnings()) {
                log.warn("[直裁自动排程] 批次级检查警告, field={}, reasonCode={}, message={}, suggestion={}",
                        warning.getField(), warning.getReasonCode(), warning.getMessage(), warning.getSuggestion());
            }
        } else {
            log.info("[直裁自动排程] 批次级数据先行检查通过, factoryCode={}, scheduleDate={}",
                    factoryCode, scheduleDate);
        }
        return result;
    }

    /**
     * 批次级检查：成型计划数据是否就绪。
     * 按文档1.5节，成型 scheduleDate 与直裁排程日相同（均为 T+1），
     * 成型一条记录的 CLASS1~CLASS8 已覆盖 T 日早班至 T+2 日共8个班次，
     * 因此入口先行检查只查 scheduleDate 当天的成型排程记录即可。
     * engine 运行时可能按停产场景额外读取相邻班次，但入口检查只关心用户该维护的核心记录是否存在。
     * 返回查询到的成型记录，供后续施工信息检查使用。
     */
    private List<CxScheduleResult> checkFormingSchedule(Cd90BatchDataCheckResult.Builder builder,
                                                         String factoryCode, LocalDate scheduleDate) {
        List<CxScheduleResult> schedules = cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .eq(CxScheduleResult::getScheduleDate, Date.valueOf(scheduleDate)));
        if (schedules == null || schedules.isEmpty()) {
            builder.addError("成型计划", "DATA_MISSING",
                    "未找到排程日 " + scheduleDate + " 的成型排程记录",
                    "请先在成型排程页面生成排程日 " + scheduleDate + " 的成型排程");
            return new ArrayList<>();
        }
        return schedules;
    }

    /**
     * 批次级检查：是否有启用的直裁机台。
     * 口径与 Cd90MachineResourceServiceImpl 对齐：factoryCode + status=启用。
     */
    private void checkMachineInfo(Cd90BatchDataCheckResult.Builder builder, String factoryCode) {
        Long count = machineInfoMapper.selectCount(Wrappers.<Cd90MachineInfo>lambdaQuery()
                .eq(Cd90MachineInfo::getFactoryCode, factoryCode)
                .eq(Cd90MachineInfo::getStatus, ApsConstant.APS_STRING_1));
        if (count == null || count == 0L) {
            builder.addError("机台档案", "DATA_MISSING",
                    "未找到启用的直裁机台",
                    "请在机台档案页面启用至少一台直裁机台");
        }
    }

    /**
     * 批次级检查：施工信息完整性。
     * 1. 收集成型计划中 (EMBRYO_CODE, CLASSn_RECIPE_NO) 配对；班次计划量为空或≤0时跳过该班次；
     * 2. 按 CONSTRUCTION_CODE + CONSTRUCTION_VERSION 查施工表，缺失则报错；
     * 3. 对每条施工记录校验 CORD_SPEC 及三层帘布层位字段。
     * 返回施工中出现过的帘布代号集合，供卷曲长度检查使用。
     */
    private Set<String> checkConstructionInfo(Cd90BatchDataCheckResult.Builder builder,
                                               String factoryCode,
                                               List<CxScheduleResult> formingSchedules) {
        if (formingSchedules == null || formingSchedules.isEmpty()) {
            return new LinkedHashSet<>();
        }
        // 1. 收集 (constructionCode, constructionVersion) 配对，同时校验胎胚代号和施工版本非空
        Set<String> embryoCodes = new LinkedHashSet<>();
        Set<String> constructionVersions = new LinkedHashSet<>();
        Set<String> constructionPairs = new LinkedHashSet<>();
        for (CxScheduleResult schedule : formingSchedules) {
            String embryoCode = schedule.getEmbryoCode();
            if (!StringUtils.hasText(embryoCode)) {
                builder.addError("成型计划", "DATA_MISSING",
                        "成型计划存在胎胚代号为空的记录",
                        "请检查成型排程数据的胎胚代号");
                continue;
            }
            embryoCodes.add(embryoCode);
            for (int classIndex = 1; classIndex <= CLASS_RECIPE_FIELDS.length; classIndex++) {
                // 班次计划量为空或≤0时，该班次不生产，跳过施工版本校验
                BigDecimal planQty = getClassPlanQty(schedule, classIndex);
                if (planQty == null || planQty.signum() <= 0) {
                    continue;
                }
                String classField = CLASS_RECIPE_FIELDS[classIndex - 1];
                String recipeNo = getRecipeNo(schedule, classField);
                if (!StringUtils.hasText(recipeNo)) {
                    builder.addError("成型计划", "DATA_MISSING",
                            "胎胚 " + embryoCode + " 的 " + classField + " 施工版本为空",
                            "请检查成型排程数据各班次施工版本");
                    continue;
                }
                constructionVersions.add(recipeNo);
                constructionPairs.add(embryoCode + "@" + recipeNo);
            }
        }
        if (embryoCodes.isEmpty() || constructionVersions.isEmpty()) {
            return new LinkedHashSet<>();
        }

        // 2. 查施工表
        List<MdmConstructionInfo> constructions = constructionMapper.selectList(
                Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions));
        Map<String, MdmConstructionInfo> constructionByKey = constructions.stream()
                .collect(Collectors.toMap(
                        item -> item.getConstructionCode() + "@" + item.getConstructionVersion(),
                        item -> item, (left, right) -> left, LinkedHashMap::new));

        // 3. 逐个配对校验
        Set<String> clothCodes = new LinkedHashSet<>();
        for (String pair : constructionPairs) {
            String[] parts = pair.split("@", 2);
            String constructionCode = parts[0];
            String constructionVersion = parts[1];
            MdmConstructionInfo construction = constructionByKey.get(pair);
            if (construction == null) {
                builder.addError("施工信息", "DATA_MISSING",
                        "胎胚 " + constructionCode + " 施工版本 " + constructionVersion + " 未维护",
                        "请在施工信息页面维护对应胎胚和版本的施工资料");
                continue;
            }
            checkConstructionFields(builder, construction, clothCodes);
        }
        return clothCodes;
    }

    /**
     * 校验单条施工记录的关键字段，并将帘布代号收集到 clothCodes。
     */
    private void checkConstructionFields(Cd90BatchDataCheckResult.Builder builder,
                                          MdmConstructionInfo construction,
                                          Set<String> clothCodes) {
        String code = construction.getConstructionCode();
        String version = construction.getConstructionVersion();
        String prefix = "胎胚 " + code + " 施工版本 " + version + " ";

        // CORD_SPEC 大卷代码
        if (!StringUtils.hasText(construction.getCordSpec())) {
            builder.addError("施工信息", "DATA_MISSING",
                    prefix + "大卷代码(CORD_SPEC)缺失",
                    "请在施工信息页面维护大卷代码");
        }
        // 三层帘布层位
        for (int layer = 1; layer <= CONSTRUCTION_LAYERS; layer++) {
            String clothCode = getLayerClothCode(construction, layer);
            if (!StringUtils.hasText(clothCode)) {
                continue; // 非必填层，未配置跳过
            }
            clothCodes.add(clothCode);
            String craftRaw = getLayerCraftRaw(construction, layer);
            BigDecimal craftWidth = parseDecimal(craftRaw);
            if (craftWidth == null || craftWidth.signum() <= 0) {
                builder.addError("施工信息", "DATA_MISSING",
                        prefix + "第 " + layer + " 层帘布 " + clothCode + " 直裁宽度缺失或非正",
                        "请在施工信息页面维护 TIRE_FABRIC_CRAFT" + layer + " 且大于0");
            }
            if (!isPositive(getLayerUnitConsume(construction, layer))) {
                builder.addError("施工信息", "DATA_MISSING",
                        prefix + "第 " + layer + " 层帘布 " + clothCode + " 单耗缺失或非正",
                        "请在施工信息页面维护 TIRE_FABRIC_LENGTH" + layer + " 且大于0");
            }
        }
    }

    /**
     * 批次级检查：卷曲长度配置。
     * 对施工中出现的每个帘布代号，要求 t_cd90_curl_length 有记录且 CURL_LENGTH > 0。
     * 若某帘布未维护标准卷曲长度，但参数 CRIMP_LENGTH (SYS0701011) 已配置有效正值，
     * 则降级为 warning（排程运行时会使用该参数兜底）；
     * 若兜底参数也未配置，则报 error 阻止排程。
     */
    private void checkCurlLength(Cd90BatchDataCheckResult.Builder builder,
                                  String factoryCode,
                                  Set<String> clothCodes) {
        if (clothCodes == null || clothCodes.isEmpty()) {
            return;
        }
        List<Cd90CurlLength> curls = curlLengthMapper.selectList(
                Wrappers.<Cd90CurlLength>lambdaQuery()
                        .eq(Cd90CurlLength::getFactoryCode, factoryCode)
                        .in(Cd90CurlLength::getClothCode, clothCodes));
        Map<String, Cd90CurlLength> curlByCloth = curls.stream()
                .collect(Collectors.toMap(Cd90CurlLength::getClothCode,
                        item -> item, (left, right) -> left, LinkedHashMap::new));

        // 查询 CRIMP_LENGTH 兜底参数是否已配置有效正值
        boolean hasFallback = false;
        try {
            Cd90Params fallbackParam = paramsMapper.selectOne(
                    Wrappers.<Cd90Params>lambdaQuery()
                            .eq(Cd90Params::getFactoryCode, factoryCode)
                            .eq(Cd90Params::getParamCode, Cd90AutoScheduleParamCode.CRIMP_LENGTH));
            if (fallbackParam != null) {
                BigDecimal fb = new BigDecimal(fallbackParam.getParamValue());
                hasFallback = fb != null && fb.signum() > 0;
            }
        } catch (RuntimeException ignored) {
            // 解析失败当作无可兜底
        }

        for (String clothCode : clothCodes) {
            Cd90CurlLength curl = curlByCloth.get(clothCode);
            if (curl == null) {
                if (hasFallback) {
                    builder.addWarning("卷曲长度", "FALLBACK_CRIMP_LENGTH",
                            "帘布 " + clothCode + " 卷曲长度未维护，将使用参数CRIMP_LENGTH兜底",
                            "建议在卷曲长度配置页面维护帘布 " + clothCode + " 的标准卷曲长度");
                } else {
                    builder.addError("卷曲长度", "DATA_MISSING",
                            "帘布 " + clothCode + " 卷曲长度未维护且兜底参数CRIMP_LENGTH也未配置",
                            "请在卷曲长度配置页面维护帘布 " + clothCode + " 的卷曲长度");
                }
                continue;
            }
            if (curl.getCurlLength() == null || curl.getCurlLength() <= 0) {
                if (hasFallback) {
                    builder.addWarning("卷曲长度", "FALLBACK_CRIMP_LENGTH",
                            "帘布 " + clothCode + " 卷曲长度非正，将使用参数CRIMP_LENGTH兜底",
                            "建议在卷曲长度配置页面维护帘布 " + clothCode + " 的标准卷曲长度且大于0");
                } else {
                    builder.addError("卷曲长度", "DATA_MISSING",
                            "帘布 " + clothCode + " 卷曲长度非正且兜底参数CRIMP_LENGTH也未配置",
                            "请在卷曲长度配置页面维护帘布 " + clothCode + " 的卷曲长度且大于0");
                }
            }
        }
    }

    /**
     * 批次级检查：任务启动时当前资源班次的库排数据完整性。
     * 1. MAX_CAR_NUM 必须维护且 >0(2026/06/24 变更,去掉 default 7,必填);
     * 2. CAR_NUM 不能为负,且不能大于 MAX_CAR_NUM;
     * 3. 空库排(MATERIAL_CODE 为空)时 CAR_NUM 必须 = 0。
     * 排程前拦截,避免 Allocator 运行时抛异常。
     */
    private void checkStorageLaneLimit(Cd90BatchDataCheckResult.Builder builder,
                                       String factoryCode) {
        List<Cd90ShiftConfig> configs = shiftConfigMapper.selectList(
                Wrappers.<Cd90ShiftConfig>lambdaQuery()
                        .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd90ShiftConfig::getIsActive, ACTIVE));
        Cd90ShiftDescriptor baselineShift;
        try {
            baselineShift = shiftWindowResolver.resolveCurrentResourceShift(
                    LocalDateTime.now(), configs);
        } catch (IllegalArgumentException exception) {
            builder.addError(I18nUtil.getMessage(
                            "ui.data.column.cd90StorageLaneLimit.modelName"),
                    "DATA_MISSING",
                    MessageFormat.format(I18nUtil.getMessage(
                                    "ui.cd90.autoSchedule.resourceBaselineResolveFailed"),
                            exception.getMessage()),
                    I18nUtil.getMessage(
                            "ui.cd90.autoSchedule.resourceBaselineResolveSuggestion"));
            return;
        }
        List<Cd90StorageLaneLimit> lanes = storageLaneMapper.selectList(
                Wrappers.<Cd90StorageLaneLimit>lambdaQuery()
                        .eq(Cd90StorageLaneLimit::getFactoryCode, factoryCode)
                        .eq(Cd90StorageLaneLimit::getLaneDate,
                                Date.valueOf(baselineShift.getScheduleDate()))
                        .eq(Cd90StorageLaneLimit::getShiftCode,
                                baselineShift.getShiftCode()));
        if (lanes == null || lanes.isEmpty()) {
            builder.addError(I18nUtil.getMessage(
                            "ui.data.column.cd90StorageLaneLimit.modelName"),
                    "DATA_MISSING",
                    MessageFormat.format(I18nUtil.getMessage(
                                    "ui.cd90.autoSchedule.resourceBaselineMissing"),
                            baselineShift.getScheduleDate(), baselineShift.getShiftCode()),
                    I18nUtil.getMessage(
                            "ui.cd90.autoSchedule.resourceBaselineSyncSuggestion"));
            return;
        }
        for (Cd90StorageLaneLimit lane : lanes) {
            String laneCode = lane.getStorageLaneCode();
            String shiftCode = lane.getShiftCode();
            String prefix = "库排 " + laneCode + "(班次 " + shiftCode + ") ";
            Integer maxCarNum = lane.getMaxCarNum();
            if (maxCarNum == null || maxCarNum <= 0) {
                builder.addError("库排限制", "MAX_CAR_NUM_MISSING",
                        prefix + "未维护有效最大车数",
                        "请在库排限制维护页面补充最大车数且大于0");
                continue;
            }
            Integer carNum = lane.getCarNum();
            if (carNum == null) {
                builder.addError("库排限制", "CAR_NUM_MISSING",
                        prefix + "当前车数为空(空库排请填0)",
                        "请在库排限制维护页面维护当前车数");
                continue;
            }
            if (carNum < 0) {
                builder.addError("库排限制", "CAR_NUM_INVALID",
                        prefix + "当前车数为负数",
                        "请在库排限制维护页面修正当前车数");
            }
            if (carNum > maxCarNum) {
                builder.addError("库排限制", "CAR_NUM_EXCEED",
                        prefix + "当前车数 " + carNum + " 大于最大车数 " + maxCarNum,
                        "请在库排限制维护页面修正当前车数或最大车数");
            }
            if (!StringUtils.hasText(lane.getMaterialCode()) && carNum != 0) {
                builder.addError("库排限制", "EMPTY_LANE_CAR_NUM_INVALID",
                        prefix + "胎体代码为空(空库排)但当前车数 " + carNum + " 不为0",
                        "空库排请将当前车数填0,或补充胎体代码");
            }
        }
    }

    /** 取成型记录指定班次的施工版本。 */
    private String getRecipeNo(CxScheduleResult schedule, String classField) {
        switch (classField) {
            case "CLASS1_RECIPE_NO": return schedule.getClass1RecipeNo();
            case "CLASS2_RECIPE_NO": return schedule.getClass2RecipeNo();
            case "CLASS3_RECIPE_NO": return schedule.getClass3RecipeNo();
            case "CLASS4_RECIPE_NO": return schedule.getClass4RecipeNo();
            case "CLASS5_RECIPE_NO": return schedule.getClass5RecipeNo();
            case "CLASS6_RECIPE_NO": return schedule.getClass6RecipeNo();
            case "CLASS7_RECIPE_NO": return schedule.getClass7RecipeNo();
            case "CLASS8_RECIPE_NO": return schedule.getClass8RecipeNo();
            default: return null;
        }
    }

    /** 取成型记录指定班次（1~8）的计划量。 */
    private BigDecimal getClassPlanQty(CxScheduleResult schedule, int classIndex) {
        switch (classIndex) {
            case 1: return schedule.getClass1PlanQty();
            case 2: return schedule.getClass2PlanQty();
            case 3: return schedule.getClass3PlanQty();
            case 4: return schedule.getClass4PlanQty();
            case 5: return schedule.getClass5PlanQty();
            case 6: return schedule.getClass6PlanQty();
            case 7: return schedule.getClass7PlanQty();
            case 8: return schedule.getClass8PlanQty();
            default: return null;
        }
    }

    /** 取施工记录指定层位的帘布代号。 */
    private String getLayerClothCode(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCode1();
            case 2: return construction.getTireFabricCode2();
            case 3: return construction.getTireFabricCode3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的直裁宽度原始值。 */
    private String getLayerCraftRaw(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricCraft1();
            case 2: return construction.getTireFabricCraft2();
            case 3: return construction.getTireFabricCraft3();
            default: return null;
        }
    }

    /** 取施工记录指定层位的单耗（毫米/条）。 */
    private BigDecimal getLayerUnitConsume(MdmConstructionInfo construction, int layer) {
        switch (layer) {
            case 1: return construction.getTireFabricLength1();
            case 2: return construction.getTireFabricLength2();
            case 3: return construction.getTireFabricLength3();
            default: return null;
        }
    }

    private BigDecimal parseDecimal(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
