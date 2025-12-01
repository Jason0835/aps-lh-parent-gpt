package com.zlt.aps.constants;

/**
 * 硫化工序前缀相关常量
 */
public class LhPrefixConstants {
    /**
     * 自动排程抓取redis key 前缀
     */
    public static final String AUTO_SCHEDULE_PREFIX="auto:schedule:lh:";

    /**
     * 排程抓取记录批次号前缀
     */
    public static final String SCHEDULE_BATCH_NO_PREFIX="schedule:lh:batch:no:";

    /**
     * 硫化工单号生成
     */
    public static final String SCHEDULE_ORDER_NO_PREFIX="schedule:lh:order:no:";

    /**
     * 硫化工单号前缀
     */
    public static final String LH_ORDER_NO_PREFIX="LHGD";

    /**
     * 硫化批次号前缀
     */
    public static final String LH_BATCH_NO_PREFIX="LHPC";
}
