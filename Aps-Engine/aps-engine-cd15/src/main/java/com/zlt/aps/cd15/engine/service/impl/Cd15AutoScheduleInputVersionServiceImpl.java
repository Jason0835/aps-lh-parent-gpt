package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15LossSetting;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15Params;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResult;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleParamsMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineDepthConfigMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineLossSettingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMaintenanceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMonthSurplusMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStorageLaneMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineWorkCalendarMapper;
import com.zlt.aps.cd15.engine.constant.Cd15AutoScheduleParamCode;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.mdm.api.domain.entity.MdmWorkCalendar;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于 CD15 自动排程关键输入生成版本指纹。
 */
@Service
public class Cd15AutoScheduleInputVersionServiceImpl implements Cd15AutoScheduleInputVersionService {

    private static final String CD15_PROCESS_CODE = "CD15";

    private final Cd15EngineCxScheduleMapper cxMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineScheduleResultMapper scheduleResultMapper;
    private final Cd15EngineStockMapper stockMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;
    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineMachineRollMapper machineRollMappingMapper;
    private final Cd15EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd15EngineMaintenanceMapper maintenanceMapper;
    private final Cd15EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd15EngineWorkCalendarMapper workCalendarMapper;
    private final Cd15EngineGdyyStockMapper gdyyStockMapper;
    private final Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper;
    private final Cd15EngineShiftConfigMapper shiftConfigMapper;
    private final Cd15EngineDepthConfigMapper depthConfigMapper;
    private final Cd15EngineLossSettingMapper lossSettingMapper;
    private final Cd15EngineStorageLaneMapper laneMapper;
    private final Cd15AutoScheduleParamsMapper paramsMapper;

    public Cd15AutoScheduleInputVersionServiceImpl(Cd15EngineCxScheduleMapper cxMapper,
                                                   Cd15EngineConstructionMapper constructionMapper,
                                                   Cd15EngineScheduleResultMapper scheduleResultMapper,
                                                   Cd15EngineStockMapper stockMapper,
                                                   Cd15EngineCurlLengthMapper curlLengthMapper,
                                                   Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper,
                                                   Cd15EngineMachineInfoMapper machineInfoMapper,
                                                   Cd15EngineMachineRollMapper machineRollMappingMapper,
                                                   Cd15EngineSpecifyMachineMapper specifyMachineMapper,
                                                   Cd15EngineMaintenanceMapper maintenanceMapper,
                                                   Cd15EngineMonthSurplusMapper monthSurplusMapper,
                                                   Cd15EngineWorkCalendarMapper workCalendarMapper,
                                                   Cd15EngineGdyyStockMapper gdyyStockMapper,
                                                   Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper,
                                                   Cd15EngineShiftConfigMapper shiftConfigMapper,
                                                   Cd15EngineDepthConfigMapper depthConfigMapper,
                                                   Cd15EngineLossSettingMapper lossSettingMapper,
                                                   Cd15EngineStorageLaneMapper laneMapper,
                                                   Cd15AutoScheduleParamsMapper paramsMapper) {
        this.cxMapper = cxMapper;
        this.constructionMapper = constructionMapper;
        this.scheduleResultMapper = scheduleResultMapper;
        this.stockMapper = stockMapper;
        this.curlLengthMapper = curlLengthMapper;
        this.angleWidthMappingMapper = angleWidthMappingMapper;
        this.machineInfoMapper = machineInfoMapper;
        this.machineRollMappingMapper = machineRollMappingMapper;
        this.specifyMachineMapper = specifyMachineMapper;
        this.maintenanceMapper = maintenanceMapper;
        this.monthSurplusMapper = monthSurplusMapper;
        this.workCalendarMapper = workCalendarMapper;
        this.gdyyStockMapper = gdyyStockMapper;
        this.gdyyScheduleResultMapper = gdyyScheduleResultMapper;
        this.shiftConfigMapper = shiftConfigMapper;
        this.depthConfigMapper = depthConfigMapper;
        this.lossSettingMapper = lossSettingMapper;
        this.laneMapper = laneMapper;
        this.paramsMapper = paramsMapper;
    }

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
                        .eq(CxScheduleResult::getScheduleDate, Date.valueOf(scheduleDate))
                        .orderByAsc(CxScheduleResult::getId));
        String forming = formingEntities.stream()
                .map(this::formingFingerprint)
                .collect(Collectors.joining("|"));
        List<MdmConstructionInfo> constructionEntities = constructionMapper.selectList(
                Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .orderByAsc(MdmConstructionInfo::getId));
        String constructions = constructionEntities.stream()
                .map(this::constructionFingerprint)
                .collect(Collectors.joining("|"));
        String stock = includeStock ? stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                        .eq(Cd15Stock::getFactoryCode, factoryCode)
                        .eq(Cd15Stock::getStockDate, Date.valueOf(resourceBaselineDate))
                        .eq(Cd15Stock::getShiftCode, resourceBaselineShiftCode)
                        .orderByAsc(Cd15Stock::getMaterialCode)
                        .orderByAsc(Cd15Stock::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMaterialCode() + ":" + item.getStockDate()
                        + ":" + item.getShiftCode()
                        + ":" + item.getStockNum() + ":" + item.getModifyNum() + ":" + item.getBadNum()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|")) : "";
        String curls = curlLengthMapper.selectList(Wrappers.<Cd15CurlLength>lambdaQuery()
                        .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15CurlLength::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getSteelStripCode() + ":" + item.getCurlLength()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String angleWidths = angleWidthMappingMapper.selectList(Wrappers.<Cd15AngleWidthMapping>lambdaQuery()
                        .eq(Cd15AngleWidthMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15AngleWidthMapping::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getCutAngle() + ":" + item.getClothWidthMax()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String machines = machineInfoMapper.selectList(Wrappers.<Cd15MachineInfo>lambdaQuery()
                        .eq(Cd15MachineInfo::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15MachineInfo::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getMachineCode(),
                        item.getStatus(), item.getClothWidthMin(),
                        item.getClothWidthMax(), item.getOpenMachineClass(),
                        item.getIsOutTwo(),
                        item.getSingleCutFlag(), item.getSplitCutFlag(),
                        item.getDefaultCutMode(),
                        item.getSingleShiftCapacity(), item.getSplitShiftCapacity(),
                        item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String machineRolls = machineRollMappingMapper.selectList(Wrappers.<Cd15MachineRollMapping>lambdaQuery()
                        .eq(Cd15MachineRollMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15MachineRollMapping::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getBigRollCode() + ":" + item.getMachineCode()
                        + ":" + item.getShiftCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String specifyMachines = specifyMachineMapper.selectList(Wrappers.<Cd15SpecifyMachine>lambdaQuery()
                        .eq(Cd15SpecifyMachine::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15SpecifyMachine::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getSteelStripCode() + ":" + item.getMachineCode()
                        + ":" + item.getJobType() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String maintenances = maintenanceMapper.selectList(Wrappers.<Cd15MachineMaintenancePlan>lambdaQuery()
                        .eq(Cd15MachineMaintenancePlan::getFactoryCode, factoryCode)
                        .between(Cd15MachineMaintenancePlan::getDowntimeDate,
                                Date.valueOf(scheduleDate.minusDays(1)), Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(Cd15MachineMaintenancePlan::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMachineCode() + ":" + item.getDowntimeDate()
                        + ":" + item.getDowntimeStartTime() + ":" + item.getDowntimeEndTime()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        List<Cd15ShiftConfig> shiftConfigs = shiftConfigMapper.selectList(
                Wrappers.<Cd15ShiftConfig>lambdaQuery()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15ShiftConfig::getId));

        String shifts = shiftConfigs.stream()
                .map(item -> item.getId() + ":" + item.getShiftCode() + ":" + item.getShiftName()
                        + ":" + item.getShiftOrder() + ":" + item.getStartTime() + ":" + item.getEndTime()
                        + ":" + item.getShiftHours() + ":" + item.getIsCrossDay()
                        + ":" + item.getScheduleDay() + ":" + item.getDayShiftOrder()
                        + ":" + item.getClassField() + ":" + item.getIsActive() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String depthConfigs = depthConfigMapper.selectList(Wrappers.<Cd15DepthConfig>lambdaQuery()
                        .eq(Cd15DepthConfig::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15DepthConfig::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMinMachineQty() + ":" + item.getMaxMachineQty()
                        + ":" + item.getDepthClassQty() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String lossSettings = lossSettingMapper.selectList(Wrappers.<Cd15LossSetting>lambdaQuery()
                        .eq(Cd15LossSetting::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15LossSetting::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getSteelStripCode() + ":" + item.getMachineCode()
                        + ":" + item.getLossRate() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String lanes = laneMapper.selectList(Wrappers.<Cd15StorageLaneLimit>lambdaQuery()
                        .eq(Cd15StorageLaneLimit::getFactoryCode, factoryCode)
                        .eq(Cd15StorageLaneLimit::getLaneDate,
                                Date.valueOf(resourceBaselineDate))
                        .eq(Cd15StorageLaneLimit::getShiftCode,
                                resourceBaselineShiftCode)
                        .orderByAsc(Cd15StorageLaneLimit::getStorageLaneCode))
                .stream()
                .map(item -> this.row(item.getFactoryCode(), item.getLaneDate(), item.getMaterialCode(),
                        item.getShiftCode(), item.getMachineCode(), item.getStorageLaneCode(), item.getCarNum(),
                        item.getMaxCarNum(), item.getAvailableCarNum(), item.getDataSource(),
                        item.getMesSyncTime()))
                .collect(Collectors.joining("|"));
        List<Cd15Params> parameterEntities = paramsMapper.selectList(
                Wrappers.<Cd15Params>lambdaQuery()
                        .eq(Cd15Params::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15Params::getParamCode)
                        .orderByAsc(Cd15Params::getId));
        String parameters = parameterEntities.stream()
                .map(item -> this.row(item.getId(), item.getParamCode(), item.getParamValue(),
                        item.getRegularExpression(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        Set<String> embryoCodes = formingEntities.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        String monthSurpluses = embryoCodes.isEmpty() ? ""
                : monthSurplusMapper.selectList(Wrappers.<MdmMonthSurplus>lambdaQuery()
                                .eq(MdmMonthSurplus::getFactoryCode, factoryCode)
                                .eq(MdmMonthSurplus::getYear, scheduleDate.getYear())
                                .eq(MdmMonthSurplus::getMonth, scheduleDate.getMonthValue())
                                .in(MdmMonthSurplus::getMaterialCode, embryoCodes)
                                .orderByAsc(MdmMonthSurplus::getMaterialCode)
                                .orderByAsc(MdmMonthSurplus::getId))
                        .stream()
                        .map(item -> this.row(item.getId(), item.getYear(), item.getMonth(),
                                item.getRequireVersion(), item.getMaterialCode(),
                                item.getPlanSurplusQty(), item.getStockCaptureDate(),
                                item.getUpdateTime()))
                        .collect(Collectors.joining("|"));
        Set<String> steelStripCodes = constructionEntities.stream()
                .flatMap(item -> Arrays.asList(item.getBeltCode1(), item.getBeltCode2(),
                                item.getBeltCode3(), item.getBeltCodeLeftCode(),
                                item.getBeltCodeRightCode()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        int lookbackDays = this.nonNegativeIntParameter(parameterEntities,
                Cd15AutoScheduleParamCode.NEW_SPEC_LOOKBACK_DAYS);
        String historySchedules = steelStripCodes.isEmpty() || lookbackDays <= 0 ? ""
                : scheduleResultMapper.selectList(
                                Wrappers.<Cd15ScheduleResult>lambdaQuery()
                                        .eq(Cd15ScheduleResult::getFactoryCode, factoryCode)
                                        .between(Cd15ScheduleResult::getScheduleDate,
                                                Date.valueOf(scheduleDate.minusDays(lookbackDays)),
                                                Date.valueOf(scheduleDate.minusDays(1)))
                                        .in(Cd15ScheduleResult::getSteelStripCode, steelStripCodes)
                                        .orderByAsc(Cd15ScheduleResult::getScheduleDate)
                                        .orderByAsc(Cd15ScheduleResult::getSteelStripCode)
                                        .orderByAsc(Cd15ScheduleResult::getId))
                        .stream()
                        .map(this::historyScheduleFingerprint)
                        .collect(Collectors.joining("|"));
        String workCalendars = workCalendarMapper.selectList(
                        Wrappers.<MdmWorkCalendar>lambdaQuery()
                                .eq(MdmWorkCalendar::getFactoryCode, factoryCode)
                                .eq(MdmWorkCalendar::getProcCode, CD15_PROCESS_CODE)
                                .between(MdmWorkCalendar::getProductionDate,
                                        Date.valueOf(scheduleDate.minusDays(1)),
                                        Date.valueOf(scheduleDate.plusDays(3)))
                                .orderByAsc(MdmWorkCalendar::getProductionDate)
                                .orderByAsc(MdmWorkCalendar::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getProductionDate(),
                        item.getOneShiftFlag(), item.getTwoShiftFlag(),
                        item.getThreeShiftFlag(), item.getDayFlag(), item.getRate(),
                        item.getCalendarTime(), item.getHolidayNames(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String gdyyStock = gdyyStockMapper.selectList(Wrappers.<GdyyStock>lambdaQuery()
                        .eq(GdyyStock::getFactoryCode, factoryCode)
                        .orderByAsc(GdyyStock::getId))
                .stream()
                .map(item -> this.row(item.getId(), item.getStockDate(), item.getInboundTime(),
                        item.getBigRollCode(), item.getBigRollBarcode(), item.getStockNum(),
                        item.getStockRollNum(), item.getModifyNum(), item.getBadNum(),
                        item.getStockMeters(),
                        item.getEstimateStockFlag(), item.getUpdateTime()))
                .collect(Collectors.joining("|"));
        String gdyyPlan = gdyyScheduleResultMapper.selectList(Wrappers.<GdyyScheduleResult>lambdaQuery()
                        .eq(GdyyScheduleResult::getFactoryCode, factoryCode)
                        .between(GdyyScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)), Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(GdyyScheduleResult::getId))
                .stream()
                .map(this::gdyyPlanFingerprint)
                .collect(Collectors.joining("|"));
        String resourceBaseline = resourceBaselineDate + ":" + resourceBaselineShiftCode;
        return this.sha256(String.join("#", forming, constructions, stock, curls, angleWidths, machines,
                machineRolls, specifyMachines, maintenances, shifts, depthConfigs, lossSettings,
                resourceBaseline, lanes, parameters, monthSurpluses, historySchedules,
                workCalendars, gdyyStock, gdyyPlan));
    }

    /** 成型摘要纳入实际参与需求展开的排程日期、计划量和示方书版本。 */
    private String formingFingerprint(CxScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(
                        item.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index)),
                        item.getFieldValueByFieldName(
                                String.format("class%dRecipeNo", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getCxBatchNo(), item.getScheduleDate(),
                item.getCxMachineCode(), item.getEmbryoCode(), classValues,
                item.getUpdateTime());
    }

    /**
     * 施工版本指纹必须包含完整CD15材料身份，避免钢带、角度或工艺尺寸变化后提交旧结果。
     */
    private String constructionFingerprint(MdmConstructionInfo item) {
        return this.row(item.getId(), item.getConstructionCode(), item.getConstructionVersion(),
                item.getArticleCrownSpec(), item.getCordWidth(), item.getBeltCuttingAngle(),
                item.getBeltCode1(), item.getBeltCraft1(), item.getBelt1Length(),
                item.getBeltCode2(), item.getBeltCraft2(), item.getBelt2Length(),
                item.getBeltCode3(), item.getBeltCraft3(), item.getBelt3Length(),
                item.getBeltCodeLeftCode(), item.getBeltCodeLeftCraft(),
                item.getBeltCodeLeftLength(), item.getBeltCodeRightCode(),
                item.getBeltCodeRightCraft(), item.getBeltCodeRightLength(),
                item.getUpdateTime());
    }

    /** 新增规格判断实际读取的历史斜裁计划量。 */
    private String historyScheduleFingerprint(Cd15ScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(item.getFieldValueByFieldName(
                        String.format("class%dPlanQty", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getScheduleDate(),
                item.getSteelStripCode(), classValues, item.getUpdateTime());
    }

    /** GDYY计划摘要纳入各班计划日期和计划量。 */
    private String gdyyPlanFingerprint(GdyyScheduleResult item) {
        String classValues = IntStream.rangeClosed(1, 8)
                .mapToObj(index -> this.row(
                        item.getFieldValueByFieldName(
                                String.format("class%dScheduleDate", index)),
                        item.getFieldValueByFieldName(
                                String.format("class%dPlanQty", index))))
                .collect(Collectors.joining(":"));
        return this.row(item.getId(), item.getBatchNo(), item.getScheduleDate(),
                item.getBigRollCode(), item.getMachineCode(), classValues,
                item.getUpdateTime());
    }

    /** 从参数快照读取非负整数，口径与强类型参数解析保持一致。 */
    private int nonNegativeIntParameter(List<Cd15Params> parameters,
                                        String paramCode) {
        String value = null;
        for (Cd15Params parameter : parameters) {
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
