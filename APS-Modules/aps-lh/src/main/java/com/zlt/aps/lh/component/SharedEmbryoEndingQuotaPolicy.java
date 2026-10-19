package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.PriorityTraceLogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 独立共用胎胚份额的准入、去重、首次冻结和偏差记录。
 * 仅维护身份及初始份额，实际扣减、恢复及副本同步仍由统一数量解析器完成。
 *
 * @author APS
 * @date 2026/10/19
 */
@Slf4j
@Component
public class SharedEmbryoEndingQuotaPolicy {
    /**
     * 两层分配共用的无状态算法。
     */
    private final SharedEmbryoEndingAllocationService sharedEmbryoEndingAllocationService;

    /**
     * 兼容既有不经容器的直接构造入口。
     */
    public SharedEmbryoEndingQuotaPolicy() {
        this(new SharedEmbryoEndingAllocationService());
    }

    /**
     * 注入共享分配算法，不依赖目标量解析器以避免反向循环。
     *
     * @param sharedEmbryoEndingAllocationService 无状态分配算法
     */
    @Autowired
    public SharedEmbryoEndingQuotaPolicy(SharedEmbryoEndingAllocationService sharedEmbryoEndingAllocationService) {
        this.sharedEmbryoEndingAllocationService = sharedEmbryoEndingAllocationService;
    }

    /**
     * 判断本轮是否已冻结同胎胚份额，候选退出不撤销该事实。
     *
     * @param context 排程上下文
     * @param embryoCode 胎胚编码
     * @return 已存在冻结身份时返回true
     */
    public boolean hasFrozenGroup(LhScheduleContext context, String embryoCode) {
        return context.getSharedEmbryoStockEndingIdentityMap().containsValue(embryoCode);
    }

    /**
     * 判断运行态共用组是否进入独立收尾，不增加SKU标签或T日条件。
     *
     * @param context 排程上下文
     * @param embryoCode 胎胚编码
     * @param shared 已由统一使用关系确认的共用事实
     * @return 共用且胎胚收尾标识为1时返回true
     */
    public boolean isEligible(LhScheduleContext context, String embryoCode, boolean shared) {
        return shared && this.isEmbryoStockEndingFlagYes(context, embryoCode);
    }

    /**
     * 读取本轮首次份额，调用方先确认独立身份，禁止使用候选变化重算额度。
     *
     * @param context 排程上下文
     * @param sku 当前来源或副本
     * @return 已冻结份额，未登记时返回空
     */
    public Integer frozenQuota(LhScheduleContext context, SkuScheduleDTO sku) {
        return context.getSharedEmbryoStockEndingInitialQuotaMap().get(this.buildSkuKey(sku));
    }

    /**
     * 判断当前物料状态是否已经取得本轮共用胎胚收尾身份。
     *
     * @param context 本轮冻结身份和数量账本
     * @param sku 当前来源或补偿副本
     * @return 已成立且胎胚代码相符时返回true
     */
    public boolean isSharedEmbryoStockEnding(LhScheduleContext context, SkuScheduleDTO sku) {
        return Objects.nonNull(context) && Objects.nonNull(sku) && StringUtils.isNotBlank(sku.getEmbryoCode())
                && StringUtils.equals(sku.getEmbryoCode(),
                context.getSharedEmbryoStockEndingIdentityMap().get(this.buildSkuKey(sku)));
    }

    /**
     * 判断胎胚是否配置为胎胚库存收尾。
     * <p>供收尾目标量门控与共用胎胚零余量预剔除生产者候选判定复用。</p>
     *
     * @param context 排程上下文
     * @param embryoCode 胎胚代码
     * @return true-胎胚库存收尾；false-普通胎胚
     */
    public boolean isEmbryoStockEndingFlagYes(LhScheduleContext context, String embryoCode) {
        if (Objects.isNull(context) || StringUtils.isEmpty(embryoCode)
                || CollectionUtils.isEmpty(context.getEmbryoEndingFlagMap())) {
            return false;
        }
        return 1 == context.getEmbryoEndingFlagMap().getOrDefault(embryoCode, 0);
    }

    /**
     * 按物料及产品状态去重收集有效SKU，避免同SKU多机台重复分摊。
     *
     * @param context 当前有效候选及来源集合
     * @param embryoCode 待分摊胎胚代码
     * @param candidates 本轮统一来源候选
     * @return 当前有效且去重的SKU列表
     */
    public List<SkuScheduleDTO> collectActiveSkusByEmbryo(LhScheduleContext context, String embryoCode, List<SkuScheduleDTO> candidates) {
        List<String> activeSkuKeyList = context.getActiveEmbryoSkuMap().get(embryoCode);
        if (CollectionUtils.isEmpty(activeSkuKeyList)) {
            return new ArrayList<SkuScheduleDTO>(0);
        }
        Set<String> activeSkuKeySet = new HashSet<String>(activeSkuKeyList);
        List<SkuScheduleDTO> activeSkuList = new ArrayList<SkuScheduleDTO>(activeSkuKeyList.size());
        Set<String> collectedKeys = new HashSet<>();
        for (SkuScheduleDTO sku : candidates) {
            if (Objects.nonNull(sku) && activeSkuKeySet.contains(this.buildSkuKey(sku))
                    && StringUtils.equals(embryoCode, sku.getEmbryoCode())
                    && collectedKeys.add(this.buildSkuKey(sku))) {
                activeSkuList.add(sku);
            }
        }
        return activeSkuList;
    }

    /**
     * 按原始余量确定本轮唯一份额，记录理论偏差，不按可行性或候选退出跨SKU转量。
     *
     * @param context 本轮独立身份及账本
     * @param embryoCode 胎胚编码
     * @param rawEmbryoStock 原始库存
     * @param skuList 按物料状态去重的SKU
     * @param reason 分配原因
     */
    public void freeze(LhScheduleContext context, String embryoCode,
            int rawEmbryoStock, List<SkuScheduleDTO> skuList, String reason) {
        if (this.hasFrozenGroup(context, embryoCode)) {
            return;
        }
        if (skuList.size() == 1 && skuList.get(0).getSurplusQty() <= 0
                && context.getDynamicSingleEmbryoEndingMaterialSet().contains(skuList.get(0).getMaterialCode())) {
            // 沿用既有全零组唯一续作承接，不新增比例或均分兜底。
            String skuKey = this.buildSkuKey(skuList.get(0));
            context.getSharedEmbryoStockEndingIdentityMap().put(skuKey, embryoCode);
            context.getSharedEmbryoStockEndingInitialQuotaMap().put(skuKey, Math.max(0, rawEmbryoStock));
            return;
        }
        Map<String, Integer> allocated = sharedEmbryoEndingAllocationService.allocateSkus(rawEmbryoStock, skuList);
        if (allocated.isEmpty()) {
            // 全零组仍交给既有唯一续作承接流程，不引入平均分配。
            log.info("独立胎胚分摊等待全零余量承接规则, factoryCode: {}, batchNo: {}, embryoCode: {}, reason: {}",
                    context.getFactoryCode(), context.getBatchNo(), embryoCode, reason);
            return;
        }
        long totalSurplus = skuList.stream().mapToLong(sku -> Math.max(0, sku.getSurplusQty())).sum();
        Map<String, SkuScheduleDTO> sources = skuList.stream().collect(Collectors.toMap(
                this::buildSkuKey, sku -> sku, (first, second) -> first));
        allocated.forEach((skuKey, quota) -> {
            context.getSharedEmbryoStockEndingIdentityMap().put(skuKey, embryoCode);
            context.getSharedEmbryoStockEndingInitialQuotaMap().put(skuKey, quota);
            double theoretical = (double) Math.max(0, rawEmbryoStock)
                    * Math.max(0, sources.get(skuKey).getSurplusQty()) / totalSurplus;
            String detail = String.format("工厂=%s, 日期=%s, 批次=%s, 胎胚=%s, SKU=%s, 原始余量=%s, 库存=%s, "
                            + "理论量=%.6f, 分摊量=%s, 偏差=%.6f, 偏差超过2=%s, 原因=%s",
                    context.getFactoryCode(), context.getScheduleDate(), context.getBatchNo(), embryoCode, skuKey,
                    sources.get(skuKey).getSurplusQty(), rawEmbryoStock, theoretical, quota, quota - theoretical,
                    Math.abs(quota - theoretical) > 2, reason);
            PriorityTraceLogHelper.appendProcessLog(context, "共用胎胚余量占比分摊", detail);
            log.info("共用胎胚余量占比分摊, {}", detail);
        });
    }

    /**
     * 以统一物料及产品状态口径构造身份键。
     *
     * @param sku 当前SKU
     * @return 去除状态两端空白后的复合键
     */
    private String buildSkuKey(SkuScheduleDTO sku) {
        return MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
    }
}
