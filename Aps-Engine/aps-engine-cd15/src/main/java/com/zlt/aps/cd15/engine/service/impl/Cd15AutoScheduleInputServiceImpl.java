package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollAgingStockBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15SteelStripDepthResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15SteelStripSourceTraceResolver;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleSourceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15ConstructionMaterialMapper;
import com.zlt.aps.cd15.engine.algorithm.Cd15FormingDemandExpander;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineDepthConfigMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStorageLaneMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMonthSurplusMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyStockMapper;
import com.zlt.aps.cd15.engine.model.Cd15AutoScheduleInput;
import com.zlt.aps.cd15.engine.model.Cd15BigRollAgingBuildResult;
import com.zlt.aps.cd15.engine.model.Cd15SteelStripSourceTrace;
import com.zlt.aps.cd15.engine.model.Cd15ConstructionMaterial;
import com.zlt.aps.cd15.engine.model.Cd15DemandShift;
import com.zlt.aps.cd15.engine.model.Cd15FormingScheduleSource;
import com.zlt.aps.cd15.engine.model.Cd15EmbryoPlanSurplus;
import com.zlt.aps.cd15.engine.model.Cd15StockSource;
import com.zlt.aps.cd15.engine.model.Cd15StorageLaneState;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 斜裁自动排程输入数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleInputServiceImpl implements Cd15AutoScheduleInputService {

    private final Cd15EngineCxScheduleMapper cxScheduleMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineStockMapper stockMapper;
    private final Cd15EngineStorageLaneMapper storageLaneMapper;
    private final Cd15EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineDepthConfigMapper depthConfigMapper;
    private final Cd15ConstructionMaterialMapper constructionMaterialMapper;
    private final Cd15EngineGdyyStockMapper gdyyStockMapper;
    private final Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper;
    private final Cd15BigRollAgingStockBuilder bigRollAgingStockBuilder;
    private final Cd15AutoScheduleSourceMapper sourceMapper;
    private final Cd15FormingDemandExpander formingDemandExpander;
    private final Cd15SteelStripDepthResolver steelStripDepthResolver;
    private final Cd15SteelStripSourceTraceResolver steelStripSourceTraceResolver;

    /**
     * 加载第1至5步所需的成型计划、施工、6点库存和当前班次库排快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 斜裁结果班次字段
     * @param shiftCode 业务班次编码，用于匹配库排班次
     * @return 标准化输入数据
     */
    @Override
    public Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode, int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");

        LocalDate formingStartDate = scheduleDate.minusDays(1);
        LocalDate formingEndDate = scheduleDate.plusDays(3);
        log.info("[斜裁自动排程] 加载成型计划, factoryCode={}, scheduleDate={}, formingStartDate={}, formingEndDate={}",
                factoryCode, scheduleDate, formingStartDate, formingEndDate);
        List<CxScheduleResult> formingEntities = loadFormingSchedules(
                factoryCode, formingStartDate, formingEndDate);
        log.info("[斜裁自动排程] 成型计划加载结果, factoryCode={}, formingStartDate={}, formingEndDate={}, recordCount={}",
                factoryCode, formingStartDate, formingEndDate, formingEntities.size());
        List<Cd15FormingScheduleSource> formingSchedules = formingEntities.stream()
                .map(sourceMapper::mapFormingSchedule)
                .collect(Collectors.toList());

        Set<String> embryoCodes = formingEntities.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> constructionVersions = formingSchedules.stream()
                .flatMap(schedule -> safe(schedule.getClassRecipeNos()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<Cd15ConstructionMaterial> constructionMaterials = loadConstructionMaterials(
                factoryCode, embryoCodes, constructionVersions);
        fillStandardCurlLength(factoryCode, constructionMaterials);
        List<Cd15DepthConfig> depthConfigs = depthConfigMapper.selectList(
                Wrappers.<Cd15DepthConfig>lambdaQuery()
                        .eq(Cd15DepthConfig::getFactoryCode, factoryCode)
                        .orderByDesc(Cd15DepthConfig::getMachineQty)
                        .orderByAsc(Cd15DepthConfig::getMachineRange));
        Map<String, BigDecimal> depthClassQtyBySteelStrip = steelStripDepthResolver.resolve(
                formingSchedules, constructionMaterials, depthConfigs);
        List<Cd15DemandShift> demandShifts = formingDemandExpander.expand(
                formingSchedules, constructionMaterials);
        List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses = loadEmbryoPlanSurpluses(
                factoryCode, scheduleDate, embryoCodes);
        Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip =
                steelStripSourceTraceResolver.resolve(
                        formingSchedules, constructionMaterials, embryoPlanSurpluses);

        List<Cd15StockSource> stocksAtSix = stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                        .eq(Cd15Stock::getFactoryCode, factoryCode)
                        .eq(Cd15Stock::getStockDate, Date.valueOf(scheduleDate))
                        .orderByAsc(Cd15Stock::getMaterialCode))
                .stream()
                .map(sourceMapper::mapStock)
                .collect(Collectors.toList());

        List<Cd15StorageLaneState> storageLanesAtSix = storageLaneMapper.selectList(
                        Wrappers.<Cd15StorageLaneLimit>lambdaQuery()
                                .eq(Cd15StorageLaneLimit::getFactoryCode, factoryCode)
                                .eq(Cd15StorageLaneLimit::getLaneDate, Date.valueOf(scheduleDate))
                                .eq(Cd15StorageLaneLimit::getShiftCode, shiftCode)
                                .orderByAsc(Cd15StorageLaneLimit::getStorageLaneCode))
                .stream()
                .map(sourceMapper::mapStorageLane)
                .collect(Collectors.toList());

        List<GdyyStock> gdyyActualStocks = gdyyStockMapper.selectList(
                Wrappers.<GdyyStock>lambdaQuery()
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
        Cd15BigRollAgingBuildResult agingResult = bigRollAgingStockBuilder.build(
                gdyyActualStocks, gdyyPlans, agingPeriodHours);

        log.info("[斜裁自动排程] 输入数据加载完成, factoryCode={}, scheduleDate={}, classField={}, shiftCode={}, "
                        + "formingRange={}~{}, formingCount={}, constructionMaterialCount={}, "
                        + "demandShiftCount={}, depthSteelStripCount={}, resourceBaselineShiftCode={}, "
                        + "stockCount={}, storageLaneCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingStartDate, formingEndDate,
                formingSchedules.size(), constructionMaterials.size(), demandShifts.size(),
                depthClassQtyBySteelStrip.size(), shiftCode, stocksAtSix.size(),
                storageLanesAtSix.size());

        return Cd15AutoScheduleInput.builder()
                .formingSchedules(formingSchedules)
                .constructionMaterials(constructionMaterials)
                .stocksAtSix(stocksAtSix)
                .embryoPlanSurpluses(embryoPlanSurpluses)
                .demandShifts(demandShifts)
                .steelStripSourceTraceBySteelStrip(steelStripSourceTraceBySteelStrip)
                .depthClassQtyBySteelStrip(depthClassQtyBySteelStrip)
                .storageLanesAtSix(storageLanesAtSix)
                .bigRollAgingStocks(agingResult.getStocks())
                .bigRollAgingDataMissingCodes(agingResult.getDataMissingBigRollCodes())
                .inboundRecords(Collections.emptyList())
                .build();
    }

    /** 按胎胚代码加载当前排程月份的月计划剩余量。 */
    private List<Cd15EmbryoPlanSurplus> loadEmbryoPlanSurpluses(String factoryCode,
                                                                LocalDate scheduleDate,
                                                                Set<String> embryoCodes) {
        if (embryoCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return monthSurplusMapper.selectList(Wrappers.<MdmMonthSurplus>lambdaQuery()
                        .select(MdmMonthSurplus::getMaterialCode, MdmMonthSurplus::getPlanSurplusQty)
                        .eq(MdmMonthSurplus::getFactoryCode, factoryCode)
                        .eq(MdmMonthSurplus::getYear, scheduleDate.getYear())
                        .eq(MdmMonthSurplus::getMonth, scheduleDate.getMonthValue())
                        .in(MdmMonthSurplus::getMaterialCode, embryoCodes)
                        .orderByAsc(MdmMonthSurplus::getMaterialCode))
                .stream().map(item -> Cd15EmbryoPlanSurplus.builder()
                        .embryoCode(item.getMaterialCode())
                        .planSurplusQuantity(item.getPlanSurplusQty()).build())
                .collect(Collectors.toList());
    }

    private List<CxScheduleResult> loadFormingSchedules(String factoryCode,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        // 斜裁按胎胚代码分解施工层位，仅查询需求计算所需字段，避免共享实体的展示字段影响排程。
        return cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .select(CxScheduleResult::getCxBatchNo,
                        CxScheduleResult::getScheduleDate,
                        CxScheduleResult::getEmbryoCode,
                        CxScheduleResult::getCxMachineCode,
                        CxScheduleResult::getClass1PlanQty,
                        CxScheduleResult::getClass1RecipeNo,
                        CxScheduleResult::getClass2PlanQty,
                        CxScheduleResult::getClass2RecipeNo,
                        CxScheduleResult::getClass3PlanQty,
                        CxScheduleResult::getClass3RecipeNo,
                        CxScheduleResult::getClass4PlanQty,
                        CxScheduleResult::getClass4RecipeNo,
                        CxScheduleResult::getClass5PlanQty,
                        CxScheduleResult::getClass5RecipeNo,
                        CxScheduleResult::getClass6PlanQty,
                        CxScheduleResult::getClass6RecipeNo,
                        CxScheduleResult::getClass7PlanQty,
                        CxScheduleResult::getClass7RecipeNo,
                        CxScheduleResult::getClass8PlanQty,
                        CxScheduleResult::getClass8RecipeNo)
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .between(CxScheduleResult::getScheduleDate,
                        Date.valueOf(startDate), Date.valueOf(endDate))
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getCxBatchNo));
    }

    private List<Cd15ConstructionMaterial> loadConstructionMaterials(String factoryCode,
                                                                      Set<String> embryoCodes,
                                                                      Set<String> constructionVersions) {
        if (embryoCodes.isEmpty()) {
            log.warn("[斜裁自动排程] 成型计划未找到胎胚代码, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        if (constructionVersions.isEmpty()) {
            log.warn("[斜裁自动排程] 成型计划未找到施工版本CLASSn_RECIPE_NO, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        return constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions)
                        .orderByAsc(MdmConstructionInfo::getConstructionCode)
                        .orderByAsc(MdmConstructionInfo::getConstructionVersion))
                .stream()
                .flatMap(construction -> constructionMaterialMapper.map(construction).stream())
                .collect(Collectors.toList());
    }

    /**
     * 按钢带代号补充标准卷曲长度，单位米。
     * <p>
     * 这里只读取t_cd15_curl_length的标准值；如果标准表没有维护或维护了非正数，
     * 后续排程会在拿到参数快照后再使用CRIMP_LENGTH兜底，避免输入加载接口反向依赖参数解析。
     * </p>
     */
    private void fillStandardCurlLength(String factoryCode, List<Cd15ConstructionMaterial> materials) {
        Set<String> steelStripCodes = safe(materials).stream()
                .filter(item -> item != null)
                .map(Cd15ConstructionMaterial::getSteelStripCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (steelStripCodes.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> curlLengthBySteelStrip = curlLengthMapper.selectList(
                        Wrappers.<Cd15CurlLength>lambdaQuery()
                                .select(Cd15CurlLength::getSteelStripCode, Cd15CurlLength::getCurlLength)
                                .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                                .in(Cd15CurlLength::getSteelStripCode, steelStripCodes)
                                .orderByAsc(Cd15CurlLength::getSteelStripCode))
                .stream()
                .filter(item -> item != null && StringUtils.hasText(item.getSteelStripCode())
                        && item.getCurlLength() != null && item.getCurlLength() > 0)
                .collect(Collectors.toMap(Cd15CurlLength::getSteelStripCode,
                        item -> BigDecimal.valueOf(item.getCurlLength()),
                        (first, second) -> first));
        safe(materials).stream()
                .filter(item -> item != null)
                .forEach(item -> item.setCurlLength(curlLengthBySteelStrip.get(item.getSteelStripCode())));
        Set<String> missing = steelStripCodes.stream()
                .filter(steelStripCode -> !curlLengthBySteelStrip.containsKey(steelStripCode))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            log.warn("[斜裁自动排程] 标准卷曲长度未维护或非正数，将使用参数CRIMP_LENGTH兜底, factoryCode={}, steelStripCodes={}",
                    factoryCode, missing);
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
