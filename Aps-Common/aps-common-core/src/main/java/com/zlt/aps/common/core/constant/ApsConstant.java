package com.zlt.aps.common.core.constant;

import java.util.HashMap;
import java.util.Map;

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
     * 四个班次
     */
    public static final Integer FOUR_CLASS = 4;

    /**
     * 六个班次
     */
    public static final Integer SIX_CLASS = 6;

    /**
     * 单模
     */
    public static final Integer SINGLE_MOLD = 1;

    /**
     * 双模
     */
    public static final Integer DOUBLE_MOLD = 2;

    /**
     * 2天
     */
    public static final Integer TWO_DAY = 2;

    /**
     * 1天
     */
    public static final Integer ONE_DAY = 1;

    /**
     * 左模
     */
    public static final String L_MOLD = "L";
    /**
     * 右模
     */
    public static final String R_MOLD = "R";

    /**
     * 分隔符号
     */
    public static final String SPLIT_CHAR = "#@%#";

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
     * 工序类型：硫化（工序值0已被密炼占用）
     */
    public static String PROCEDURE_CODE_LH = "11";

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
     * APS模具计划下发接口
     */
    public static String APS_MOLD_PLAN_2_MES = "APS_MOLD_PLAN_2_MES";

    /**
     * 下发模具变动单接口同步key
     */
    public static String MOULD_CHANGE_FBK = "MOULD_CHANGE_FBK";

    /**
     * 请求MES系统模具变更计划同步KEY
     */
    public static String LH_MOLD_ADJUST_PLAN = "LH_MOLD_ADJUST_PLAN";

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
     * 供应链已计划未发货数据同步key
     */
    public static final String SYNC_PLANED_NOT_SHIP = "SYNC_PLANED_NOT_SHIP";

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

    /**
     * 接口token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer:";

    /**
     * 是
     */
    public static final String TRUE = "1";

    /**
     * 否
     */
    public static final String FALSE = "0";

    /**
     * 是否 为否
     */
    public static final Integer APS_YES_NO_0 = 0;
    /**
     * 是否 为是
     */
    public static final Integer APS_YES_NO_1 = 1;

    /**
     * sync服务同步状态：异常
     */
    public static final Integer SYNC_STATUS_3 = 3;

    /**
     * sync服务同步状态：成功
     */
    public static final Integer SYNC_STATUS_6 = 6;

    /**
     * 升序
     */
    public static final Integer SORT_ASC = 1;

    /**
     * 降序
     */
    public static final Integer SORT_DESC = 2;

    /**
     * 常用字符数字
     */
    public static final String APS_STRING_0 = "0";
    public static final String APS_STRING_1 = "1";
    public static final String APS_STRING_2 = "2";
    public static final String APS_STRING_3 = "3";
    public static final String APS_STRING_36 = "36";
    public static final String APS_STRING_37 = "37";
    public static final String APS_STRING_4 = "4";
    public static final String APS_STRING_5 = "5";
    public static final String APS_STRING_6 = "6";
    public static final String APS_STRING_10 = "10";
    public static final String APS_STRING_11 = "11";
    public static final String APS_STRING_20 = "20";
    public static final String APS_STRING_30 = "30";
    public static final String VN_TAX_CODE = "CN8";

    /**
     * 常用带零字符数字
     */
    public static final String APS_ZERO_1 = "01";
    public static final String APS_ZERO_2 = "02";
    public static final String APS_ZERO_3 = "03";
    public static final String APS_ZERO_4 = "04";
    public static final String APS_ZERO_5 = "05";
    public static final String APS_ZERO_6 = "06";

    public static final String APS_ZERO_00 = "00";

    public static final String APS_ZERO_01 = "01";

    public static final String PLAN_DELAY_AUTO_SUPPLE = "延误增补";

    public static final String CHANGE_MOULD_LIMIT = "超出换模次数限制";

    /**
     * POST请求
     */
    public static final String HTTP_POST = "POST";

    /**
     * 26个字母表
     */
    public static final String ALPHABET = "A,B,C,D,E,F,G,H,I,J,K,L,M,N,O,P,Q,R,S,T,U,V,W,X,Y,Z";
    /**
     * 返回成功
     **/
    public static final String SUCCESS = "success";

    /**
     * 返回成功
     **/
    public static final String FAIL = "fail";

    /**
     * DC-SCM桶
     **/
    public static final String DC_APS_BUCKET = "upload-file";

    public static final Long EXPIRE_ONE = 1L;

    /**
     * 销售优先级：1-高优先级；
     */
    public static final String SAL_PRIORITY_HIGHT = "1";
    /**
     * 供应链订单类型：2-高优先级；
     */
    public static final String SAL_PRIORITY_CYCLE_STOCK_UP = "2";

    /**
     * 销售优先级：3-中优先级；
     */
    public static final String SAL_PRIORITY_MID = "3";
    /**
     * 供应链订单类型：4-常规储备；
     */
    public static final String SAL_PRIORITY_PRECEDENT_STOCK_UP = "4";
    /**
     *  销售优先级：5-暂缓订单
     */
    public static final String SAL_PRIORITY_POSTPONE = "5";

    /**
     * 发货模式：02-整单发货
     */
    public static final String DELIVERY_MODE_ALL = "02";

    /**
     * 发货模式：01-分批发货
     */
    public static final String DELIVERY_MODE_SPLIT = "01";

    /**
     * SCM发货模式：10-整单发货
     */
    public static final String SCM_DELIVERY_MODE_ALL = "10";

    /**
     * SCM发货模式：20-分批发货
     */
    public static final String SCM_DELIVERY_MODE_SPLIT = "20";

    /**
     * 每日分钟数
     */
    public static final int MINUTES_PER_DAY = 24 * 60;

    /**
     * 每日秒数
     */
    public static final int SECOND_PER_DAY = 24 * 60 * 60;

    /**
     * 硫化排程管理，自动排程
     */
    public static final String REDIS_APS_LH_AUTO_SCHEDULE = "APS:LH:AUTO:SCHEDULE:";


    // 月份与后缀的映射关系
    public static final Map<Integer, String> MONTH_SUFFIX_MAP = new HashMap<>();

    /**
     * 日志分割符
     */
    public static final String DIVISION = "\r\n---------------------------------------------------\r\n";

    static {
        MONTH_SUFFIX_MAP.put(1, "1");
        MONTH_SUFFIX_MAP.put(2, "2");
        MONTH_SUFFIX_MAP.put(3, "3");
        MONTH_SUFFIX_MAP.put(4, "4");
        MONTH_SUFFIX_MAP.put(5, "5");
        MONTH_SUFFIX_MAP.put(6, "6");
        MONTH_SUFFIX_MAP.put(7, "7");
        MONTH_SUFFIX_MAP.put(8, "8");
        MONTH_SUFFIX_MAP.put(9, "9");
        MONTH_SUFFIX_MAP.put(10, "10");
        MONTH_SUFFIX_MAP.put(11, "11");
        MONTH_SUFFIX_MAP.put(12, "12");
    }

    // 库位类型与字段名称的映射关系
    public static final Map<Integer, String> LOCATION_TYPE_FIELD_MAP = new HashMap<>();

    static {
        LOCATION_TYPE_FIELD_MAP.put(1, "domestic");
        LOCATION_TYPE_FIELD_MAP.put(2, "foreign");
        LOCATION_TYPE_FIELD_MAP.put(3, "oe");
    }

    /**
     * 生产顺序标识
     */
    public static final Integer PRODUCT_ORDER_FLAG = 1;


    /**
     * 结构调整-自动调整
     */
    public static final String REDIS_ADJUST_STRUCT_AUTO = "APS:STRUCT:AUTO:";

}
