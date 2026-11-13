package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 等结果结构切换第二班选料策略，按已提交首台的SKU及胎胚关系稳定分组。
 * 每轮只读取当前来源运行态，供编排在机台组合等级之前消费关系组。
 *
 * @author APS
 * @date 2026/11/12
 */
public final class StructureSwitchSecondShiftSelectionPolicy {

    /**
     * 禁止创建无状态选料策略实例。
     */
    private StructureSwitchSecondShiftSelectionPolicy() {
    }

    /**
     * 将等结果第二班候选按同SKU、同胎胚、不同胎胚分组，组内保留原业务顺序。
     * 非适用场景返回单组；调用方依次完整尝试各组，合法候选仍需通过全部资源约束。
     *
     * @param context 当前排程上下文及已提交首台状态
     * @param source 当前结构冻结的供胚来源
     * @param shift 当前竞争的真实班次
     * @param candidates 同结构、同来源且已有业务顺序的非空元素列表
     * @return 按关系优先级排列的候选组，空候选返回空列表
     */
    public static List<List<SkuScheduleDTO>> groupCandidates(LhScheduleContext context,
            CxEmbryoLhTime source, LhShiftConfigVO shift, List<SkuScheduleDTO> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        if (StructureSwitchSchedulingPolicy.expansionRank(context, candidates.get(0), shift, source) < 0) {
            return Collections.singletonList(candidates);
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(source.getNextStructureName());
        if (Objects.isNull(state) || !Objects.equals(source.getId(), state.getSourceId())) {
            return Collections.singletonList(candidates);
        }
        // 关系只有三个等级，单次遍历稳定分组；首台参照在本轮固定，下一轮重新读取提交或回滚后的状态。
        Map<Integer, List<SkuScheduleDTO>> groups = candidates.stream().collect(Collectors.groupingBy(
                sku -> StructureSwitchSchedulingPolicy.skuRelationRank(state, sku),
                TreeMap::new, Collectors.toList()));
        return new ArrayList<>(groups.values());
    }
}
