package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90CurlLength;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.algorithm.Cd90BigRollAgingStockBuilder;
import com.zlt.aps.cd90.engine.algorithm.Cd90ClothDepthResolver;
import com.zlt.aps.cd90.engine.algorithm.Cd90ClothSourceTraceResolver;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleSourceMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90ConstructionMaterialMapper;
import com.zlt.aps.cd90.engine.algorithm.Cd90FormingDemandExpander;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCurlLengthMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineDepthConfigMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStorageLaneMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMonthSurplusMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyScheduleResultMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyStockMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90BigRollAgingBuildResult;
import com.zlt.aps.cd90.engine.model.Cd90ClothSourceTrace;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoPlanSurplus;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
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
 * 直裁自动排程输入数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleInputServiceImpl implements Cd90AutoScheduleInputService {

    private static final int FORMING_SHIFT_HOURS = 8;
    private static final LocalTime FORMING_FIRST_SHIFT_TIME = LocalTime.of(6, 0);
    private static final LocalTime FIRST_FORMING_DEMAND_TIME = LocalTime.of(22, 0);

    private final Cd90EngineCxScheduleMapper cxScheduleMapper;
    private final Cd90EngineConstructionMapper constructionMapper;
    private final Cd90EngineStockMapper stockMapper;
    private final Cd90EngineStorageLaneMapper storageLaneMapper;
    private final Cd90EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd90EngineCurlLengthMapper curlLengthMapper;
    private final Cd90EngineDepthConfigMapper depthConfigMapper;
    private final Cd90ConstructionMaterialMapper constructionMaterialMapper;
    private final Cd90EngineXwyyStockMapper xwyyStockMapper;
    private final Cd90EngineXwyyScheduleResultMapper xwyyScheduleResultMapper;
    private final Cd90BigRollAgingStockBuilder bigRollAgingStockBuilder;
    private final Cd90AutoScheduleSourceMapper sourceMapper;
    private final Cd90FormingDemandExpander formingDemandExpander;
    private final Cd90ClothDepthResolver clothDepthResolver;
    private final Cd90ClothSourceTraceResolver clothSourceTraceResolver;

    /**
     * 加载第1至5步所需的成型计划、施工，以及任务启动时冻结的库存和库排快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 直裁结果班次字段
     * @param shiftCode 当前输出业务班次编码
     * @param resourceBaselineDate 任务启动时资源快照业务日期
     * @param resourceBaselineShiftCode 任务启动时资源快照班次
     * @return 标准化输入数据
     */
    @Override
    public Cd90AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode,
                                      LocalDate resourceBaselineDate,
                                      String resourceBaselineShiftCode,
                                      int agingPeriodHours) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");
        Assert.notNull(resourceBaselineDate, "资源基线日期不能为空");
        Assert.hasText(resourceBaselineShiftCode, "资源基线班次不能为空");

        LocalDate formingStartDate = scheduleDate.minusDays(1);
        LocalDate formingEndDate = scheduleDate.plusDays(3);
        log.info("[直裁自动排程] 加载成型计划, factoryCode={}, scheduleDate={}, formingStartDate={}, formingEndDate={}",
                factoryCode, scheduleDate, formingStartDate, formingEndDate);
        List<CxScheduleResult> formingEntities = loadFormingSchedules(
                factoryCode, formingStartDate, formingEndDate);
        log.info("[直裁自动排程] 成型计划加载结果, factoryCode={}, formingStartDate={}, formingEndDate={}, recordCount={}",
                factoryCode, formingStartDate, formingEndDate, formingEntities.size());
        List<Cd90FormingScheduleSource> formingSchedules = formingEntities.stream()
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
        List<Cd90ConstructionMaterial> constructionMaterials = loadConstructionMaterials(
                factoryCode, embryoCodes, constructionVersions);
        fillStandardCurlLength(factoryCode, constructionMaterials);
        LocalDateTime firstDemandStart = scheduleDate.minusDays(1)
                .atTime(FIRST_FORMING_DEMAND_TIME);
        List<Cd90DemandShift> demandShifts = formingDemandExpander.expand(
                formingSchedules, constructionMaterials, firstDemandStart);
        this.validateEffectiveDemand(
                firstDemandStart, formingSchedules, demandShifts, formingEntities.size());
        List<Cd90DepthConfig> depthConfigs = depthConfigMapper.selectList(
                Wrappers.<Cd90DepthConfig>lambdaQuery()
                        .eq(Cd90DepthConfig::getFactoryCode, factoryCode)
                        .orderByAsc(Cd90DepthConfig::getMinMachineQty));
        Map<String, BigDecimal> depthClassQtyByCloth = clothDepthResolver.resolve(
                formingSchedules, constructionMaterials, depthConfigs);
        List<Cd90EmbryoPlanSurplus> embryoPlanSurpluses = loadEmbryoPlanSurpluses(
                factoryCode, scheduleDate, embryoCodes);
        Map<String, Cd90ClothSourceTrace> clothSourceTraceByCloth =
                clothSourceTraceResolver.resolve(
                        formingSchedules, constructionMaterials, embryoPlanSurpluses);

        List<Cd90StockSource> stocksAtSix = stockMapper.selectList(Wrappers.<Cd90Stock>lambdaQuery()
                        .eq(Cd90Stock::getFactoryCode, factoryCode)
                        .eq(Cd90Stock::getStockDate, Date.valueOf(resourceBaselineDate))
                        .eq(Cd90Stock::getShiftCode, resourceBaselineShiftCode)
                        .orderByAsc(Cd90Stock::getMaterialCode))
                .stream()
                .map(sourceMapper::mapStock)
                .collect(Collectors.toList());

        List<Cd90StorageLaneState> storageLanesAtSix = storageLaneMapper.selectList(
                        Wrappers.<Cd90StorageLaneLimit>lambdaQuery()
                                .eq(Cd90StorageLaneLimit::getFactoryCode, factoryCode)
                                .eq(Cd90StorageLaneLimit::getLaneDate,
                                        Date.valueOf(resourceBaselineDate))
                                .eq(Cd90StorageLaneLimit::getShiftCode,
                                        resourceBaselineShiftCode)
                                .orderByAsc(Cd90StorageLaneLimit::getStorageLaneCode))
                .stream()
                .map(sourceMapper::mapStorageLane)
                .collect(Collectors.toList());

        List<XwyyStock> xwyyActualStocks = xwyyStockMapper.selectList(
                Wrappers.<XwyyStock>lambdaQuery()
                        .eq(XwyyStock::getFactoryCode, factoryCode)
                        .orderByAsc(XwyyStock::getStockInTime)
                        .orderByAsc(XwyyStock::getBigRollCode)
                        .orderByAsc(XwyyStock::getId));
        List<XwyyScheduleResult> xwyyPlans = xwyyScheduleResultMapper.selectList(
                Wrappers.<XwyyScheduleResult>lambdaQuery()
                        .eq(XwyyScheduleResult::getFactoryCode, factoryCode)
                        .between(XwyyScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)),
                                Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(XwyyScheduleResult::getScheduleDate)
                        .orderByAsc(XwyyScheduleResult::getId));
        Cd90BigRollAgingBuildResult agingResult = bigRollAgingStockBuilder.build(
                xwyyActualStocks, xwyyPlans, agingPeriodHours);

        log.info("[直裁自动排程] 输入数据加载完成, factoryCode={}, scheduleDate={}, classField={}, shiftCode={}, "
                        + "formingRange={}~{}, formingCount={}, constructionMaterialCount={}, "
                        + "demandShiftCount={}, depthClothCount={}, resourceBaselineDate={}, "
                        + "resourceBaselineShiftCode={}, "
                        + "stockCount={}, storageLaneCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingStartDate, formingEndDate,
                formingSchedules.size(), constructionMaterials.size(), demandShifts.size(),
                depthClassQtyByCloth.size(), resourceBaselineDate,
                resourceBaselineShiftCode, stocksAtSix.size(),
                storageLanesAtSix.size());

        return Cd90AutoScheduleInput.builder()
                .formingSchedules(formingSchedules)
                .constructionMaterials(constructionMaterials)
                .stocksAtSix(stocksAtSix)
                .embryoPlanSurpluses(embryoPlanSurpluses)
                .demandShifts(demandShifts)
                .clothSourceTraceByCloth(clothSourceTraceByCloth)
                .depthClassQtyByCloth(depthClassQtyByCloth)
                .storageLanesAtSix(storageLanesAtSix)
                .bigRollAgingStocks(agingResult.getStocks())
                .bigRollAgingDataMissingCodes(agingResult.getDataMissingBigRollCodes())
                .inboundRecords(Collections.emptyList())
                .build();
    }

    /** 按胎胚代码加载当前排程月份的月计划剩余量。 */
    private List<Cd90EmbryoPlanSurplus> loadEmbryoPlanSurpluses(String factoryCode,
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
                .stream().map(item -> Cd90EmbryoPlanSurplus.builder()
                        .embryoCode(item.getMaterialCode())
                        .planSurplusQuantity(item.getPlanSurplusQty()).build())
                .collect(Collectors.toList());
    }

    private List<CxScheduleResult> loadFormingSchedules(String factoryCode,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        // 直裁按胎胚代码分解施工层位，仅查询需求计算所需字段，避免共享实体的展示字段影响排程。
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

    private List<Cd90ConstructionMaterial> loadConstructionMaterials(String factoryCode,
                                                                      Set<String> embryoCodes,
                                                                      Set<String> constructionVersions) {
        if (embryoCodes.isEmpty()) {
            log.warn("[直裁自动排程] 成型计划未找到胎胚代码, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        if (constructionVersions.isEmpty()) {
            log.warn("[直裁自动排程] 成型计划未找到施工版本CLASSn_RECIPE_NO, factoryCode={}", factoryCode);
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
     * 按帘布代号补充标准卷曲长度，单位米。
     * <p>
     * 这里只读取t_cd90_curl_length的标准值；如果标准表没有维护或维护了非正数，
     * 后续排程会在拿到参数快照后再使用CRIMP_LENGTH兜底，避免输入加载接口反向依赖参数解析。
     * </p>
     */
    private void fillStandardCurlLength(String factoryCode, List<Cd90ConstructionMaterial> materials) {
        Set<String> clothCodes = safe(materials).stream()
                .filter(item -> item != null)
                .map(Cd90ConstructionMaterial::getClothCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (clothCodes.isEmpty()) {
            return;
        }
        Map<String, BigDecimal> curlLengthByCloth = curlLengthMapper.selectList(
                        Wrappers.<Cd90CurlLength>lambdaQuery()
                                .select(Cd90CurlLength::getClothCode, Cd90CurlLength::getCurlLength)
                                .eq(Cd90CurlLength::getFactoryCode, factoryCode)
                                .in(Cd90CurlLength::getClothCode, clothCodes)
                                .orderByAsc(Cd90CurlLength::getClothCode))
                .stream()
                .filter(item -> item != null && StringUtils.hasText(item.getClothCode())
                        && item.getCurlLength() != null && item.getCurlLength() > 0)
                .collect(Collectors.toMap(Cd90CurlLength::getClothCode,
                        item -> BigDecimal.valueOf(item.getCurlLength()),
                        (first, second) -> first));
        safe(materials).stream()
                .filter(item -> item != null)
                .forEach(item -> item.setCurlLength(curlLengthByCloth.get(item.getClothCode())));
        Set<String> missing = clothCodes.stream()
                .filter(clothCode -> !curlLengthByCloth.containsKey(clothCode))
                .collect(Collectors.toSet());
        if (!missing.isEmpty()) {
            log.warn("[直裁自动排程] 标准卷曲长度未维护或非正数，将使用参数CRIMP_LENGTH兜底, factoryCode={}, clothCodes={}",
                    factoryCode, missing);
        }
    }

    /** 晚班起点后的正计划量未形成任何有效帘布需求时终止任务。 */
    private void validateEffectiveDemand(LocalDateTime firstDemandStart,
                                         List<Cd90FormingScheduleSource> formingSchedules,
                                         List<Cd90DemandShift> demandShifts,
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
                        && shift.getClothDemandQuantity() != null
                        && shift.getClothDemandQuantity().signum() > 0);
        if (positiveFormingShiftCount > 0 && !hasEffectiveDemand) {
            throw new IllegalStateException(MessageFormat.format(
                    I18nUtil.getMessage("ui.cd90.autoSchedule.noEffectiveClothDemand"),
                    formingRecordCount, positiveFormingShiftCount));
        }
    }

    /** 将成型CLASS下标换算为自然班次开始时间。 */
    private LocalDateTime formingShiftStart(Cd90FormingScheduleSource schedule, int classIndex) {
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
