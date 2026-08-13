package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90LossSetting;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineMaintenancePlan;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.api.domain.entity.Cd90Params;
import com.zlt.aps.cd90.api.domain.entity.Cd90ScheduleResult;
import com.zlt.aps.cd90.api.domain.entity.Cd90ShiftConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.constant.Cd90AutoScheduleParamCode;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleParamsMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleShiftMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCurlLengthMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineDepthConfigMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineLossSettingMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineInfoMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMachineRollMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMaintenanceMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMonthSurplusMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineScheduleResultMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineSpecifyMachineMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStorageLaneMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyScheduleResultMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyStockMapper;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputVersionService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** 基于直裁自动排程实际读取的全部关键输入生成确定性版本指纹。 */
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleInputVersionServiceImpl
        implements Cd90AutoScheduleInputVersionService {

    private final Cd90EngineCxScheduleMapper cxMapper;
    private final Cd90EngineConstructionMapper constructionMapper;
    private final Cd90EngineScheduleResultMapper scheduleResultMapper;
    private final Cd90EngineStockMapper stockMapper;
    private final Cd90EngineCurlLengthMapper curlLengthMapper;
    private final Cd90EngineMachineInfoMapper machineInfoMapper;
    private final Cd90EngineMachineRollMapper machineRollMappingMapper;
    private final Cd90EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd90EngineMaintenanceMapper maintenanceMapper;
    private final Cd90AutoScheduleShiftMapper shiftConfigMapper;
    private final Cd90EngineDepthConfigMapper depthConfigMapper;
    private final Cd90EngineLossSettingMapper lossSettingMapper;
    private final Cd90EngineStorageLaneMapper laneMapper;
    private final Cd90AutoScheduleParamsMapper paramsMapper;
    private final Cd90EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd90EngineXwyyStockMapper xwyyStockMapper;
    private final Cd90EngineXwyyScheduleResultMapper xwyyScheduleResultMapper;

    @Override
    public String fingerprint(String factoryCode, LocalDate scheduleDate,
                              LocalDate resourceBaselineDate,
                              String resourceBaselineShiftCode) {
        return this.fingerprint(factoryCode, scheduleDate, resourceBaselineDate,
                resourceBaselineShiftCode, true);
    }

    @Override
    public String fingerprintWithoutStock(String factoryCode, LocalDate scheduleDate,
                                          LocalDate resourceBaselineDate,
                                          String resourceBaselineShiftCode) {
        return this.fingerprint(factoryCode, scheduleDate, resourceBaselineDate,
                resourceBaselineShiftCode, false);
    }

    private String fingerprint(String factoryCode, LocalDate scheduleDate,
                               LocalDate resourceBaselineDate,
                               String resourceBaselineShiftCode,
                               boolean includeStock) {
        List<CxScheduleResult> formingEntities = cxMapper.selectList(
                Wrappers.<CxScheduleResult>lambdaQuery()
                        .eq(CxScheduleResult::getFactoryCode, factoryCode)
                        .between(CxScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)),
                                Date.valueOf(scheduleDate.plusDays(3)))
                        .orderByAsc(CxScheduleResult::getId));
        String forming = formingEntities.stream()
                .map(this::formingFingerprint)
                .collect(Collectors.joining("|"));

        Set<String> embryoCodes = formingEntities.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> constructionVersions = formingEntities.stream()
                .flatMap(item -> this.constructionVersions(item).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<MdmConstructionInfo> constructionEntities = embryoCodes.isEmpty()
                || constructionVersions.isEmpty()
                ? Collections.emptyList()
                : constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions)
                        .orderByAsc(MdmConstructionInfo::getConstructionCode)
                        .orderByAsc(MdmConstructionInfo::getConstructionVersion)
                        .orderByAsc(MdmConstructionInfo::getId));
        String constructions = constructionEntities.stream()
                .map(this::constructionFingerprint)
                .collect(Collectors.joining("|"));

        Set<String> clothCodes = constructionEntities.stream()
                .flatMap(item -> Arrays.asList(item.getTireFabricCode1(),
                                item.getTireFabricCode2(), item.getTireFabricCode3()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<Cd90Params> parameterEntities = paramsMapper.selectList(
                Wrappers.<Cd90Params>lambdaQuery()
                        .eq(Cd90Params::getFactoryCode, factoryCode)
                        .orderByAsc(Cd90Params::getParamCode)
                        .orderByAsc(Cd90Params::getId));
        String parameters = parameterEntities.stream()
                .map(item -> this.row(item.getId(), item.getParamCode(), item.getParamValue(),
                        item.getRegularExpression(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        int newSpecLookbackDays = this.nonNegativeIntParameter(parameterEntities,
                Cd90AutoScheduleParamCode.NEW_SPEC_LOOKBACK_DAYS);
        String historySchedules = clothCodes.isEmpty() || newSpecLookbackDays <= 0 ? ""
                : scheduleResultMapper.selectList(
                        Wrappers.<Cd90ScheduleResult>lambdaQuery()
                                .eq(Cd90ScheduleResult::getFactoryCode, factoryCode)
                                .between(Cd90ScheduleResult::getScheduleDate,
                                        Date.valueOf(scheduleDate.minusDays(newSpecLookbackDays)),
                                        Date.valueOf(scheduleDate.minusDays(1)))
                                .in(Cd90ScheduleResult::getClothCode, clothCodes)
                                .orderByAsc(Cd90ScheduleResult::getScheduleDate)
                                .orderByAsc(Cd90ScheduleResult::getClothCode)
                                .orderByAsc(Cd90ScheduleResult::getId))
                        .stream()
                        .map(this::historyScheduleFingerprint)
                        .collect(Collectors.joining("|"));
        String curls = clothCodes.isEmpty() ? "" : curlLengthMapper.selectList(
                        Wrappers.<Cd90CurlLength>lambdaQuery()
                                .eq(Cd90CurlLength::getFactoryCode, factoryCode)
                                .in(Cd90CurlLength::getClothCode, clothCodes)
                                .orderByAsc(Cd90CurlLength::getClothCode)
                                .orderByAsc(Cd90CurlLength::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getClothCode(),
                        item.getCurlLength(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));

        String stock = includeStock ? stockMapper.selectList(
                        Wrappers.<Cd90Stock>lambdaQuery()
                                .eq(Cd90Stock::getFactoryCode, factoryCode)
                                .eq(Cd90Stock::getStockDate,
                                        Date.valueOf(resourceBaselineDate))
                                .eq(Cd90Stock::getShiftCode,
                                        resourceBaselineShiftCode)
                                .orderByAsc(Cd90Stock::getMaterialCode)
                                .orderByAsc(Cd90Stock::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getStockDate(), item.getShiftCode(),
                        item.getSnapshotTime(), item.getMaterialCode(), item.getStockNum(),
                        item.getModifyNum(), item.getBadNum(), item.getLayers(),
                        item.getDataSource(), item.getUpdateTime()))
                .collect(Collectors.joining("|")) : "";

        String machines = machineInfoMapper.selectList(
                        Wrappers.<Cd90MachineInfo>lambdaQuery()
                                .eq(Cd90MachineInfo::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90MachineInfo::getMachineCode)
                                .orderByAsc(Cd90MachineInfo::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getMachineCode(), item.getMachineName(),
                        item.getStatus(), item.getClothWidthMin(), item.getClothWidthMax(),
                        item.getQuota(), item.getClassShift(), item.getOpenMachineClass(),
                        item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String machineRolls = machineRollMappingMapper.selectList(
                        Wrappers.<Cd90MachineRollMapping>lambdaQuery()
                                .eq(Cd90MachineRollMapping::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90MachineRollMapping::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getBigRollCode(),
                        item.getCordFabricCode(), item.getMachineCode(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String specifyMachines = specifyMachineMapper.selectList(
                        Wrappers.<Cd90SpecifyMachine>lambdaQuery()
                                .eq(Cd90SpecifyMachine::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90SpecifyMachine::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getClothCode(), item.getMachineCode(),
                        item.getLineType(), item.getJobType(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String maintenances = maintenanceMapper.selectList(
                        Wrappers.<Cd90MachineMaintenancePlan>lambdaQuery()
                                .eq(Cd90MachineMaintenancePlan::getFactoryCode, factoryCode)
                                .between(Cd90MachineMaintenancePlan::getDowntimeDate,
                                        Date.valueOf(scheduleDate.minusDays(1)),
                                        Date.valueOf(scheduleDate.plusDays(2)))
                                .orderByAsc(Cd90MachineMaintenancePlan::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getMachineCode(), item.getDowntimeDate(),
                        item.getDowntimeStartTime(), item.getDowntimeEndTime(),
                        item.getDowntimeHours(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String shifts = shiftConfigMapper.selectList(
                        Wrappers.<Cd90ShiftConfig>lambdaQuery()
                                .eq(Cd90ShiftConfig::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90ShiftConfig::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getShiftCode(), item.getShiftName(),
                        item.getShiftOrder(), item.getStartTime(), item.getEndTime(),
                        item.getShiftHours(), item.getIsCrossDay(), item.getScheduleDay(),
                        item.getDayShiftOrder(), item.getClassField(), item.getIsActive(),
                        item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String depthConfigs = depthConfigMapper.selectList(
                        Wrappers.<Cd90DepthConfig>lambdaQuery()
                                .eq(Cd90DepthConfig::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90DepthConfig::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getMinMachineQty(),
                        item.getMaxMachineQty(), item.getDepthClassQty(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String lossSettings = lossSettingMapper.selectList(
                        Wrappers.<Cd90LossSetting>lambdaQuery()
                                .eq(Cd90LossSetting::getFactoryCode, factoryCode)
                                .orderByAsc(Cd90LossSetting::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getClothCode(), item.getMachineCode(),
                        item.getLossRate(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String lanes = laneMapper.selectList(
                        Wrappers.<Cd90StorageLaneLimit>lambdaQuery()
                                .eq(Cd90StorageLaneLimit::getFactoryCode, factoryCode)
                                .eq(Cd90StorageLaneLimit::getLaneDate,
                                        Date.valueOf(resourceBaselineDate))
                                .eq(Cd90StorageLaneLimit::getShiftCode,
                                        resourceBaselineShiftCode)
                                .orderByAsc(Cd90StorageLaneLimit::getStorageLaneCode)
                                .orderByAsc(Cd90StorageLaneLimit::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getLaneDate(), item.getShiftCode(),
                        item.getStorageLaneCode(), item.getMaterialCode(), item.getCarNum(),
                        item.getMaxCarNum(), item.getAvailableCarNum(), item.getDataSource(),
                        item.getMesSyncTime(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String monthSurpluses = embryoCodes.isEmpty() ? "" : monthSurplusMapper.selectList(
                        Wrappers.<MdmMonthSurplus>lambdaQuery()
                                .eq(MdmMonthSurplus::getFactoryCode, factoryCode)
                                .eq(MdmMonthSurplus::getYear, scheduleDate.getYear())
                                .eq(MdmMonthSurplus::getMonth, scheduleDate.getMonthValue())
                                .in(MdmMonthSurplus::getMaterialCode, embryoCodes)
                                .orderByAsc(MdmMonthSurplus::getMaterialCode)
                                .orderByAsc(MdmMonthSurplus::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getYear(), item.getMonth(),
                        item.getRequireVersion(), item.getMaterialCode(),
                        item.getPlanSurplusQty(), item.getStockCaptureDate(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String xwyyStock = xwyyStockMapper.selectList(Wrappers.<XwyyStock>lambdaQuery()
                        .eq(XwyyStock::getFactoryCode, factoryCode)
                        .orderByAsc(XwyyStock::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getStockDate(), item.getStockInTime(),
                        item.getBigRollCode(), item.getBigRollBarcode(), item.getStockNum(),
                        item.getStockRollNum(), item.getModifyNum(), item.getRollModifyNum(),
                        item.getBadNum(), item.getRollBadNum(), item.getStockMeters(),
                        item.getEstimateStockFlag(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String xwyyPlan = xwyyScheduleResultMapper.selectList(
                        Wrappers.<XwyyScheduleResult>lambdaQuery()
                                .eq(XwyyScheduleResult::getFactoryCode, factoryCode)
                                .between(XwyyScheduleResult::getScheduleDate,
                                        Date.valueOf(scheduleDate.minusDays(1)),
                                        Date.valueOf(scheduleDate.plusDays(2)))
                                .orderByAsc(XwyyScheduleResult::getId))
                .stream()
                .map(this::xwyyPlanFingerprint)
                .collect(Collectors.joining("|"));

        String requestScope = factoryCode + ":" + scheduleDate + ":"
                + resourceBaselineDate + ":" + resourceBaselineShiftCode;
        return this.sha256(String.join("#", forming, constructions, curls, stock, machines,
                machineRolls, specifyMachines, maintenances, shifts, depthConfigs,
                lossSettings, requestScope, lanes, parameters, historySchedules,
                monthSurpluses, xwyyStock, xwyyPlan));
    }

    private String formingFingerprint(CxScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(
                        item.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index)),
                        item.getFieldValueByFieldName(
                                String.format("class%dRecipeNo", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getCxBatchNo(), item.getScheduleDate(),
                item.getEmbryoCode(), item.getCxMachineCode(), classValues,
                item.getUpdateTime());
    }

    private List<String> constructionVersions(CxScheduleResult item) {
        return IntStream.rangeClosed(1, 8)
                .mapToObj(index -> (String) item.getFieldValueByFieldName(
                        String.format("class%dRecipeNo", index)))
                .collect(Collectors.toList());
    }

    private String constructionFingerprint(MdmConstructionInfo item) {
        return this.row(item.getId(), item.getConstructionCode(), item.getConstructionVersion(),
                item.getCordSpec(), item.getTireFabricCode1(), item.getTireFabricCraft1(),
                item.getTireFabricLength1(), item.getTireFabricCode2(),
                item.getTireFabricCraft2(), item.getTireFabricLength2(),
                item.getTireFabricCode3(), item.getTireFabricCraft3(),
                item.getTireFabricLength3(), item.getUpdateTime());
    }

    /** 新增规格判断实际读取的历史帘布计划量。 */
    private String historyScheduleFingerprint(Cd90ScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(item.getFieldValueByFieldName(
                        String.format("class%dPlanQty", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getScheduleDate(), item.getClothCode(),
                classValues, item.getUpdateTime());
    }

    /** 从已加载参数快照读取非负整数，口径与强类型参数解析结果保持一致。 */
    private int nonNegativeIntParameter(List<Cd90Params> parameters, String paramCode) {
        String value = null;
        for (Cd90Params parameter : parameters) {
            if (paramCode.equals(parameter.getParamCode())) {
                value = parameter.getParamValue();
            }
        }
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        int result = Integer.parseInt(value.trim());
        if (result < 0) {
            throw new IllegalArgumentException(paramCode + "不能为负整数");
        }
        return result;
    }

    private String xwyyPlanFingerprint(XwyyScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(
                        item.getFieldValueByFieldName(
                                String.format("class%dScheduleDate", index)),
                        item.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getBatchNo(), item.getScheduleDate(),
                item.getBigRollCode(), item.getMachineId(), classValues,
                item.getUpdateTime());
    }

    private String row(Object... values) {
        return Arrays.stream(values)
                .map(String::valueOf)
                .collect(Collectors.joining(":"));
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return IntStream.range(0, bytes.length)
                    .mapToObj(index -> String.format("%02x", bytes[index] & 0xff))
                    .collect(Collectors.joining());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }
}
