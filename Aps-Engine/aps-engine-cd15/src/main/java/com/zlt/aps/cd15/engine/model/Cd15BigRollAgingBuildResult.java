package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * GDYY库存成熟流水构建结果。
 */
@Data
@Builder
public class Cd15BigRollAgingBuildResult {

    /** 可参与斜裁试算的实际及计划库存成熟流水。 */
    private List<Cd15BigRollAgingStock> stocks;
    /** 无法确定成熟时间的大卷代码，按规格记录DATA_MISSING。 */
    private Set<String> dataMissingBigRollCodes;
}