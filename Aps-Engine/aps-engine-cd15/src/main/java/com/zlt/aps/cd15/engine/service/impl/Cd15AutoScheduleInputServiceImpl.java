package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.engine.algorithm.Cd15ShiftWindowResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15SteelStripSourceTraceResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMaintenanceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMonthSurplusMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStorageLaneMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoPlanSurplus;
import com.zlt.aps.cd15.engine.model.Cd15ShiftDescriptor;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 斜裁自动排程输入数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleInputServiceImpl implements Cd15AutoScheduleInputService {

    private static final int ACTIVE = 1;

    private final Cd15EngineCxScheduleMapper cxScheduleMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineStockMapper stockMapper;
    private final Cd15EngineStorageLaneMapper storageLaneMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineMachineRollMappingMapper machineRollMappingMapper;
    private final Cd15EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd15EngineMaintenanceMapper maintenanceMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;
    private final Cd15EngineGdyyStockMapper gdyyStockMapper;
    private final Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper;
    private final Cd15EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd15EngineShiftConfigMapper shiftConfigMapper;
    private final Cd15ShiftWindowResolver shiftWindowResolver;
    private final Cd15SteelStripSourceTraceResolver steelStripSourceTraceResolver;

    @Override
    public Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate, int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        List<Cd15ShiftDescriptor> shifts = this.loadShifts(factoryCode, scheduleDate);
        return this.load(factoryCode, scheduleDate, shifts.get(0), shifts, agingPeriodHours);
    }

    @Override
    public Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode, int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");
        List<Cd15ShiftDescriptor> shifts = this.loadShifts(factoryCode, scheduleDate);
        Cd15ShiftDescriptor targetShift = shifts.stream()
                .filter(shift -> shift.getClassField().equalsIgnoreCase(classField.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到启用班次配置: " + classField));
        if (!targetShift.getShiftCode().equals(shiftCode.trim())) {
            throw new IllegalArgumentException("目标班次编码与当前配置不一致: " + classField);
        }
        return this.load(factoryCode, scheduleDate, targetShift, shifts, agingPeriodHours);
    }

    /** 加载除班次配置外的排程输入，并使用指定班次读取资源基线。 */
    private Cd15AutoScheduleInput load(String factoryCode,
                                       LocalDate scheduleDate,
                                       Cd15ShiftDescriptor resourceBaseline,
                                       List<Cd15ShiftDescriptor> shifts,
                                       int agingPeriodHours) {
        String classField = resourceBaseline.getClassField();
        String shiftCode = resourceBaseline.getShiftCode();
        LocalDate formingStartDate = scheduleDate.minusDays(1);
        LocalDate formingEndDate = scheduleDate.plusDays(3);
        List<CxScheduleResult> formingSchedules = this.loadFormingSchedules(
                factoryCode, formingStartDate, formingEndDate);
        Set<String> embryoCodes = formingSchedules.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> constructionVersions = formingSchedules.stream()
                .flatMap(schedule -> shifts.stream()
                        .map(Cd15ShiftDescriptor::getClassIndex)
                        .map(classIndex -> this.readString(schedule, String.format("class%dRecipeNo", classIndex))))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<Cd15ConstructionMaterial> constructionMaterials = this.loadConstructionMaterials(
                factoryCode, embryoCodes, constructionVersions);
        this.fillStandardCurlLength(factoryCode, constructionMaterials);

        List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses = this.loadEmbryoPlanSurpluses(
                factoryCode, scheduleDate, embryoCodes);
        List<Cd15Stock> stocksAtSix = stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                .eq(Cd15Stock::getFactoryCode, factoryCode)
                .eq(Cd15Stock::getStockDate, Date.valueOf(scheduleDate))
                .orderByAsc(Cd15Stock::getMaterialCode));
        List<Cd15StorageLaneLimit> storageLanesAtSix = storageLaneMapper.selectList(Wrappers.<Cd15StorageLaneLimit>lambdaQuery()
                .eq(Cd15StorageLaneLimit::getFactoryCode, factoryCode)
                .eq(Cd15StorageLaneLimit::getLaneDate, Date.valueOf(scheduleDate))
                .eq(Cd15StorageLaneLimit::getShiftCode, shiftCode)
                .orderByAsc(Cd15StorageLaneLimit::getStorageLaneCode));
        List<Cd15MachineInfo> machines = machineInfoMapper.selectList(Wrappers.<Cd15MachineInfo>lambdaQuery()
                .eq(Cd15MachineInfo::getFactoryCode, factoryCode)
                .eq(Cd15MachineInfo::getStatus, ApsConstant.APS_STRING_1)
                .orderByAsc(Cd15MachineInfo::getMachineCode));
        List<Cd15CurlLength> curlLengths = curlLengthMapper.selectList(Wrappers.<Cd15CurlLength>lambdaQuery()
                .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                .orderByAsc(Cd15CurlLength::getSteelStripCode));
        List<Cd15AngleWidthMapping> angleWidthMappings = angleWidthMappingMapper.selectList(
                Wrappers.<Cd15AngleWidthMapping>lambdaQuery()
                        .eq(Cd15AngleWidthMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15AngleWidthMapping::getCutAngle));
        Map<String, BigDecimal> angleWidthMaxByAngle = angleWidthMappings.stream()
                .filter(item -> item != null && StringUtils.hasText(item.getCutAngle()))
                .collect(Collectors.toMap(item -> item.getCutAngle().trim(),
                        Cd15AngleWidthMapping::getClothWidthMax, (first, second) -> first));
        List<Cd15MachineRollMapping> machineRollMappings = machineRollMappingMapper.selectList(
                Wrappers.<Cd15MachineRollMapping>lambdaQuery()
                        .eq(Cd15MachineRollMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15MachineRollMapping::getBigRollCode)
                        .orderByAsc(Cd15MachineRollMapping::getMachineCode));
        List<Cd15SpecifyMachine> specifyMachines = specifyMachineMapper.selectList(
                Wrappers.<Cd15SpecifyMachine>lambdaQuery()
                        .eq(Cd15SpecifyMachine::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15SpecifyMachine::getSteelStripCode)
                        .orderByAsc(Cd15SpecifyMachine::getMachineCode));
        List<Cd15MachineMaintenancePlan> maintenancePlans = maintenanceMapper.selectList(
                Wrappers.<Cd15MachineMaintenancePlan>lambdaQuery()
                        .eq(Cd15MachineMaintenancePlan::getFactoryCode, factoryCode)
                        .between(Cd15MachineMaintenancePlan::getDowntimeDate,
                                Date.valueOf(scheduleDate.minusDays(1)),
                                Date.valueOf(scheduleDate.plusDays(3)))
                        .orderByAsc(Cd15MachineMaintenancePlan::getDowntimeStartTime)
                        .orderByAsc(Cd15MachineMaintenancePlan::getMachineCode));
        List<GdyyStock> gdyyStocks = gdyyStockMapper.selectList(Wrappers.<GdyyStock>lambdaQuery()
                .eq(GdyyStock::getFactoryCode, factoryCode)
                .orderByAsc(GdyyStock::getInboundTime)
                .orderByAsc(GdyyStock::getBigRollCode)
                .orderByAsc(GdyyStock::getId));
        List<GdyyScheduleResult> gdyyPlans = gdyyScheduleResultMapper.selectList(
                Wrappers.<GdyyScheduleResult>lambdaQuery()
                        .eq(GdyyScheduleResult::getFactoryCode, factoryCode)
                        .between(GdyyScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)),
                                Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(GdyyScheduleResult::getScheduleDate)
                        .orderByAsc(GdyyScheduleResult::getId));

        log.info("[斜裁自动排程] 输入数据加载完成, factoryCode={}, scheduleDate={}, classField={}, shiftCode={}, "
                        + "formingCount={}, constructionMaterialCount={}, stockCount={}, machineCount={}, "
                        + "angleWidthCount={}, machineRollMappingCount={}, specifyMachineCount={}, "
                        + "maintenanceCount={}, storageLaneCount={}, gdyyStockCount={}, gdyyPlanCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingSchedules.size(),
                constructionMaterials.size(), stocksAtSix.size(), machines.size(), angleWidthMappings.size(),
                machineRollMappings.size(), specifyMachines.size(), maintenancePlans.size(), storageLanesAtSix.size(), gdyyStocks.size(),
                gdyyPlans.size());

        Cd15AutoScheduleInput input = Cd15AutoScheduleInput.builder()
                .scheduleDate(Date.valueOf(scheduleDate))
                .shifts(shifts)
                .formingSchedules(formingSchedules)
                .constructionMaterials(constructionMaterials)
                .stocksAtSix(stocksAtSix)
                .machines(machines)
                .embryoPlanSurpluses(embryoPlanSurpluses)
                .curlLengths(curlLengths)
                .angleWidthMappings(angleWidthMappings)
                .angleWidthMaxByAngle(angleWidthMaxByAngle)
                .machineRollMappings(machineRollMappings)
                .specifyMachines(specifyMachines)
                .maintenancePlans(maintenancePlans)
                .storageLanesAtSix(storageLanesAtSix)
                .gdyyStocks(gdyyStocks)
                .gdyyPlans(gdyyPlans)
                .agingPeriodHours(Math.max(0, agingPeriodHours))
                .build();
        Map<String, Cd15SteelStripSourceTrace> sourceTraceBySteelStrip =
                this.steelStripSourceTraceResolver.resolve(input, embryoPlanSurpluses);
        input.setSteelStripSourceTraceBySteelStrip(sourceTraceBySteelStrip);
        return input;
    }

    /** 加载并解析当前工厂的启用班次配置。 */
    private List<Cd15ShiftDescriptor> loadShifts(String factoryCode, LocalDate scheduleDate) {
        List<Cd15ShiftConfig> configs = this.shiftConfigMapper.selectList(
                Wrappers.<Cd15ShiftConfig>lambdaQuery()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd15ShiftConfig::getIsActive, ACTIVE)
                        .orderByAsc(Cd15ShiftConfig::getScheduleDay)
                        .orderByAsc(Cd15ShiftConfig::getDayShiftOrder)
                        .orderByAsc(Cd15ShiftConfig::getShiftOrder)
                        .orderByAsc(Cd15ShiftConfig::getClassField));
        List<Cd15ShiftDescriptor> shifts = this.shiftWindowResolver.resolve(scheduleDate, configs);
        if (shifts.isEmpty()) {
            throw new IllegalArgumentException("当前工厂未维护启用的CD15班次配置");
        }
        return shifts;
    }

    /**
     * 按胎胚代码加载当前排程月份的月计划剩余量。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param embryoCodes 胎胚代码集合
     * @return 胎胚月计划剩余量
     */
    private List<Cd15EmbryoPlanSurplus> loadEmbryoPlanSurpluses(
            String factoryCode, LocalDate scheduleDate, Set<String> embryoCodes) {
        if (embryoCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return this.monthSurplusMapper.selectList(Wrappers.<MdmMonthSurplus>lambdaQuery()
                        .select(MdmMonthSurplus::getMaterialCode,
                                MdmMonthSurplus::getPlanSurplusQty)
                        .eq(MdmMonthSurplus::getFactoryCode, factoryCode)
                        .eq(MdmMonthSurplus::getYear, scheduleDate.getYear())
                        .eq(MdmMonthSurplus::getMonth, scheduleDate.getMonthValue())
                        .in(MdmMonthSurplus::getMaterialCode, embryoCodes)
                        .orderByAsc(MdmMonthSurplus::getMaterialCode))
                .stream()
                .map(item -> Cd15EmbryoPlanSurplus.builder()
                        .embryoCode(item.getMaterialCode())
                        .planSurplusQuantity(item.getPlanSurplusQty())
                        .build())
                .collect(Collectors.toList());
    }

    private List<CxScheduleResult> loadFormingSchedules(String factoryCode,
                                                        LocalDate startDate,
                                                        LocalDate endDate) {
        return cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .between(CxScheduleResult::getScheduleDate,
                        Date.valueOf(startDate), Date.valueOf(endDate))
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getCxBatchNo));
    }

    private List<Cd15ConstructionMaterial> loadConstructionMaterials(String factoryCode,
                                                                     Set<String> embryoCodes,
                                                                     Set<String> constructionVersions) {
        if (embryoCodes.isEmpty() || constructionVersions.isEmpty()) {
            return Collections.emptyList();
        }
        return constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions)
                        .orderByAsc(MdmConstructionInfo::getConstructionCode)
                        .orderByAsc(MdmConstructionInfo::getConstructionVersion))
                .stream()
                .flatMap(this::mapConstructionMaterials)
                .collect(Collectors.toList());
    }

    private Stream<Cd15ConstructionMaterial> mapConstructionMaterials(MdmConstructionInfo construction) {
        String cuttingAngle = this.trim(construction.getBeltCuttingAngle());
        String bigRollCode = this.trim(construction.getArticleCrownSpec());
        Stream<Cd15ConstructionMaterial> mainLayers = IntStream.rangeClosed(1, 3)
                .mapToObj(layer -> this.mapMainLayer(construction, cuttingAngle, bigRollCode, layer));
        Stream<Cd15ConstructionMaterial> reinforcementLayers = Stream.of(
                this.mapReinforcement(construction, cuttingAngle, bigRollCode, 101,
                        "beltCodeLeftCode", "beltCodeLeftCraft", "beltCodeLeftLength"),
                this.mapReinforcement(construction, cuttingAngle, bigRollCode, 102,
                        "beltCodeRightCode", "beltCodeRightCraft", "beltCodeRightLength"));
        return Stream.concat(mainLayers, reinforcementLayers)
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode()));
    }

    private Cd15ConstructionMaterial mapMainLayer(MdmConstructionInfo construction,
                                                  String cuttingAngle,
                                                  String bigRollCode,
                                                  int layer) {
        String steelStripCode = this.readString(construction, "beltCode" + layer);
        if (!StringUtils.hasText(steelStripCode)) {
            return null;
        }
        return this.materialBuilder(construction, steelStripCode, cuttingAngle, bigRollCode, layer, false)
                .craftWidth(BigDecimalUtils.valueOf(this.readValue(construction, "beltCraft" + layer)))
                .unitConsumeMillimeter(BigDecimalUtils.valueOf(this.readValue(construction, "belt" + layer + "Length")))
                .build();
    }

    private Cd15ConstructionMaterial mapReinforcement(MdmConstructionInfo construction,
                                                      String cuttingAngle,
                                                      String bigRollCode,
                                                      int layerNo,
                                                      String codeProperty,
                                                      String craftProperty,
                                                      String lengthProperty) {
        String steelStripCode = this.readString(construction, codeProperty);
        if (!StringUtils.hasText(steelStripCode)) {
            return null;
        }
        return this.materialBuilder(construction, steelStripCode, cuttingAngle, bigRollCode, layerNo, true)
                .craftWidth(BigDecimalUtils.valueOf(this.readValue(construction, craftProperty)))
                .unitConsumeMillimeter(BigDecimalUtils.valueOf(this.readValue(construction, lengthProperty)))
                .build();
    }

    private Cd15ConstructionMaterial.Cd15ConstructionMaterialBuilder materialBuilder(
            MdmConstructionInfo construction, String steelStripCode, String cuttingAngle,
            String bigRollCode, int layerNo, boolean reinforcement) {
        return Cd15ConstructionMaterial.builder()
                .constructionCode(construction.getConstructionCode())
                .constructionVersion(construction.getConstructionVersion())
                .steelStripCode(steelStripCode.trim())
                .bigRollCode(bigRollCode)
                .cordWidth(BigDecimalUtils.valueOf(construction.getCordWidth()))
                .cuttingAngle(cuttingAngle)
                .layerNo(layerNo)
                .reinforcement(reinforcement);
    }

    private void fillStandardCurlLength(String factoryCode, List<Cd15ConstructionMaterial> materials) {
        Set<String> steelStripCodes = materials.stream()
                .map(Cd15ConstructionMaterial::getSteelStripCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (steelStripCodes.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> curlLengthBySteel = curlLengthMapper.selectList(
                        Wrappers.<Cd15CurlLength>lambdaQuery()
                                .select(Cd15CurlLength::getSteelStripCode, Cd15CurlLength::getCurlLength)
                                .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                                .in(Cd15CurlLength::getSteelStripCode, steelStripCodes)
                                .orderByAsc(Cd15CurlLength::getSteelStripCode))
                .stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode())
                        && item.getCurlLength() != null && item.getCurlLength() > 0D)
                .collect(Collectors.toMap(Cd15CurlLength::getSteelStripCode,
                        item -> BigDecimalUtils.valueOf(item.getCurlLength()),
                        (first, second) -> first));
        materials.forEach(item -> item.setCurlLength(curlLengthBySteel.get(item.getSteelStripCode())));
    }

    private String readString(Object source, String fieldName) {
        Object value = this.readValue(source, fieldName);
        return value == null ? null : value.toString().trim();
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

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
