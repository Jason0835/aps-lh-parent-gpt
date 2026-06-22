package com.zlt.aps.tq.service;

/**
 * 胎圈排程预警Service接口
 *
 * <p>预警类型：</p>
 * <ul>
 *   <li>库存预警：胎圈库存低于安全库存时触发</li>
 *   <li>完成量预警：班次结束后，实际完成量低于计划量一定比例时触发</li>
 * </ul>
 *
 * @author APS
 */
public interface ITqWarningService {

    /**
     * 执行库存预警检查
     *
     * <p>扫描当前库存，对比安全库存，低于阈值则发送预警消息。</p>
     */
    void checkStockWarning();

    /**
     * 执行班次完成量预警检查
     *
     * <p>在班次完全结束后检查，对比计划量与实际完成量，差异超过阈值则发送预警。</p>
     *
     * @param scheduleDate 排程日期
     * @param shiftIndex   班次索引（1~6）
     */
    void checkFinishQtyWarning(java.util.Date scheduleDate, int shiftIndex);
}
