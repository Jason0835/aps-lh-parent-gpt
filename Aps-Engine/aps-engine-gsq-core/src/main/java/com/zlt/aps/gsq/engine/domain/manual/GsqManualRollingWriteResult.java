package com.zlt.aps.gsq.engine.domain.manual;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 钢丝圈人工滚动写入统计。
 */
@Data
public class GsqManualRollingWriteResult {

    /** 新增行数 */
    private int insertCount;

    /** 更新行数 */
    private int updateCount;

    /** 删除行数 */
    private int deleteCount;

    /** 未排记录数 */
    private int unplannedCount;

    /** 未排总量 */
    private BigDecimal unplannedQty = BigDecimal.ZERO;
}
