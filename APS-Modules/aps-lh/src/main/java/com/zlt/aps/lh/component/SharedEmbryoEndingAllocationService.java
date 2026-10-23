package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 共用胎胚SKU库存份额的纯计算，只确定物料状态额度，不生成逐机计划或释放。
 *
 * @author APS
 * @date 2026/10/19
 */
@Component
public class SharedEmbryoEndingAllocationService {

    /**
     * 按原始硫化余量稳定排序，前序向下取偶，末项接收全部尾差。
     * 全零余量由既有唯一续作承接规则处理，本方法不创建平均分配兜底。
     *
     * @param stock 原始胎胚库存
     * @param skus 已按物料及状态去重的有效SKU
     * @return 按分摊顺序保存的SKU额度，余量合计为零时为空
     */
    public Map<String, Integer> allocateSkus(int stock, List<SkuScheduleDTO> skus) {
        List<SkuScheduleDTO> ordered = new ArrayList<>(skus);
        ordered.sort(Comparator.comparingInt((SkuScheduleDTO sku) -> Math.max(0, sku.getSurplusQty()))
                .thenComparing(SkuScheduleDTO::getMaterialCode)
                .thenComparing(sku -> sku.getProductStatus() == null ? "" : sku.getProductStatus()));
        long totalSurplus = ordered.stream().mapToLong(sku -> Math.max(0, sku.getSurplusQty())).sum();
        Map<String, Integer> quotas = new LinkedHashMap<>(ordered.size());
        if (totalSurplus == 0) {
            return quotas;
        }
        int budget = Math.max(0, stock);
        int allocated = 0;
        for (int index = 0; index < ordered.size(); index++) {
            SkuScheduleDTO sku = ordered.get(index);
            int quantity = index == ordered.size() - 1 ? budget - allocated
                    : (int) ((long) budget * Math.max(0, sku.getSurplusQty()) / totalSurplus / 2 * 2);
            quotas.put(MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()), quantity);
            allocated += quantity;
        }
        return quotas;
    }

}
