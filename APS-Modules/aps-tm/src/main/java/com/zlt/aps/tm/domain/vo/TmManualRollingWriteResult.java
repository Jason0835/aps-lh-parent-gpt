package com.zlt.aps.tm.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 胎面人工操作滚动写入结果。
 *
 * <p>用于承载一次局部滚动后新增、更新、未排等写库摘要，避免业务服务使用内部类。</p>
 */
@Data
public class TmManualRollingWriteResult {

    /** 新增排程结果数量 */
    private int insertCount;

    /** 更新排程结果数量 */
    private int updateCount;

    /** 逻辑删除排程结果数量 */
    private int deleteCount;

    /** 写入未排数量 */
    private int unplannedCount;

    /** 未排计划量合计 */
    private BigDecimal unplannedQty = BigDecimal.ZERO;

    /**
     * 累加另一次滚动写入结果。
     *
     * @param other 其他写入结果
     */
    public void add(TmManualRollingWriteResult other) {
        if (other == null) {
            return;
        }
        this.insertCount += other.getInsertCount();
        this.updateCount += other.getUpdateCount();
        this.deleteCount += other.getDeleteCount();
        this.unplannedCount += other.getUnplannedCount();
        this.unplannedQty = this.unplannedQty.add(other.getUnplannedQty());
    }
}
