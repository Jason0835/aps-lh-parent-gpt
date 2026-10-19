package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuDailyPlanQuotaDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.enums.SkuScheduleSourceTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import com.zlt.aps.lh.util.SkuDailyPlanQuotaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 在窗口内迁移历史指定试制量试的未消费运行额度，保留原始月计划和共享总目标。
 *
 * @author APS
 * @date 2026/10/15
 */
@Slf4j
public final class PreviousAlternateTrialQuotaPolicy {

    /**
     * 禁止实例化业务额度工具。
     */
    private PreviousAlternateTrialQuotaPolicy() {
    }

    /**
     * 将较早业务日尚未消费的专用额度转入本日，不重建总目标或借用未来额度。
     * 调用方应在正式日编排中调用，指定尝试内调用时由既有快照负责失败恢复。
     *
     * @param context 当前有效历史关系及窗口
     * @param sku 原需求或与之共享日额度的已上机副本
     * @param businessDate 本次实际生产业务日
     */
    public static void prepareBusinessDate(LhScheduleContext context, SkuScheduleDTO sku,
            LocalDate businessDate) {
        if (Objects.isNull(sku) || Objects.isNull(businessDate)
                || !StringUtils.equals(SkuScheduleSourceTypeEnum.PREVIOUS_ALTERNATE_TRIAL.getCode(),
                sku.getSourceType()) || !context.hasPreviousAlternateTrialPlan(sku)
                || CollectionUtils.isEmpty(sku.getDailyPlanQuotaMap())
                || Objects.isNull(context.getScheduleDate()) || Objects.isNull(context.getWindowEndDate())) {
            return;
        }
        LocalDate firstDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate lastDate = context.getWindowEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (businessDate.isBefore(firstDate) || businessDate.isAfter(lastDate)) {
            return;
        }
        List<SkuDailyPlanQuotaDTO> previousQuotas = sku.getDailyPlanQuotaMap().entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(businessDate))
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .filter(quota -> quota.getRemainingQty() > 0)
                .collect(Collectors.toList());
        int carriedQty = previousQuotas.stream().mapToInt(SkuDailyPlanQuotaDTO::getRemainingQty).sum();
        if (carriedQty <= 0) {
            return;
        }
        SkuDailyPlanQuotaDTO currentQuota = sku.getDailyPlanQuotaMap().computeIfAbsent(businessDate, date -> {
            SkuDailyPlanQuotaDTO quota = new SkuDailyPlanQuotaDTO();
            quota.setMaterialCode(sku.getMaterialCode());
            quota.setProductionDate(date);
            return quota;
        });
        // 已消费量留在原日期，只转移剩余额度，重复编排不会再次增加本日需求。
        previousQuotas.forEach(quota -> {
            quota.setDayPlanQty(quota.getDayPlanQty() - quota.getRemainingQty());
            quota.setRemainingQty(0);
        });
        currentQuota.setDayPlanQty(currentQuota.getDayPlanQty() + carriedQty);
        currentQuota.setRemainingQty(currentQuota.getRemainingQty() + carriedQty);
        SkuDailyPlanQuotaUtil.refreshRollingFields(sku.getDailyPlanQuotaMap());
        String detail = String.format("工厂=%s, 批次=%s, 物料=%s, 状态=%s, 业务日=%s, 转入未消费额度=%s",
                context.getFactoryCode(), context.getBatchNo(), sku.getMaterialCode(), sku.getProductStatus(),
                businessDate, carriedQty);
        log.info("历史指定试制量试跨日额度迁移, {}", detail);
        PriorityTraceLogHelper.appendProcessLog(context, "历史指定试制量试跨日额度迁移", detail);
    }
}
