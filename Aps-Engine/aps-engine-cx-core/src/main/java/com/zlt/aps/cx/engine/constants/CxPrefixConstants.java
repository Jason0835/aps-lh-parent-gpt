package com.zlt.aps.cx.engine.constants;

/**
 * 成型工序前缀相关常量
 */
public class CxPrefixConstants {
    /**
     * 自动排程抓取redis key 前缀
     */
    public static final String AUTO_SCHEDULE_PREFIX="auto:schedule:cx:";

    /**
     * 排程抓取记录批次号前缀
     */
    public static final String SCHEDULE_BATCH_NO_PREFIX="schedule:cx:batch:no:";

    /**
     * 成型工单号生成
     */
    public static final String SCHEDULE_ORDER_NO_PREFIX="schedule:cx:order:no:";

    /**
     * 生产排程APS对应月度计划版本的版本号前缀
     */
    public static final String SCHEDULE_APS_VERSION_PREFIX="schedule:cx:aps:version:";

    /**
     * 成型工单号前缀
     */
    public static final String CX_ORDER_NO_PREFIX="CXGD";

    /**
     * 成型批次号前缀
     */
    public static final String CX_BATCH_NO_PREFIX="CXPC";

    /**
     * 插单前缀
     */
    public static final String CX_INSERT_VALIDATE_PREFIX="schedule:cx:insert:validate:";

    /**
     * 转机台验证前缀
     */
    public static final String CX_CHANGE_MACHINE_PREFIX="schedule:change:machine:validate:";

    /**
     * 投产验证前缀
     */
    public static final String CX_PRODUCT_PLAN_PREFIX="schedule:product:plan:validate:";

    /**
     * 投产验证前缀
     */
    public static final String CX_RE_PRODUCT_PLAN_PREFIX="schedule:reProduct:plan:validate:";

    /**
     * 成型增补计划批次号前缀
     */
    public static final String SUPPLE_BATCH_NO_PREFIX="supple:batch:no:";

    /**
     *增补计划批次号批次号前缀
     */
    public static final String SUPPLE_BATCH_PREFIX="ZBPC";

}
