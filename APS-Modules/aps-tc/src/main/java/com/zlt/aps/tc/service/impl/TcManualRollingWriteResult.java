package com.zlt.aps.tc.service.impl;

import lombok.Data;

/**
 * 胎侧人工滚动一次性持久化统计。
 */
@Data
public class TcManualRollingWriteResult {
    /** 新增行数。 */
    private int insertCount;
    /** 更新行数。 */
    private int updateCount;
    /** 逻辑删除行数。 */
    private int deleteCount;
    /** 新增未排任务数量。 */
    private int unplannedCount;
}
