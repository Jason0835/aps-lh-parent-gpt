package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
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

    private static final int CLASS_COUNT = 8;

    private final Cd15EngineCxScheduleMapper cxScheduleMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineStockMapper stockMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;
    private final Cd15EngineGdyyStockMapper gdyyStockMapper;
    private final Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper;

    @Override
    public Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode, int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");

        LocalDate formingStartDate = scheduleDate.minusDays(1);
        LocalDate formingEndDate = scheduleDate.plusDays(3);
        List<CxScheduleResult> formingSchedules = this.loadFormingSchedules(
                factoryCode, formingStartDate, formingEndDate);
        Set<String> embryoCodes = formingSchedules.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> constructionVersions = formingSchedules.stream()
                .flatMap(schedule -> IntStream.rangeClosed(1, CLASS_COUNT)
                        .mapToObj(classIndex -> this.readString(schedule, String.format("class%dRecipeNo", classIndex))))
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<Cd15ConstructionMaterial> constructionMaterials = this.loadConstructionMaterials(
                factoryCode, embryoCodes, constructionVersions);
        this.fillStandardCurlLength(factoryCode, constructionMaterials);

        List<Cd15Stock> stocksAtSix = stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                .eq(Cd15Stock::getFactoryCode, factoryCode)
                .eq(Cd15Stock::getStockDate, Date.valueOf(scheduleDate))
                .orderByAsc(Cd15Stock::getMaterialCode));
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
                        + "angleWidthCount={}, gdyyStockCount={}, gdyyPlanCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingSchedules.size(),
                constructionMaterials.size(), stocksAtSix.size(), machines.size(), angleWidthMappings.size(),
                gdyyStocks.size(), gdyyPlans.size());

        return Cd15AutoScheduleInput.builder()
                .formingSchedules(formingSchedules)
                .constructionMaterials(constructionMaterials)
                .stocksAtSix(stocksAtSix)
                .machines(machines)
                .curlLengths(curlLengths)
                .angleWidthMappings(angleWidthMappings)
                .angleWidthMaxByAngle(angleWidthMaxByAngle)
                .gdyyStocks(gdyyStocks)
                .gdyyPlans(gdyyPlans)
                .build();
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
        String bigRollCode = this.trim(construction.getCordSpec());
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