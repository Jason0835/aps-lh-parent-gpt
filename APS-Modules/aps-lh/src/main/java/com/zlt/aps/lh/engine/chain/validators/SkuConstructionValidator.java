package com.zlt.aps.lh.engine.chain.validators;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.lh.api.constant.LhDataValidationGroupConstant;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.api.enums.TrialStatusEnum;
import com.zlt.aps.lh.api.enums.ValidationPolicyEnum;
import com.zlt.aps.lh.component.SkuDecrementChecker;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.chain.IDataValidator;
import com.zlt.aps.lh.handler.ScheduleAdjustHandler;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.MonthPlanDayQtyUtil;
import com.zlt.aps.lh.util.SkuConstructionRefResolverUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SKU与示方书关系校验器
 * <p>按物料编码 + 产品状态查找示方书关系（支持降级匹配），校验 lhNo 和 lhType 是否为空。</p>
 * <p>降级规则：正规(S)→量试(T)→试制(X)；量试(T)→试制(X)；试制(X)不降级。</p>
 *
 * <p>跳过校验规则（不进入排产的物料无需示方书，避免示方书缺失误阻断整批排程）：</p>
 * <ul>
 *   <li>命中 SKU 减量清单的物料（S4.2 已加载索引，S4.3 前置过滤会拦截其全部排产入口）；</li>
 *   <li>胎胚收尾且胎胚实时库存为 0 的物料（无库存记录默认 0，不再有可硫化胎胚）；</li>
 *   <li>非胎胚收尾且硫化余量为 0 的物料（口径与 {@code loadEmbryoEndingFlagMap} 的
 *       {@code calculatePlanSurplusQty} 一致，不进入排产）；</li>
 *   <li>例外：MES 在机续作物料豁免零库存/零余量跳过——在机续作不受「无计划量不排产」拦截
 *       仍会产出结果行，结果行 LH_NO 依赖示方书关系，必须继续校验。</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Component
public class SkuConstructionValidator implements IDataValidator {

    private static final String VALIDATOR_KEY = "skuConstructionValidator";

    /** SKU减量清单过滤公共组件（复用其归一化 key 口径，禁止在本校验器另拼 key） */
    @Resource
    private SkuDecrementChecker skuDecrementChecker;

    /** 胎胚收尾标识判定组件（单一口径复用，与排产阶段判定保持一致） */
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    /** 硫化余量计算组件（与 loadEmbryoEndingFlagMap 使用完全相同的余量口径） */
    @Resource
    private ScheduleAdjustHandler scheduleAdjustHandler;

    /**
     * 校验月计划物料的示方书关系，按来源年月判断减量豁免并保留既有库存、余量边界。
     *
     * @param context 当前工厂排程上下文
     * @return 所有需要校验的月计划物料均具备示方书时返回true
     */
    @Override
    public boolean validate(LhScheduleContext context) {
        // 只校验月生产计划列表中的物料
        List<FactoryMonthPlanProductionFinalResult> monthPlanList = context.getMonthPlanList();
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return true;
        }
        Map<String, MdmSkuConstructionRef> compositeKeyMap = context.getSkuConstructionRefCompositeKeyMap();
        if (CollectionUtils.isEmpty(compositeKeyMap)) {
            log.warn("SKU与示方书关系数据为空, 工厂: {}", context.getFactoryCode());
            context.addValidationError("[" + getValidatorName() + "] "
                    + String.format(I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.skuConstructionEmpty"), context.getFactoryDisplayName()));
            return false;
        }
        // MES在机物料集合：在机续作物料仍会产出结果行（结果LH_NO依赖示方书），零库存/零余量跳过对其豁免
        Set<String> onMachineMaterialCodeSet = context.getMachineOnlineInfoMap().values().stream()
                .filter(Objects::nonNull)
                .map(LhMachineOnlineInfo::getMaterialCode)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        // 遍历月计划物料，按物料编码+产品状态降级查找并校验 lhNo/lhType
        Map<String, String> missingFieldMap = new LinkedHashMap<>();
        for (FactoryMonthPlanProductionFinalResult plan : monthPlanList) {
            // 月计划排产量为空或为0的物料跳过校验
            if (plan.getTotalQty() == null || plan.getTotalQty() == 0) {
                continue;
            }
            String materialCode = plan.getMaterialCode();
            if (StringUtils.isEmpty(materialCode)) {
                continue;
            }
            String productStatus = plan.getProductStatus();
            // 命中SKU减量清单的物料不进入任何排产入口（含试制/量试虚拟机台兜底），无需示方书校验
            if (skuDecrementChecker.isDecrementHit(context, plan.getYear(), plan.getMonth(),
                    materialCode, productStatus)) {
                log.info("命中SKU减量清单，跳过示方书校验, 工厂: {}, 物料: {}, 产品状态: {}",
                        context.getFactoryCode(), materialCode, productStatus);
                continue;
            }
            // 胎胚收尾零库存 / 非胎胚收尾零余量物料不进入排产，无需示方书校验（MES在机续作物料豁免）。
            // 判定位置特意放在窗口过滤之前：窗口无计划量的物料可能仍有欠产余量需要校验，
            // 反之月余量为0的物料即便窗口有量也不排产，零余量/零库存是更强也更直接的跳过条件。
            if (this.shouldSkipByEmbryoStockOrSurplus(context, plan, onMachineMaterialCodeSet)) {
                continue;
            }
            /*
             * 排程窗口内所有日计划量均为0或空的物料跳过校验；
             * 试制(X)/量试(T)例外：其计划日常落在排程窗口之外，但仍会被试制量试虚拟机台兜底排产，
             * 示方书缺失必须在前置校验阶段阻断，不能按“窗口无计划量”跳过。
             */
            if (!hasWindowPlanQty(context, plan) && !TrialStatusEnum.isTrialStatus(productStatus)) {
                continue;
            }
            String statusDesc = SkuConstructionRefResolverUtil.resolveProductStatusDesc(productStatus);
            // 使用降级匹配公共方法，与排程结果赋值逻辑保持一致
            MdmSkuConstructionRef ref = SkuConstructionRefResolverUtil.resolveCuringRecipeRef(
                    materialCode, productStatus, compositeKeyMap);
            if (Objects.isNull(ref)) {
                missingFieldMap.put(materialCode, I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.constructionNotFound"));
                continue;
            }
            // 校验 lhNo / lhType 是否为空
            if (StringUtils.isEmpty(ref.getLhNo()) && StringUtils.isEmpty(ref.getLhType())) {
                missingFieldMap.put(materialCode, I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.lhNoAndTypeEmpty"));
            } else if (StringUtils.isEmpty(ref.getLhNo())) {
                missingFieldMap.put(materialCode, I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.lhNoEmpty"));
            } else if (StringUtils.isEmpty(ref.getLhType())) {
                missingFieldMap.put(materialCode, I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.lhTypeEmpty"));
            }
        }
        if (!missingFieldMap.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("[").append(getValidatorName()).append("] ");
            errorMsg.append(I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.skuConstructionIncomplete")).append(" ");
            for (Map.Entry<String, String> missingEntry : missingFieldMap.entrySet()) {
                errorMsg.append("[").append(I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.materialCode"))
                        .append(":").append(missingEntry.getKey())
                        .append(", ").append(missingEntry.getValue()).append("]; ");
            }
            String errorText = errorMsg.toString();
            log.warn("SKU与示方书关系校验失败, 工厂: {}, 异常物料数: {}, 详情: {}",
                    context.getFactoryCode(), missingFieldMap.size(), errorText);
            context.addValidationError(errorText);
            return false;
        }
        log.info("SKU与示方书关系校验通过, 月计划物料数: {}", monthPlanList.size());
        return true;
    }

    @Override
    public String getValidatorName() {
        return I18nUtil.getMessage("ui.data.column.lhScheduleResult.validator.skuConstructionName");
    }

    @Override
    public String getValidatorKey() {
        return VALIDATOR_KEY;
    }

    @Override
    public int getGroup() {
        return LhDataValidationGroupConstant.BASE_DATA_INTEGRITY;
    }

    @Override
    public ValidationPolicyEnum getValidationPolicy() {
        return ValidationPolicyEnum.COLLECT_ALL;
    }

    @Override
    public int getOrder() {
        return 25;
    }

    /**
     * 判断月计划行是否满足胎胚零库存/物料零余量跳过条件。
     *
     * <p>跳过口径：</p>
     * <ul>
     *   <li>胎胚收尾：胎胚实时库存为 0（无库存记录默认 0）时不再有可硫化胎胚，跳过校验；</li>
     *   <li>非胎胚收尾：硫化余量为 0 时不进入排产，跳过校验；余量口径复用
     *       {@link ScheduleAdjustHandler#calculatePlanSurplusQty}，与
     *       {@code loadEmbryoEndingFlagMap} 在 S4.2 的判定完全一致。</li>
     * </ul>
     *
     * <p>MES 在机续作豁免：在机续作物料不受「无计划量不排产」拦截仍会产出结果行，
     * 结果行 LH_NO 依赖示方书关系，因此即使满足零库存/零余量也必须继续校验。</p>
     *
     * @param context                  排程上下文
     * @param plan                     当前月计划
     * @param onMachineMaterialCodeSet MES在机物料编码集合
     * @return true-跳过校验；false-继续校验
     */
    private boolean shouldSkipByEmbryoStockOrSurplus(LhScheduleContext context,
                                                     FactoryMonthPlanProductionFinalResult plan,
                                                     Set<String> onMachineMaterialCodeSet) {
        String materialCode = plan.getMaterialCode();
        // MES在机续作物料豁免：仍会排产并产出结果行，示方书缺失必须阻断
        if (onMachineMaterialCodeSet.contains(materialCode)) {
            return false;
        }
        String embryoCode = plan.getEmbryoCode();
        if (targetScheduleQtyResolver.isEmbryoStockEndingFlagYes(context, embryoCode)) {
            // 胎胚收尾：胎胚实时库存为0（无库存记录默认0）不再有可硫化胎胚，跳过校验
            Integer embryoStock = context.getEmbryoRealtimeStockMap().get(embryoCode);
            if (Objects.isNull(embryoStock) || embryoStock <= 0) {
                log.info("胎胚收尾且胎胚库存为0，跳过示方书校验, 工厂: {}, 物料: {}, 胎胚: {}, 库存: {}",
                        context.getFactoryCode(), materialCode, embryoCode, embryoStock);
                return true;
            }
            return false;
        }
        // 非胎胚收尾：硫化余量为0不进入排产，跳过校验（惰性计算，仅非收尾分支才触发）
        int surplusQty = scheduleAdjustHandler.calculatePlanSurplusQty(context, plan);
        if (surplusQty <= 0) {
            log.info("非胎胚收尾且硫化余量为0，跳过示方书校验, 工厂: {}, 物料: {}, 产品状态: {}, 余量: {}",
                    context.getFactoryCode(), materialCode, plan.getProductStatus(), surplusQty);
            return true;
        }
        return false;
    }

    /**
     * 判断当前月计划在排程窗口内（仅限本月份范围）是否存在有效日计划量。
     *
     * @param context 排程上下文
     * @param plan 当前月计划
     * @return true-窗口内存在有效计划量(&gt;0)，false-窗口内全部为0或null
     */
    private boolean hasWindowPlanQty(LhScheduleContext context, FactoryMonthPlanProductionFinalResult plan) {
        Date scheduleDate = context.getScheduleDate();
        Date windowEndDate = context.getWindowEndDate();
        if (Objects.isNull(scheduleDate) || Objects.isNull(windowEndDate)
                || Objects.isNull(plan.getYear()) || Objects.isNull(plan.getMonth())) {
            return true;
        }
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(LhScheduleTimeUtil.clearTime(scheduleDate));
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(LhScheduleTimeUtil.clearTime(windowEndDate));
        int planYear = plan.getYear().intValue();
        int planMonth = plan.getMonth().intValue();
        while (!cursor.after(endCal)) {
            int year = cursor.get(Calendar.YEAR);
            int month = cursor.get(Calendar.MONTH) + 1;
            // 只检查属于本计划月份的天，跨月天数由对应月份的 plan 单独处理
            if (year == planYear && month == planMonth) {
                int dayQty = MonthPlanDayQtyUtil.resolveDayQty(plan, cursor.get(Calendar.DAY_OF_MONTH));
                if (dayQty > 0) {
                    return true;
                }
            }
            cursor.add(Calendar.DAY_OF_MONTH, 1);
        }
        return false;
    }
}
