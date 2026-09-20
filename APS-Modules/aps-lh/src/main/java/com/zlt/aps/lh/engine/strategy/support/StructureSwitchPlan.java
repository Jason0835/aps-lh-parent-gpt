package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import lombok.Value;
import java.io.Serializable;
import java.util.Date;

/** 结构切换只读提案；冻结来源、首检及首个批量班，试算期间不登记资源。 */
@Value
public class StructureSwitchPlan implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 时间和两个标识来自同一有效记录。 */
    CxEmbryoLhTime source;
    /** 本候选首个实际正量班次，不能用提前换模班次代替。 */
    LhShiftConfigVO firstQuantityShift;
    /** 本候选首个普通生产班P0（仅首检时取首检班）；结构S0/S1由运行态按实际正量独立维护。 */
    LhShiftConfigVO firstBatchShift;
    /** 叠加延迟前，实际计件首检可开始时间与供胚时间的较晚值。 */
    Date baseStart;
    /** 本候选正式批量生产起点。 */
    Date productionStart;
    /** 冻结首检分摊；普通场景直接引用原计划。 */
    FirstInspectionAllocationPlan inspectionPlan;
    /** 本候选是否采用含首检公式时间轴；本机后续连续生产不再追加延迟。 */
    boolean largeTimeline;
}
