package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

/**
 * 胎胚库存收尾的零库存禁排及后半段正库存准入规则。
 * 仅读取本轮原始标识、库存及共用关系，不冻结份额或修改需求、资源及消费账本。
 *
 * @author APS
 * @date 2026/11/04
 */
public final class EmbryoEndingStockAdmissionPolicy {

    /**
     * 胎胚收尾标识。
     */
    private static final Integer EMBRYO_ENDING_FLAG = 1;

    /**
     * 同SKU构成共用胎胚的最少物理机台数。
     */
    private static final int SHARED_EMBRYO_MIN_MACHINE_COUNT = 2;

    /**
     * 月计划首日序号。
     */
    private static final int MIN_DAY_OF_MONTH = 1;

    /**
     * 禁止实例化无状态准入规则。
     */
    private EmbryoEndingStockAdmissionPolicy() {
    }

    /**
     * 判断普通排程的后半段无计划SKU是否具有胎胚库存收尾资格。
     * 仅单胎胚或非共用胎胚可以清库存；准入阶段读取原始归集关系，不能借过滤后的活跃集合取得资格。
     *
     * @param context 本轮排程模式、胎胚收尾标识与库存快照
     * @param sku 待判断的物料及产品状态
     * @return 后半段窗口无计划、胎胚收尾标识为1、本轮库存为正且不是共用胎胚时返回true
     */
    public static boolean isEligible(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || context.isStopProductionMode()
                || sku.isOverdueSurplusFlag() || sku.getOriginalWindowPlanQty() > 0
                || StringUtils.isEmpty(sku.getEmbryoCode())
                || !EMBRYO_ENDING_FLAG.equals(context.getEmbryoEndingFlagMap().get(sku.getEmbryoCode()))) {
            return false;
        }
        Integer stockQty = context.getEmbryoRealtimeStockMap().get(sku.getEmbryoCode());
        return Objects.nonNull(stockQty) && stockQty > 0 && !isSharedOrUnusedEmbryoSource(context, sku);
    }

    /**
     * 判断胎胚库存收尾来源是否已无本轮可用库存。
     * 缺记录和非正库存均不得回退硫化余量或旧DTO库存，历史交替身份不豁免。
     * 单胎胚、共用胎胚及同物料多机台统一禁排，不依赖候选或份额冻结状态。
     *
     * @param context 本轮胎胚收尾标识和T日库存
     * @param sku 当前物料及产品状态来源
     * @return 胎胚收尾且本轮库存缺失或非正时返回true
     */
    public static boolean isEmbryoEndingWithoutStock(LhScheduleContext context, SkuScheduleDTO sku) {
        if (Objects.isNull(context) || Objects.isNull(sku) || StringUtils.isBlank(sku.getEmbryoCode())
                || !EMBRYO_ENDING_FLAG.equals(context.getEmbryoEndingFlagMap().get(sku.getEmbryoCode()))) {
            return false;
        }
        Integer stockQty = context.getEmbryoRealtimeStockMap().get(sku.getEmbryoCode());
        return Objects.isNull(stockQty) || stockQty <= 0;
    }

    /**
     * 按有效用胚来源判断是否为共用胎胚，或当前空计划行不能承接其他SKU的库存。
     * 月计划仅将T日至月底有正日计划的其他SKU视为用胚，实际在机来源仍保留；同SKU多物理机台也按共用处理。
     * 没有本月正日计划且未实际在机的空计划行，不得借其他本月用胚SKU的库存建立清量需求。
     * 仅读取原始来源与MES事实，不刷新活跃集合，避免循环依赖准入结果。
     *
     * @param context 原始归集月计划、运行来源及MES在机事实
     * @param sku 待评估的物料及产品状态
     * @return 属于共用胎胚，或未用胚空计划行试图承接其他SKU库存时返回true
     */
    private static boolean isSharedOrUnusedEmbryoSource(LhScheduleContext context, SkuScheduleDTO sku) {
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
        Set<String> onlineSkuKeys = context.getMachineOnlineInfoMap().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue())
                        && context.getMachineScheduleMap().containsKey(entry.getKey()))
                .map(entry -> MonthPlanDateResolver.buildMaterialStatusKey(entry.getValue().getMaterialCode(),
                        StringUtils.defaultIfBlank(entry.getValue().getProductStatus(), TrialStatusEnum.FORMAL.getCode())))
                .collect(Collectors.toSet());
        Set<String> monthlyUsageSkuKeys = context.getMonthPlanList().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(sku.getEmbryoCode(), plan.getEmbryoCode()))
                .filter(plan -> hasPositivePlanInScheduleMonth(context, plan, true))
                .map(plan -> MonthPlanDateResolver.buildMaterialStatusKey(plan.getMaterialCode(), plan.getProductStatus()))
                .collect(Collectors.toSet());
        if (!monthlyUsageSkuKeys.isEmpty() && !monthlyUsageSkuKeys.contains(skuKey)
                && !onlineSkuKeys.contains(skuKey)) {
            return true;
        }
        Stream<String> monthlySkuKeys = context.getMonthPlanList().stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.equals(sku.getEmbryoCode(), plan.getEmbryoCode()))
                .filter(plan -> hasPositivePlanInScheduleMonth(context, plan, false)
                        || onlineSkuKeys.contains(MonthPlanDateResolver.buildMaterialStatusKey(
                        plan.getMaterialCode(), plan.getProductStatus())))
                .map(plan -> MonthPlanDateResolver.buildMaterialStatusKey(plan.getMaterialCode(), plan.getProductStatus()));
        Stream<String> runtimeSkuKeys = Stream.concat(context.getContinuousSkuList().stream(),
                        context.getPreviousAlternateNewSpecCandidates().stream())
                .filter(Objects::nonNull)
                .filter(candidate -> StringUtils.equals(sku.getEmbryoCode(), candidate.getEmbryoCode()))
                .map(candidate -> MonthPlanDateResolver.buildMaterialStatusKey(
                        candidate.getMaterialCode(), candidate.getProductStatus()));
        if (Stream.concat(monthlySkuKeys, runtimeSkuKeys).anyMatch(sourceKey -> !StringUtils.equals(skuKey, sourceKey))) {
            return true;
        }
        String productStatus = StringUtils.defaultIfBlank(sku.getProductStatus(), TrialStatusEnum.FORMAL.getCode());
        Stream<String> onlineMachines = context.getMachineOnlineInfoMap().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue())
                        && context.getMachineScheduleMap().containsKey(entry.getKey()))
                .filter(entry -> StringUtils.equals(sku.getMaterialCode(), entry.getValue().getMaterialCode())
                        && StringUtils.equals(productStatus,
                        StringUtils.defaultIfBlank(entry.getValue().getProductStatus(), TrialStatusEnum.FORMAL.getCode())))
                .map(Map.Entry::getKey);
        Stream<String> continuousMachines = context.getContinuousSkuList().stream()
                .filter(Objects::nonNull)
                .filter(candidate -> StringUtils.equals(skuKey, MonthPlanDateResolver.buildMaterialStatusKey(
                        candidate.getMaterialCode(), candidate.getProductStatus())))
                .map(SkuScheduleDTO::getContinuousMachineCode);
        return Stream.concat(onlineMachines, continuousMachines)
                .filter(StringUtils::isNotBlank)
                .filter(machineCode -> !LhSingleControlMachineUtil.isVirtualMachineCode(machineCode))
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .distinct().limit(SHARED_EMBRYO_MIN_MACHINE_COUNT).count() >= SHARED_EMBRYO_MIN_MACHINE_COUNT;
    }

    /**
     * 判断原始月计划在T日所属月份是否存在实际用胚日计划。
     * 整月口径识别库存需求所属SKU，剩余月口径判断本轮是否仍与其他SKU共用。
     *
     * @param context 提供本轮T日的上下文
     * @param plan 待判断的原始月计划
     * @param includePastDays 是否包含T日前日期
     * @return 指定月份日期范围存在正日计划时返回true
     */
    private static boolean hasPositivePlanInScheduleMonth(LhScheduleContext context,
            FactoryMonthPlanProductionFinalResult plan, boolean includePastDays) {
        if (Objects.isNull(context.getScheduleDate())) {
            return false;
        }
        LocalDate scheduleDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Objects.equals(plan.getYear(), scheduleDate.getYear())
                && Objects.equals(plan.getMonth(), scheduleDate.getMonthValue())
                && IntStream.rangeClosed(includePastDays ? MIN_DAY_OF_MONTH : scheduleDate.getDayOfMonth(),
                        scheduleDate.lengthOfMonth())
                .anyMatch(dayIndex -> MonthPlanDayQtyUtil.resolveDayQty(plan, dayIndex) > 0);
    }

}
