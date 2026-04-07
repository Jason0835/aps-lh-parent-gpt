package com.zlt.mix.common.core.constant;

public class ZltConstant {

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
     * 唯一
     */
    public final static String UNIQUE = "0";

    /**
     * 不唯一
     */
    public final static String NOT_UNIQUE = "1";

    /**
     * 模块code：密炼排程
     */
    public final static String PROCEDURE_CODE_MIX = "1";

    /**
     * 模块code：排程设置
     */
    public final static String PROCEDURE_CODE_SETTING = "2";

    /**
     * 模块code：硫磺辅料排程设置
     */
    public final static String PROCEDURE_CODE_FL_SETTING = "3";

    /**
     * 模块code：硫化排程
     */
    public final static String PROCEDURE_CODE_LH = "5";

    /**
     * 模块code：月度计划
     */
    public final static String PROCEDURE_CODE_MONTHPLAN = "4";

    /**
     * 工序code：母炼
     */
    public final static String PROCEDURE_CODE_ML = "3";

    /**
     * 分厂胶料需求计划数据来源：新增
     */
    public final static String DEMAND_SOURCE_ADD = "1";

    /**
     * 分厂胶料需求计划数据来源：导入
     */
    public final static String DEMAND_SOURCE_IMPORT = "2";

    /**
     * 分厂胶料需求计划数据来源：拆分
     */
    public final static String DEMAND_SOURCE_SPLIT = "3";

    /**
     * 汇总胶料需求计划数据来源：汇总计划
     */
    public final static String COLLECT_SOURCE_AUTO = "0";

    /**
     * 汇总胶料需求计划数据来源：新增
     */
    public final static String COLLECT_SOURCE_ADD = "1";

    /**
     * 汇总胶料需求计划数据来源：导入
     */
    public final static String COLLECT_SOURCE_IMPORT = "2";

    /**
     * 分解胶料需求量数据来源：分解计划
     */
    public final static String DECOMPOSE_SOURCE_AUTO = "0";

    /**
     * 分解胶料需求量数据来源：新增
     */
    public final static String DECOMPOSE_SOURCE_ADD = "1";

    /**
     * 分解胶料需求量数据来源：导入
     */
    public final static String DECOMPOSE_SOURCE_IMPORT = "2";

    /**
     * 终炼/母炼日计划排程数据来源：自动排程
     */
    public final static String GLUE_SCHEDULE_SOURCE_AUTO = "0";

    /**
     * 终炼/母炼日计划排程数据来源：插单
     */
    public final static String GLUE_SCHEDULE_SOURCE_ADD = "1";

    /**
     * 终炼/母炼日计划排程数据来源：导入
     */
    public final static String GLUE_SCHEDULE_SOURCE_IMPORT = "2";

    /**
     * 终炼/母炼日计划排程数据来源：联动新增
     */
    public final static String GLUE_SCHEDULE_SOURCE_CASCADE = "3";

    /**
     * 终炼/母炼日计划排程数据来源：补量
     */
    public final static String GLUE_SCHEDULE_SOURCE_SUPPLEMENT = "4";

    /**
     * 硫磺辅料日计划排程数据来源：自动排程
     */
    public final static String MATERIAL_SCHEDULE_SOURCE_AUTO = "0";

    /**
     * 硫磺辅料日计划排程数据来源：插单
     */
    public final static String MATERIAL_SCHEDULE_SOURCE_ADD = "1";

    /**
     * 硫磺辅料日计划排程数据来源：导入
     */
    public final static String MATERIAL_SCHEDULE_SOURCE_IMPORT = "2";

    /**
     * 硫磺辅料日计划排程数据来源：跨区接收
     */
    public final static String MATERIAL_SCHEDULE_SOURCE_RECEIVE = "3";

    /**
     * 转机台
     */
    public final static String MATERIAL_SCHEDULE_SOURCE_MACHINE = "4";

    /**
     * 胶料名称列表的Redis缓存
     */
    public final static String CACHE_COMPOUND_NAME = "cache:compound:name";

    /**
     * 终炼胶料名称列表的Redis缓存
     */
    public final static String CACHE_FINALGLUE_NAME = "cache:finalglue:name";

    /**
     * 辅料名称列表的Redis缓存
     */
    public final static String CACHE_ACCESSORIES_NAME = "cache:accessories:name";

    /**
     * 物料名称列表的Redis缓存
     */
    public final static String CACHE_MATERIALS_NAME = "cache:materials:name";

    /**
     * 前端查询默认密炼区、配方默认密炼区
     */
    public final static String DEFAULT_MIX_AREA = "M2";

    /**
     * 参数表默认密炼区
     */
    public final static String DEFAULT_PARAMS_MIX_AREA = "0";

    /**
     * 指定密炼区的密炼机的Redis缓存（在冒号后拼接密炼区，不拼接表示所有密炼区）
     */
    public final static String CACHE_MIX_MACHINE = "cache:mix:machine:";

    /**
     * 指定密炼区的小料机台的Redis缓存（在冒号后拼接密炼区，不拼接表示所有密炼区）
     */
    public final static String CACHE_LHFL_MACHINE = "cache:lhfl:machine:";

    /**
     * 指定密炼区且启用的密炼机的Redis缓存（在冒号后拼接密炼区，不拼接表示所有密炼区）
     */
    public final static String CACHE_ENABLE_MIX_MACHINE = "cache:enable:mix:machine:";

    /**
     * 指定密炼区且启用的小料机台的Redis缓存（在冒号后拼接密炼区，不拼接表示所有密炼区）
     */
    public final static String CACHE_ENABLE_LHFL_MACHINE = "cache:enable:lhfl:machine:";

    /**
     * 所有配方机台信息缓存Key
     */
    public final static String CACHE_RECIPE_MACHINE = "cache:recipe:machine";

    /**
     * 排程日志排程类型：终炼母炼日计划排程
     */
    public final static String OPER_SCHEDULE_TYPE_GLUE = "0";

    /**
     * 排程日志排程类型：硫磺辅料日计划排程
     */
    public final static String OPER_SCHEDULE_TYPE_MATERIAL = "1";

    /**
     * 密炼区的权限数组
     */
    public final static String[] MIX_AREA_PERMISSIONS = {"M2"};

    /**
     * 对应的用户权限的key
     */
    public final static String MIX = "MIX";

    /**
     * Admin用户的权限
     */
    public final static String ADMIN_PERMISSION = "*:*:*";

    /**
     * 排程日志操作类型：转机台
     */
    public final static String OPER_TYPE_CHANGE_MACHINE = "0";

    /**
     * 排程日志操作类型：调量
     */
    public final static String OPER_TYPE_CHANGE_PLAN = "1";

    /**
     * 排程日志操作类型：插单
     */
    public final static String OPER_TYPE_INSERT_ORDER = "2";

    /**
     * 排程日志操作类型：调序
     */
    public final static String OPER_TYPE_SEQUENCING = "3";

    /**
     * 排程日志操作类型：自动排程
     */
    public final static String OPER_TYPE_AUTO_SCHEDULE = "4";

    /**
     * 排程日志操作类型：发布
     */
    public final static String OPER_TYPE_PUBLISH = "5";

    /**
     * 机台班制：长白班
     */
    public final static Integer CLASS_SHIFT_LONG_DAY = 1;

    /**
     * 机台班制：两班制
     */
    public final static Integer CLASS_SHIFT_TWO_SHIFT = 2;

    /**
     * 收尾计划：是
     */
    public final static String IS_FINISHING_YES = "1";

    /**
     * 收尾计划：否
     */
    public final static String IS_FINISHING_NO = "0";

    /**
     * 跨区发送接收状态：未接收
     */
    public final static String RECEIVE_STATUS_NO = "0";

    /**
     * 跨区发送接收状态：已接收
     */
    public final static String RECEIVE_STATUS_YES = "1";

    /**
     * 硫磺辅料跨区发送：是否自动生成：是
     */
    public final static String IS_AUTO_YES = "0";

    /**
     * 硫磺辅料跨区发送：是否自动生成：不是
     */
    public final static String IS_AUTO_NO = "1";

    /**
     * 跨区功能数据来源：分解胶料需求量
     */
    public final static String SOURCE_GLUE_DECOMPOSE_PLAN = "0";

    /**
     * 跨区功能数据来源：终炼母炼日计划
     */
    public final static String SOURCE_GLUE_SCHEDULE_RESULT = "1";

    /**
     * 跨区发送记录是通过跨区设置自动生成
     */
    public final static String SPAN_SEND_IS_AUTO = "0";
}
