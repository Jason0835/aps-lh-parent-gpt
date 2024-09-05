package com.zlt.aps.lh.engine.constants;

/**
 * 硫化工序引擎常量
 */
public class LhEngineConstants {
    /**
     * 模具变动单批次号
     */
    public static final String LH_MOLD_BATCH_NO_PREFIX="MJPC";

    /**
     * 模具变动单工单号前缀
     */
    public static final String LH_MOLD_ORDER_NO_PREFIX="MJGD";

    /**
     * 硫化自动排程批次号
     */
    public static final String LH_AUTO_BATCH_NO_PREFIX="LHPC";

    /**
     *  硫化工单号前缀
     */
    public static final String LH_AUTO_ORDER_NO_PREFIX="LHGD";

    /**
     * 成型状态为：投产中
     */
    public static final String  CX_PRODUCTION_STATUS_DOING="1";

    /**
     * 成型排程为收尾状态的规格
     */
    public  static final String CX_PRODUCT_STATUS_CLOSE_OUT="2";

    /**
     * 自动排程记录成功
     */
    public  static final String LH_AUTO_RECORD_STATUS_SUCCESS="0";

    /**
     * 自动排程记录失败
     */
    public  static final String LH_AUTO_RECORD_STATUS_FAIL="1";

    /**
     * 机台状态:启用
     */
    public static final String LH_MACHINE_STATUS_ENABLE="0";

    /**
     * 硫化排程生产状态：未生产
     */
    public  static final String LH_SCHEDULE_PRODUCT_STATUS_UNDO="0";
    /**
     * 发布状态：未发布
     */
    public static final String LH_SCHEDULE_IS_RELEASE_NO="0";

    /**
     * 机台状态：启用
     */
    public static final String MACHINE_STATUS_ENABLE="0";

    /**
     * 模具更换类型为无更换模时则不进行模具变动单生成
     */
    public static final String UN_CHANGE_MOLD="0";

    /**
     * redis 前置的校验缓存
     */
    public static final String MOLD_CX_BATCH_NO_CACHE="mold:cx_batch_no:validate:";

    /**
     * aps模具计划执行状态：已执行
     */
    public static final String MOLD_EXECUTE_STATUS_YES="1";


    /**
     * 班制小时制时长
     */
    public static final Integer CLASS_SHIFT_HOUR=8;

    /**
     * 两个模常量
     */
    public static final Integer TWO_MOLD_NUMBER=2;


}
