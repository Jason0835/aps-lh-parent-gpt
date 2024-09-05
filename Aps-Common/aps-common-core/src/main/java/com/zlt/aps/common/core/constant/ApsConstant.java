package com.zlt.aps.common.core.constant;

public class ApsConstant {

    /**
     * 删除标识：正常
     */
    public static final String DEL_FLAG_NORMAL = "0";

    /**
     * 删除标识：删除
     */
    public static final String DEL_FLAG_DEL = "1";

    /**
     * 状态：启用
     */
    public static final String STATUS_ENABLE = "0";

    /**
     * 状态：禁用
     */
    public static final String STATUS_DISABLE = "1";

    /**
     * 未发布
     */
    public static final String NO_RELEASE = "0";

    /**
     * 已发布
     */
    public static final String IS_RELEASE = "1";

    /**
     * 发布失败
     */
    public static final String FAILURE_RELEASE = "2";

    /**
     * 发布中
     */
    public static final String RELEASING = "3";

    /**
     * 超时失败
     */
    public static final String TIMEOUT_FAILURE = "4";

    /**
     * 待发布
     */
    public static final String WAIT_RELEASING = "5";

    /**
     * 未投产
     */
    public static final String NO_PRODUNTION = "0";

    /**
     * 已投产
     */
    public static final String IS_PRODUNTION = "1";

    /**
     * 工序类型：硫化
     */
    public static String PROCEDURE_CODE_LH = "0";

    /**
     * 工序类型：成型
     */
    public static String PROCEDURE_CODE_CX = "1";

    /**
     * 工序类型：胎面
     */
    public static String PROCEDURE_CODE_TM = "2";

    /**
     * 工序类型：胎侧
     */
    public static String PROCEDURE_CODE_TC = "3";

    /**
     * 工序类型：内衬
     */
    public static String PROCEDURE_CODE_NC = "4";

    /**
     * 工序类型：胎圈
     */
    public static String PROCEDURE_CODE_TQ = "5";

    /**
     * 工序类型：钢丝圈
     */
    public static String PROCEDURE_CODE_GSQ = "6";

    /**
     * 工序类型：15度裁断
     */
    public static String PROCEDURE_CODE_CD15 = "7";

    /**
     * 工序类型：90度裁断
     */
    public static String PROCEDURE_CODE_CD90 = "8";


    /**
     * 工序类型：钢带压延
     */
    public static String PROCEDURE_CODE_GDYY = "9";

    /**
     * 工序类型：纤维压延
     */
    public static String PROCEDURE_CODE_XWYY = "10";

    /**
     * 系统管理模块
     */
    public static String PROCEDURE_CODE_SYSTEM = "11";

    /**
     * 胎面排程下发接口同步key
     */
    public static String TM_DEPLOY_SYNC_KEY = "TREAD_SCHE_FBK";

    /**
     * 胎侧排程下发接口同步key
     */
    public static String TC_DEPLOY_SYNC_KEY = "SIDEWALL_SCHE_FBK";

    /**
     * 内衬排程下发接口同步key
     */
    public static String NC_DEPLOY_SYNC_KEY = "LINING_SCHE_FBK";

    /**
     * 胎圈排程下发接口同步key
     */
    public static String TQ_DEPLOY_SYNC_KEY = "BEAD_SCHE_FBK";

    /**
     * 钢丝圈排程下发接口同步key
     */
    public static String GSQ_DEPLOY_SYNC_KEY = "STEEL_WIRE_SCHE_FBK";

    /**
     * 15度裁断排程下发接口同步key
     */
    public static String CD15_DEPLOY_SYNC_KEY = "ADJUDI15_SCHE_FBK";

    /**
     * 90度裁断排程下发接口同步key
     */
    public static String CD90_DEPLOY_SYNC_KEY = "ADJUDI90_SCHE_FBK";

    /**
     * 钢带压延排程下发接口同步key
     */
    public static String GDYY_DEPLOY_SYNC_KEY = "GDYY_SCHE_FBK";

    /**
     * 纤维压延排程下发接口同步key
     */
    public static String XWYY_DEPLOY_SYNC_KEY = "XWYY_SCHE_FBK";

    /**
     * 成型排程下发接口同步key
     */
    public static String CX_DEPLOY_SYNC_KEY = "FINISH_SCHE_RST_FBK";

    /**
     * 硫化排程下发接口同步key
     */
    public static String LH_DEPLOY_SYNC_KEY = "VULCANIZE_SCHE_RST_FBK";
    
    /**
     *APS模具计划下发接口
     */
    public static String APS_MOLD_PLAN_2_MES = "APS_MOLD_PLAN_2_MES";

    /**
     * 下发模具变动单接口同步key
     */
    public static String MOULD_CHANGE_FBK = "MOULD_CHANGE_FBK";

    /**
     * 请求MES系统模具变更计划同步KEY
     */
    public static String LH_MOLD_ADJUST_PLAN="LH_MOLD_ADJUST_PLAN";

    /**
     * MES监听APS发送的消息-QUEUE
     */
    public static String MES_QUEUE = "SYNC_PMBALANCE_MES_QUEUE";

    /**
     * MES监听APS发送的消息-EXCHANGE
     */
    public static String MES_EXCHANGE = "SYNC_PMBALANCE_EXCHANGE";

    /**
     * MES监听APS发送的消息-ROUTING_KEY
     */
    public static String MES_ROUTING = "SYNC_PMBALANCE_MES_ROUTING_KEY";

    /**
     * MES监听APS发送的系统编号-MES
     */
    public static String DOCK_SYS_MES = "MES";

    /**
     * 调度员操作类型：转机台
     */
    public static String DISPATCHER_OPER_MACHINE = "0";

    /**
     * 调度员操作类型：调量
     */
    public static String DISPATCHER_OPER_PLAN = "1";

    /**
     * 调度员操作类型：插单
     */
    public static String DISPATCHER_OPER_INSERT_ORDER = "2";

    /**
     * 调度员角色编码
     */
    public static String DISPATCHER_ROLE = "dispatcher";
}
