package com.zlt.aps.cx.engine.constants;

import java.math.BigDecimal;

/**
  * 成型工序计算常量类
  * @ClassName CxEngineConstants
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/23 16:53
  * @Version 1.0
**/
public class CxEngineConstants {


    /**
     * 机台类型：一次法
     */
    public static final String MACHINE_TYPE_ONCE="1";


    /**
     * 机台类型：二次法
     */
    public static final String MACHINE_TYPE_TWICE="2";

    /**
     * 任务类型：待投产
     */
    public static final String TASK_TYPE_TODO="1";

    /**
     * 任务类型：待换模
     */
    public static final String TASK_TYPE_CHANGE_MOLD="2";

    /**
     * 任务类型：投产中
     */
    public static final String TASK_TYPE_DOING="3";

    /**
     * 任务类型：已收尾
     */
    public static final String TASK_CLOSE_OUT="4";

    /**
     * 任务类型：已收尾欠产
     */
    public static final String TASK_CLOSE_OUT_DELIN="5";

    /**
     * 生产状态：未生产
     */
    public static final String PRODUCTION_STATUS_UNDO="0";

    /**
     * 生产状态：生产中
     */
    public static final String PRODUCTION_STATUS_DOING="1";

    /**
     * 生产状态：已收尾
     */
    public static final String PRODUCTION_STATUS_CLOSE_OUT="2";

    /**
     * 提示收尾标识：是
     */
    public static final String CLOSE_OUT_TIP_YES="0";

    /**
     * 提示收尾标识：否
     */
    public static final String CLOSE_OUT_TIP_NO="1";



    /**
     * 一分钟60秒
     */
    public static final Integer ONE_MINUTE_SECOND=60;

    /**
     *  常量0
     */
    public static final Double ZERO=Double.valueOf(0);
    /**
     * 保留两位小数
     */
    public static final Integer TWO_SCALE=2;


    /**
     * 可排的最大班次数
     */
    public static final Integer TASK_MAX_CLASS_SHIFT=5;

    /**
     * 月度计划明细投产状态：未投产
     */
    public static final String MDM_PLAN_PRODUCT_STATUS_NO="0";

    /**
     * 月度计划明细投产状态：已投产
     */
    public static final String MDM_PLAN_PRODUCT_STATUS_YES="1";

    /**
     * 月度计划明细投产状态：待发布状态（发布后变更为已投产）
     */
    public static final String MDM_PLAN_PRODUCT_STATUS_WAIT="2";

    /**
     * 限制作业
     */
    public static final String SPECIFY_JOB_TYPE_YES="0";
    /**
     * 不可作业
     */
    public static final String SPECIFY_JOB_TYPE_NO="1";

    /**
     * 自动排程过期时间5分钟
     */
    public static final Long AUTO_SCHEDULE_KEY_TIME=5L;

    /**
     * 自动排程状态：成功
     */
    public static final String AUTO_SCHEDULE_STATUS_SUCCESS="0";

    /**
     * 自动排程状态：失败
     */
    public static final String AUTO_SCHEDULE_STATUS_FAILE="1";

    /**
     * 发布状态：是
     */
    public static final String IS_PUBLISH_YES="1";

    /**
     * 发布状态：否
     */
    public static final String IS_PUBLISH_NO="0";

    /**
     * 成型外胎月度计划量汇总表：主计划
     */
    public static final String CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_MPS="0";

    /**
     * 成型外胎月度计划量汇总表：插单
     */
    public static final String CX_MONTH_PLAN_SURPLUS_DATA_SOURCE_INSERT="1";


    /**
     * 成型排程结果表：自动排程
     */
    public static final String CX_SCHEDULE_DATA_SOURCE_AUTO="0";

    /**
     * 成型排程结果表：插单
     */
    public static final String CX_SCHEDULE_DATA_SOURCE_INSERT="1";

    /**
     * 成型排程结果表：导入
     */
    public static final String CX_SCHEDULE_DATA_SOURCE_IMPORT="2";

    /**
     * 标记不投产：是
     */
    public static final String MARK_UN_PRODUCT_YES="1";

    /**
     * 标记不投产：否
     */
    public static final String MARK_UN_PRODUCT_NO="0";

    /**
     * 自动排程没有月度剩余量key
     */
    public static final String AUTO_OUTOVER_REMAIN_MONTH_QTY="remainMonthQty";

    /**
     * 自动排程没有任务剩余量key
     */
    public static final String AUTO_OUTOVER_TASK_QTY="remainTaskQty";

    /**
     * 自动排程班次没有剩余时间key
     */
    public static final String AUTO_OUTOVER_REMAIN_TIME="remainTime";

    /**
     * 是否投产：是
     */
    public static final String TO_PRODUCT_YES="0";

    /**
     * 成型排程是否投产：否
     */
    public static final String TO_PRODUCT_NO="1";

    /**
     * 获取当前班次类型
     */
    public static final String CURRENT_SHIFT_TYPE="0";

    /**
     * 获取前班次类型
     */
    public static final String BEFORE_SHIFT_TYPE="-1";

    /**
     * 最小硫化机比对数为0.1
     */
    public static final BigDecimal MIN_LH_MACHINE_COM_QTY=BigDecimal.valueOf(0.1);

    /**
     * 类型：库存上限
     */
    public static final String STOCK_UP_LIMIT="1";

    /**
     * 类型：库存上限预警值
     */
    public static final String STOCK_UP_LIMIT_WARNING="2";

    /**
     * 类型：库存下限
     */
    public static final String STOCK_BOTTOM_LIMIT="3";

    /**
     * 类型：库存下限预警值
     */
    public static final String STOCK_BOTTOM_LIMIT_WARNING="4";

    /**
     * 默认补产中夜班差异量默认值
     */
    public static final Integer DEFAULT_FINISH_PLAN_DIFF_CONDITION=20;

    /**
     * 班次时长常量为8小时
     */
    public static final Double CLASS_SHIFT_HOUR=BigDecimal.valueOf(8).doubleValue();

    /**
     * 增补批次状态为未确认
     */
    public static final String SUPPLE_BATCH_STATUS_NO="0";

    /**
     * 增补批次状态为已确认
     */
    public static final String SUPPLE_BATCH_STATUS_YES="1";

    /**
     * 成型单机台排程最大循环次数，出现异常死循环的话，通过这个count来控制强制退出
     */
    public static final Integer AUTO_SCHEDULE_MAX_ROOP_COUNT=30;

    public static final String YES="Y";

    public static final String NO="N";

    /**
     * 默认投产日期最大的天数
     */
    public static final Integer DEFAULT_MAX_STEP=3;

    /**
     * 数据来源：成型排程
     */
    public static final String CHANGE_MACHINE_DATA_SOURCE_SCHEDULE="0";

    /**
     * 数据来源：增补计划
     */
    public static final String CHANGE_MACHINE_DATA_SOURCE_SUPPLE="1";

}
