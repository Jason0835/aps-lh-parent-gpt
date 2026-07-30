package com.zlt.aps.gsq.constant;

/**
 * 钢丝圈排程共享常量
 *
 * <p>集中管理钢丝圈自动滚动更新、班次配置等场景使用的常量，
 * 避免在多个 Service / Controller / Job 类中重复硬编码。</p>
 *
 * @author APS
 */
public final class GsqScheduleConstants {

    // ==================== 班次相关 ====================

    /** 钢丝圈宽表支持的最大班次序号（6班次制） */
    public static final int GSQ_MAX_SHIFT_ORDER = 6;

    // ==================== 任务编号前缀 ====================

    /** 自动滚动任务编号前缀 */
    public static final String ROLLING_TASK_ID_PREFIX = "GSQ-ROLL-";

    // ==================== 任务阶段编码 ====================

    /** 自动滚动计算阶段编码 */
    public static final String ROLLING_STAGE_CALCULATING = "ROLLING_CALCULATING";

    /** 自动滚动持久化阶段编码 */
    public static final String ROLLING_STAGE_PERSISTING = "ROLLING_PERSISTING";

    /** 自动滚动完成阶段编码 */
    public static final String ROLLING_STAGE_COMPLETE = "COMPLETE";

    // ==================== 任务创建者标识 ====================

    /** 自动滚动任务创建者 */
    public static final String AUTO_ROLLING_OPERATOR = "AUTO_ROLLING";

    // ==================== 错误摘要长度 ====================

    /** 错误摘要最大长度（数据库字段长度限制） */
    public static final int MAX_ERROR_MESSAGE_LENGTH = 2000;

    // ==================== 班次业务日期偏移 ====================

    /**
     * class1至class6相对排程日期（SCHEDULE_DATE，即D+1）的MES业务日偏移。
     *
     * <p>6班次制对应关系：</p>
     * <ul>
     *   <li>1班：D日 中班（offset = -1）</li>
     *   <li>2班：D+1日 夜班（offset = 0）</li>
     *   <li>3班：D+1日 早班（offset = 0）</li>
     *   <li>4班：D+1日 中班（offset = 0）</li>
     *   <li>5班：D+2日 夜班（offset = 1）</li>
     *   <li>6班：D+2日 早班（offset = 1）</li>
     * </ul>
     */
    public static final int[] MES_BUSINESS_DATE_OFFSETS = {-1, 0, 0, 0, 1, 1};

    /**
     * 工具类不允许实例化。
     */
    private GsqScheduleConstants() {
    }
}
