package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15ShiftConfig;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.api.domain.entity.Cd15StorageLaneLimit;
import com.zlt.aps.cd15.engine.algorithm.Cd15BigRollAgingStockBuilder;
import com.zlt.aps.cd15.engine.algorithm.Cd15SteelStripDepthResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15SteelStripSourceTraceResolver;
import com.zlt.aps.cd15.engine.algorithm.Cd15StorageLaneBaselineValidator;
import com.zlt.aps.cd15.engine.mapper.Cd15AutoScheduleSourceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15ConstructionMaterialMapper;
import com.zlt.aps.cd15.engine.algorithm.Cd15FormingDemandExpander;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineDepthConfigMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStorageLaneMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineShiftConfigMapper;
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
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 斜裁自动排程输入数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd15AutoScheduleInputServiceImpl implements Cd15AutoScheduleInputService {

    private static final int FORMING_SHIFT_HOURS = 8;
    private static final LocalTime FORMING_FIRST_SHIFT_TIME = LocalTime.of(6, 0);
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

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
    private final Cd15EngineShiftConfigMapper shiftConfigMapper;
    private final Cd15BigRollAgingStockBuilder bigRollAgingStockBuilder;
    private final Cd15AutoScheduleSourceMapper sourceMapper;
    private final Cd15FormingDemandExpander formingDemandExpander;
    private final Cd15SteelStripDepthResolver steelStripDepthResolver;
    private final Cd15SteelStripSourceTraceResolver steelStripSourceTraceResolver;
    private final Cd15StorageLaneBaselineValidator storageLaneBaselineValidator;

    /**
     * 加载第1至5步所需的成型计划、施工、6点库存和任务启动时当前班次库排基线。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 斜裁结果班次字段
     * @param shiftCode 当前业务班次编码
     * @param resourceBaselineDate 库排资源基线日期
     * @param resourceBaselineShiftCode 库排资源基线班次
     * @param agingPeriodHours 大卷静置时长（小时）
     * @return 标准化输入数据
     */
    @Override
    public Cd15AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode,
                                      LocalDate resourceBaselineDate,
                                      String resourceBaselineShiftCode,
                                      int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");
        Assert.notNull(resourceBaselineDate, "库排资源基线日期不能为空");
        Assert.hasText(resourceBaselineShiftCode, "库排资源基线班次不能为空");

        log.info("[斜裁自动排程] 加载成型计划, factoryCode={}, scheduleDate={}",
                factoryCode, scheduleDate);
        List<CxScheduleResult> formingEntities = loadFormingSchedules(factoryCode, scheduleDate);
        log.info("[斜裁自动排程] 成型计划加载结果, factoryCode={}, scheduleDate={}, recordCount={}",
                factoryCode, scheduleDate, formingEntities.size());
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
        LocalDateTime firstDemandStart = scheduleDate.minusDays(1)
                .atTime(FIRST_FORMING_DEMAND_TIME);
        List<Cd15DemandShift> demandShifts = formingDemandExpander.expand(
                formingSchedules, constructionMaterials, firstDemandStart);
        this.validateEffectiveDemand(
                firstDemandStart, formingSchedules, demandShifts, formingEntities.size());
        List<Cd15DepthConfig> depthConfigs = depthConfigMapper.selectList(
                Wrappers.<Cd15DepthConfig>lambdaQuery()
                        .eq(Cd15DepthConfig::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15DepthConfig::getMinMachineQty));
        Map<String, BigDecimal> depthClassQtyBySteelStrip = steelStripDepthResolver.resolve(
                formingSchedules, constructionMaterials, depthConfigs);
        List<Cd15EmbryoPlanSurplus> embryoPlanSurpluses = loadEmbryoPlanSurpluses(
                factoryCode, scheduleDate, embryoCodes);
        Map<String, Cd15SteelStripSourceTrace> steelStripSourceTraceBySteelStrip =
                steelStripSourceTraceResolver.resolve(
                        formingSchedules, constructionMaterials, embryoPlanSurpluses);

        List<Cd15StockSource> stocksAtSix = stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                        .eq(Cd15Stock::getFactoryCode, factoryCode)
                        .eq(Cd15Stock::getStockDate, Date.valueOf(resourceBaselineDate))
                        .eq(Cd15Stock::getShiftCode, resourceBaselineShiftCode)
                        .orderByAsc(Cd15Stock::getMaterialCode))
                .stream()
                .map(sourceMapper::mapStock)
                .collect(Collectors.toList());

        List<Cd15StorageLaneLimit> storageLaneBaseline = storageLaneMapper.selectList(
                Wrappers.<Cd15StorageLaneLimit>lambdaQuery()
                        .eq(Cd15StorageLaneLimit::getFactoryCode, factoryCode)
                        .eq(Cd15StorageLaneLimit::getLaneDate,
                                Date.valueOf(resourceBaselineDate))
                        .eq(Cd15StorageLaneLimit::getShiftCode,
                                resourceBaselineShiftCode)
                        .orderByAsc(Cd15StorageLaneLimit::getStorageLaneCode));
        this.storageLaneBaselineValidator.validateUnique(
                resourceBaselineDate, resourceBaselineShiftCode, storageLaneBaseline);
        List<Cd15StorageLaneState> storageLanesAtSix = storageLaneBaseline.stream()
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
        List<Cd15ShiftConfig> shiftConfigs = shiftConfigMapper.selectList(
                Wrappers.<Cd15ShiftConfig>lambdaQuery()
                        .eq(Cd15ShiftConfig::getFactoryCode, factoryCode)
                        .eq(Cd15ShiftConfig::getIsActive, 1)
                        .orderByAsc(Cd15ShiftConfig::getShiftOrder)
                        .orderByAsc(Cd15ShiftConfig::getClassField));
        Cd15BigRollAgingBuildResult agingResult = bigRollAgingStockBuilder.build(
                gdyyActualStocks, gdyyPlans, shiftConfigs, agingPeriodHours);

        log.info("[斜裁自动排程] 输入数据加载完成, factoryCode={}, scheduleDate={}, classField={}, shiftCode={}, "
                        + "formingCount={}, constructionMaterialCount={}, "
                        + "demandShiftCount={}, depthSteelStripCount={}, resourceBaselineDate={}, "
                        + "resourceBaselineShiftCode={}, stockCount={}, storageLaneCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingSchedules.size(),
                constructionMaterials.size(), demandShifts.size(),
                depthClassQtyBySteelStrip.size(), resourceBaselineDate,
                resourceBaselineShiftCode, stocksAtSix.size(), storageLanesAtSix.size());

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
                                                         LocalDate scheduleDate) {
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
                .eq(CxScheduleResult::getScheduleDate, Date.valueOf(scheduleDate))
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

    /** 晚班起点后的正计划量未形成任何有效钢带需求时终止任务。 */
    private void validateEffectiveDemand(LocalDateTime firstDemandStart,
                                         List<Cd15FormingScheduleSource> formingSchedules,
                                         List<Cd15DemandShift> demandShifts,
                                         int formingRecordCount) {
        long positiveFormingShiftCount = this.safe(formingSchedules).stream()
                .filter(schedule -> schedule != null)
                .flatMapToLong(schedule -> IntStream.range(
                                0, Math.min(8, this.safe(schedule.getClassPlanQuantities()).size()))
                        .filter(index -> {
                            BigDecimal quantity = schedule.getClassPlanQuantities().get(index);
                            LocalDateTime shiftStart = this.formingShiftStart(schedule, index);
                            return quantity != null && quantity.signum() > 0
                                    && shiftStart != null
                                    && !shiftStart.isBefore(firstDemandStart);
                        })
                        .mapToLong(index -> 1L))
                .count();
        boolean hasEffectiveDemand = this.safe(demandShifts).stream()
                .filter(shift -> shift != null && shift.getStartTime() != null)
                .filter(shift -> !shift.getStartTime().isBefore(firstDemandStart))
                .anyMatch(shift -> shift.getFormingQuantity() != null
                        && shift.getFormingQuantity().signum() > 0
                        && shift.getSteelStripDemandQuantity() != null
                        && shift.getSteelStripDemandQuantity().signum() > 0);
        if (positiveFormingShiftCount > 0 && !hasEffectiveDemand) {
            throw new IllegalStateException(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd15.autoSchedule.noEffectiveSteelStripDemand"),
                    formingRecordCount, positiveFormingShiftCount));
        }
    }

    /** 将成型CLASS下标换算为自然班次开始时间。 */
    private LocalDateTime formingShiftStart(Cd15FormingScheduleSource schedule, int classIndex) {
        if (schedule == null || schedule.getScheduleDate() == null) {
            return null;
        }
        return schedule.getScheduleDate().minusDays(1).atTime(FORMING_FIRST_SHIFT_TIME)
                .plusHours(classIndex * (long) FORMING_SHIFT_HOURS);
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
