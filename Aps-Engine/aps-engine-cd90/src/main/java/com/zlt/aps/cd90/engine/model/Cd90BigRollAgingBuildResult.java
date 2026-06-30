package com.zlt.aps.cd90.engine.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * XWYY库存成熟流水构建结果。
 */
@Data
@Builder
public class Cd90BigRollAgingBuildResult {

    /** 可参与直裁试算的实际及计划库存成熟流水。 */
    private List<Cd90BigRollAgingStock> stocks;
    /** 无法确定成熟时间的大卷编码，按规格记录DATA_MISSING。 */
    private Set<String> dataMissingBigRollCodes;
}
