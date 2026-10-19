package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import lombok.Value;
import lombok.AllArgsConstructor;
import java.io.Serializable;
import java.util.Date;

/**
 * 结构切换只读提案；规则首班、实际首产班和候选批量班分别冻结。
 *
 * @author APS
 * @date 2026/10/14
 */
@Value
@AllArgsConstructor
public class StructureSwitchPlan implements Serializable {
    /**
     * 保持已有序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 时间和两个标识来自同一有效记录。
     */
    CxEmbryoLhTime source;
    /**
     * 本候选首个实际正量班次，不能用提前换模班次代替。
     */
    LhShiftConfigVO firstQuantityShift;
    /**
     * 本候选首个普通生产班P0（仅首检时取首检班）；规则S0/S1由续作标识及供胚班判定独立维护。
     */
    LhShiftConfigVO firstBatchShift;
    /**
     * 叠加延迟前，实际计件首检可开始时间与供胚时间的较晚值。
     */
    Date baseStart;
    /**
     * 本候选正式批量生产起点。
     */
    Date productionStart;
    /**
     * 冻结首检分摊；普通场景直接引用原计划。
     */
    FirstInspectionAllocationPlan inspectionPlan;
    /**
     * 本候选是否采用含首检公式时间轴；本机后续连续生产不再追加延迟。
     */
    boolean largeTimeline;
    /**
     * 后结构续作标识及供胚班判定；为空时保留适用范围之外的原首班规则。
     */
    StructureSwitchFirstShiftDecision firstShiftDecision;

    /**
     * 保留原提案构造入口，适用范围外的调用沿用实际首产班。
     *
     * @param source 供胚来源
     * @param firstQuantityShift 实际首产班
     * @param firstBatchShift 候选批量班
     * @param baseStart 原始开产基准
     * @param productionStart 正式生产起点
     * @param inspectionPlan 首检分摊
     * @param largeTimeline 是否采用大换英寸时间轴
     */
    public StructureSwitchPlan(CxEmbryoLhTime source, LhShiftConfigVO firstQuantityShift,
            LhShiftConfigVO firstBatchShift, Date baseStart, Date productionStart,
            FirstInspectionAllocationPlan inspectionPlan, boolean largeTimeline) {
        this(source, firstQuantityShift, firstBatchShift, baseStart, productionStart,
                inspectionPlan, largeTimeline, null);
    }

    /**
     * 为原时间轴附加首班判定，不修改实际首检和生产班次。
     *
     * @param decision 只读规则首班判定
     * @return 含判定的提案副本
     */
    public StructureSwitchPlan withFirstShiftDecision(StructureSwitchFirstShiftDecision decision) {
        return new StructureSwitchPlan(source, firstQuantityShift, firstBatchShift, baseStart,
                productionStart, inspectionPlan, largeTimeline, decision);
    }
}
