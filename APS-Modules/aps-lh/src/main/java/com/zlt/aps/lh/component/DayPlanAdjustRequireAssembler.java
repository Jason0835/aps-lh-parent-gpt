package com.zlt.aps.lh.component;

import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.api.enums.SkuTagEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 硫化日计划调整需求组装器。
 *
 * <p>职责独立于新增排产主循环：只从 {@code context.allLhDayPlanAdjustList} 加载本月月计划中
 * 不存在的物料，按“物料编码 + 产品状态”分组汇总调整量，并复用现有硫化余量口径判断是否进入
 * 硫化日计划调整待排清单。该组件不写入排程结果、不选机、不扣减任何运行态账本。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DayPlanAdjustRequireAssembler {

    /**
     * 无硫化产能数据时使用的默认硫化时间（秒），与新增排产兜底口径保持一致。
     */
    private static final int DEFAULT_LH_TIME_SECONDS = 3600;

    /**
     * 加载并汇总月计划不存在的日计划调整待排物料。
     *
     * <p>汇总口径：同一“物料编码 + 产品状态”的多条调整记录按调整量求和。只有同时满足
     * “汇总调整量 &gt; 0”和“复用现有口径计算出的硫化余量 &gt; 0”才进入待排清单。</p>
     *
     * @param context 排程上下文
     * @return 硫化日计划调整待排 SKU 列表，无有效物料时返回空列表
     */
    public List<SkuScheduleDTO> assemble(LhScheduleContext context) {
        if (Objects.isNull(context) || CollectionUtils.isEmpty(context.getAllLhDayPlanAdjustList())) {
            return Collections.emptyList();
        }

        Set<String> monthPlanMaterialStatusKeySet = this.buildMonthPlanMaterialStatusKeySet(context);
        Map<String, LhDayPlanAdjustVo> aggregatedAdjustMap = this.aggregateByMaterialStatus(context);
        if (CollectionUtils.isEmpty(aggregatedAdjustMap)) {
            return Collections.emptyList();
        }

        List<SkuScheduleDTO> resultList = new ArrayList<>(aggregatedAdjustMap.size());
        int scheduleOrder = 0;
        for (Map.Entry<String, LhDayPlanAdjustVo> entry : aggregatedAdjustMap.entrySet()) {
            LhDayPlanAdjustVo adjustVo = entry.getValue();
            if (StringUtils.isEmpty(adjustVo.getMaterialCode())) {
                continue;
            }
            String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    adjustVo.getMaterialCode(), adjustVo.getProductStatus());
            if (monthPlanMaterialStatusKeySet.contains(materialStatusKey)) {
                continue;
            }

            int adjustTotalQty = Math.max(0, adjustVo.getPlanQtyValue());
            if (adjustTotalQty <= 0) {
                continue;
            }

            int finishedQty = this.resolveFinishedQty(context, adjustVo.getMaterialCode(),
                    adjustVo.getProductStatus());
            int surplusQty = this.resolveSurplusQty(context, adjustVo.getMaterialCode(),
                    adjustVo.getProductStatus(), adjustTotalQty, finishedQty);
            if (surplusQty <= 0) {
                log.info("硫化日计划调整物料未进入待排清单, factoryCode: {}, materialCode: {}, "
                                + "productStatus: {}, adjustTotalQty: {}, finishedQty: {}, surplusQty: {}",
                        context.getFactoryCode(), adjustVo.getMaterialCode(), adjustVo.getProductStatus(),
                        adjustTotalQty, finishedQty, surplusQty);
                continue;
            }

            SkuScheduleDTO sku = this.buildSkuScheduleDTO(context, adjustVo, surplusQty, ++scheduleOrder);
            if (Objects.nonNull(sku)) {
                resultList.add(sku);
                log.info("硫化日计划调整物料进入待排清单, factoryCode: {}, materialCode: {}, "
                                + "productStatus: {}, adjustTotalQty: {}, finishedQty: {}, surplusQty: {}, "
                                + "scheduleOrder: {}",
                        context.getFactoryCode(), sku.getMaterialCode(), sku.getProductStatus(),
                        adjustTotalQty, finishedQty, surplusQty, scheduleOrder);
            }
        }
        return resultList;
    }

    /**
     * 构建当前已加载月计划的“物料编码 + 产品状态”集合。
     *
     * @param context 排程上下文
     * @return 已存在月计划的物料复合键集合
     */
    private Set<String> buildMonthPlanMaterialStatusKeySet(LhScheduleContext context) {
        List<FactoryMonthPlanProductionFinalResult> loadedPlanList = context.getLoadedMonthPlanList();
        if (CollectionUtils.isEmpty(loadedPlanList)) {
            return new HashSet<>(0);
        }
        Set<String> keySet = new HashSet<>(loadedPlanList.size() * 2);
        for (FactoryMonthPlanProductionFinalResult plan : loadedPlanList) {
            if (Objects.isNull(plan) || StringUtils.isEmpty(plan.getMaterialCode())) {
                continue;
            }
            keySet.add(MonthPlanDateResolver.buildMaterialStatusKey(
                    plan.getMaterialCode(), plan.getProductStatus()));
        }
        return keySet;
    }

    /**
     * 按“物料编码 + 产品状态”汇总日计划调整量。
     *
     * @param context 排程上下文
     * @return 物料复合键到汇总调整记录的映射，保留首次出现的物料描述
     */
    private Map<String, LhDayPlanAdjustVo> aggregateByMaterialStatus(LhScheduleContext context) {
        Map<String, LhDayPlanAdjustVo> aggregatedMap = new LinkedHashMap<>(16);
        for (LhDayPlanAdjustVo adjustVo : context.getAllLhDayPlanAdjustList()) {
            if (Objects.isNull(adjustVo) || StringUtils.isEmpty(adjustVo.getMaterialCode())) {
                continue;
            }
            String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    adjustVo.getMaterialCode(), adjustVo.getProductStatus());
            LhDayPlanAdjustVo existing = aggregatedMap.get(materialStatusKey);
            if (Objects.isNull(existing)) {
                existing = new LhDayPlanAdjustVo();
                existing.setFactoryCode(adjustVo.getFactoryCode());
                existing.setYear(adjustVo.getYear());
                existing.setMonth(adjustVo.getMonth());
                existing.setMaterialCode(adjustVo.getMaterialCode());
                existing.setMaterialDesc(adjustVo.getMaterialDesc());
                existing.setProductStatus(adjustVo.getProductStatus());
                existing.setMesMaterialCode(adjustVo.getMesMaterialCode());
                existing.setPlanQty(java.math.BigDecimal.ZERO);
                aggregatedMap.put(materialStatusKey, existing);
            }
            int currentQty = existing.getPlanQtyValue();
            int addQty = adjustVo.getPlanQtyValue();
            existing.setPlanQty(java.math.BigDecimal.valueOf(currentQty + addQty));
        }
        return aggregatedMap;
    }

    /**
     * 解析日计划调整物料的月累计完成量。
     *
     * <p>复用上下文已加载的月累计完成量与 T 日晚班完成量口径，不新增查询。</p>
     *
     * @param context       排程上下文
     * @param materialCode  物料编码
     * @param productStatus 产品状态
     * @return 已完成量
     */
    private int resolveFinishedQty(LhScheduleContext context, String materialCode, String productStatus) {
        String materialStatusKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, productStatus);
        int monthFinishedQty = Objects.isNull(context.getMaterialMonthFinishedQtyMap())
                ? 0 : context.getMaterialMonthFinishedQtyMap().getOrDefault(materialStatusKey, 0);
        int scheDayFinishQty = Objects.isNull(context.getMaterialScheDayFinishQtyMap())
                ? 0 : context.getMaterialScheDayFinishQtyMap().getOrDefault(materialStatusKey, 0);
        return Math.max(0, monthFinishedQty) + Math.max(0, scheDayFinishQty);
    }

    /**
     * 复用现有硫化余量口径计算日计划调整物料的有效余量。
     *
     * <p>无月计划物料的月计划基础量为 0，故把汇总调整量作为本月有效计划量传入共享计算器，
     * 余量 = 汇总调整量 - 已完成量 + 有效超欠产(0)。</p>
     *
     * @param context        排程上下文
     * @param materialCode   物料编码
     * @param productStatus  产品状态
     * @param adjustTotalQty 汇总调整量
     * @param finishedQty    已完成量
     * @return 硫化余量，最小为 0
     */
    private int resolveSurplusQty(LhScheduleContext context, String materialCode, String productStatus,
                                  int adjustTotalQty, int finishedQty) {
        if (Objects.isNull(context.getScheduleDate())) {
            return 0;
        }
        List<Date> allProductionDate = new ArrayList<>(context.getAllProductionDateInfo());
        YearMonth productionYearMonth = MonthPlanSurplusCalculator.getProductionYearAndMonth(
                context.getScheduleDate());
        Map<YearMonth, Integer> monthPlanQtyMap = new HashMap<>(4);
        monthPlanQtyMap.put(productionYearMonth, adjustTotalQty);
        Map<YearMonth, Integer> monthOverdueQtyMap = new HashMap<>(4);
        monthOverdueQtyMap.put(productionYearMonth, 0);
        Integer surplusQty = MonthPlanSurplusCalculator.getSurplusQty(
                productionYearMonth,
                allProductionDate,
                Collections.<YearMonth, FactoryMonthPlanProductionFinalResult>emptyMap(),
                monthOverdueQtyMap,
                monthPlanQtyMap,
                finishedQty);
        return Math.max(0, Objects.isNull(surplusQty) ? 0 : surplusQty);
    }

    /**
     * 根据汇总调整记录与主数据组装日计划调整 SKU DTO。
     *
     * @param context       排程上下文
     * @param adjustVo      汇总后的调整记录
     * @param surplusQty    硫化余量
     * @param scheduleOrder 排产顺序
     * @return 日计划调整 SKU DTO
     */
    private SkuScheduleDTO buildSkuScheduleDTO(LhScheduleContext context,
                                               LhDayPlanAdjustVo adjustVo,
                                               int surplusQty,
                                               int scheduleOrder) {
        SkuScheduleDTO dto = new SkuScheduleDTO();
        dto.setMaterialCode(adjustVo.getMaterialCode());
        dto.setProductStatus(adjustVo.getProductStatus());
        dto.setMaterialDesc(adjustVo.getMaterialDesc());
        dto.setScheduleType(ScheduleTypeEnum.NEW_SPEC.getCode());
        dto.setSourceType(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getCode());
        dto.setSkuTag(SkuTagEnum.NORMAL.getCode());
        dto.setStrictTargetQty(true);

        this.fillMaterialAttribute(context, dto);
        this.fillCapacity(context, dto);
        this.fillMould(context, dto);
        this.fillEmbryoStock(context, dto);

        dto.setSurplusQty(surplusQty);
        dto.setMonthPlanQty(surplusQty);
        dto.setMonthPlanSumTotal(surplusQty);
        dto.setFinishedQty(0);
        dto.setPendingQty(surplusQty);
        dto.setWindowPlanQty(surplusQty);
        dto.setOriginalWindowPlanQty(surplusQty);
        dto.setWindowRemainingPlanQty(surplusQty);
        dto.setTargetScheduleQty(surplusQty);
        dto.setDailyPlanQuotaMap(this.buildWindowDailyPlanQuotaMap(context, surplusQty));
        dto.setScheduleOrder(scheduleOrder);
        dto.setSortRank(scheduleOrder);
        dto.setSortDesc(SkuScheduleSourceTypeEnum.DAY_PLAN_ADJUST.getDescription());
        return dto;
    }

    /**
     * 从物料主数据与施工关系回填结构、规格、花纹、胎胚等选机属性。
     *
     * @param context 排程上下文
     * @param dto     日计划调整 SKU DTO
     */
    private void fillMaterialAttribute(LhScheduleContext context, SkuScheduleDTO dto) {
        MdmMaterialInfo materialInfo = Objects.isNull(context.getMaterialInfoMap())
                ? null : context.getMaterialInfoMap().get(dto.getMaterialCode());
        if (Objects.nonNull(materialInfo)) {
            dto.setStructureName(materialInfo.getStructureName());
            dto.setSpecCode(materialInfo.getSpecifications());
            dto.setPattern(materialInfo.getPattern());
            dto.setMainPattern(materialInfo.getMainPattern());
            if (StringUtils.isEmpty(dto.getMaterialDesc())) {
                dto.setMaterialDesc(materialInfo.getMaterialDesc());
            }
        }
        MdmSkuConstructionRef constructionRef = context.findSkuConstructionRef(
                dto.getMaterialCode(), dto.getProductStatus());
        if (Objects.nonNull(constructionRef)) {
            dto.setEmbryoCode(constructionRef.getEmbryoCode());
            if (StringUtils.isEmpty(dto.getSpecCode())) {
                dto.setSpecCode(constructionRef.getSpecCode());
            }
            dto.setEmbryoNo(constructionRef.getEmbryoNo());
        }
    }

    /**
     * 从 SKU 硫化产能回填硫化时间、班产与日产能。
     *
     * @param context 排程上下文
     * @param dto     日计划调整 SKU DTO
     */
    private void fillCapacity(LhScheduleContext context, SkuScheduleDTO dto) {
        MdmSkuLhCapacity capacity = Objects.isNull(context.getSkuLhCapacityMap())
                ? null : context.getSkuLhCapacityMap().get(dto.getMaterialCode());
        if (Objects.isNull(capacity)) {
            dto.setLhTimeSeconds(DEFAULT_LH_TIME_SECONDS);
            dto.setShiftCapacity(0);
            dto.setDailyCapacity(0);
            log.warn("硫化日计划调整物料缺少硫化产能, materialCode: {}, productStatus: {}",
                    dto.getMaterialCode(), dto.getProductStatus());
            return;
        }
        int lhTimeSeconds = Objects.isNull(capacity.getVulcanizationTime())
                ? DEFAULT_LH_TIME_SECONDS : capacity.getVulcanizationTime();
        int shiftCapacity = Objects.isNull(capacity.getClassCapacity()) ? 0 : capacity.getClassCapacity();
        int dailyCapacity = Objects.isNull(capacity.getApsCapacity())
                ? (Objects.isNull(capacity.getStandardCapacity()) ? 0 : capacity.getStandardCapacity())
                : capacity.getApsCapacity();
        dto.setLhTimeSeconds(lhTimeSeconds);
        dto.setShiftCapacity(shiftCapacity);
        dto.setDailyCapacity(dailyCapacity);
    }

    /**
     * 从 SKU 模具关系回填模具列表与模数。
     *
     * @param context 排程上下文
     * @param dto     日计划调整 SKU DTO
     */
    private void fillMould(LhScheduleContext context, SkuScheduleDTO dto) {
        List<MdmSkuMouldRel> mouldRelList = Objects.isNull(context.getSkuMouldRelMap())
                ? Collections.<MdmSkuMouldRel>emptyList() : context.getSkuMouldRelMap().get(dto.getMaterialCode());
        if (CollectionUtils.isEmpty(mouldRelList)) {
            return;
        }
        List<String> mouldCodeList = mouldRelList.stream()
                .filter(Objects::nonNull)
                .map(MdmSkuMouldRel::getMouldCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .collect(Collectors.toList());
        dto.setMouldCodeList(mouldCodeList);
        dto.setMouldQty(mouldCodeList.size());
    }

    /**
     * 从实时胎胚库存回填胎胚库存，胎胚编码缺失时保持未知库存。
     *
     * @param context 排程上下文
     * @param dto     日计划调整 SKU DTO
     */
    private void fillEmbryoStock(LhScheduleContext context, SkuScheduleDTO dto) {
        dto.setEmbryoStock(-1);
        if (StringUtils.isEmpty(dto.getEmbryoCode())
                || Objects.isNull(context.getEmbryoRealtimeStockMap())
                || !context.getEmbryoRealtimeStockMap().containsKey(dto.getEmbryoCode())) {
            return;
        }
        Integer embryoStock = context.getEmbryoRealtimeStockMap().get(dto.getEmbryoCode());
        if (Objects.nonNull(embryoStock)) {
            dto.setEmbryoStock(embryoStock);
        }
    }

    /**
     * 将汇总调整量作为 T 日（排程窗口首日）日计划额度，供新增排产日驱动主链按日准入。
     *
     * @param context    排程上下文
     * @param surplusQty 硫化余量
     * @return 窗口日计划额度账本，仅 T 日有正计划量
     */
    private Map<LocalDate, SkuDailyPlanQuotaDTO> buildWindowDailyPlanQuotaMap(LhScheduleContext context,
                                                                              int surplusQty) {
        Map<LocalDate, SkuDailyPlanQuotaDTO> quotaMap = new LinkedHashMap<>(8);
        if (Objects.isNull(context.getScheduleDate())) {
            return quotaMap;
        }
        LocalDate scheduleDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        SkuDailyPlanQuotaDTO quota = new SkuDailyPlanQuotaDTO();
        quota.setProductionDate(scheduleDate);
        quota.setDayPlanQty(surplusQty);
        quota.setScheduledQty(0);
        quota.setRemainingQty(surplusQty);
        quota.setShiftFillOverQty(0);
        quota.setCarryLossQty(0);
        quota.setFutureBorrowQty(0);
        quota.setActualQty(0);
        quota.setCumulativeQty(0);
        quota.setFinalLossQty(0);
        quota.setCompleted(false);
        quotaMap.put(scheduleDate, quota);
        SkuDailyPlanQuotaUtil.refreshRollingFields(quotaMap);
        return quotaMap;
    }
}
