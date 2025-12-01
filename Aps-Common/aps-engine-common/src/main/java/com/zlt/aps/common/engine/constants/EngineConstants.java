package com.zlt.aps.common.engine.constants;

/**
  * 引擎常量类
**/
public class EngineConstants {

    /**
     * 胎面批次号前缀
     */
    public static String TM_BATCH_NO_PREFIX = "TM";

    /**
     * 胎面批次号前缀
     */
    public static String TC_BATCH_NO_PREFIX = "TC";

    /**
     * 内衬批次号前缀
     */
    public static String NC_BATCH_NO_PREFIX = "NC";

    /**
     * 胎面批次号前缀
     */
    public static String TQ_BATCH_NO_PREFIX = "TQ";

    /**
     * 15度裁断批次号前缀
     */
    public static String CD15_BATCH_NO_PREFIX = "CD15";

    /**
     * 90度裁断批次号前缀
     */
    public static String CD90_BATCH_NO_PREFIX = "CD90";

    /**
     * 钢带压延批次号前缀
     */
    public static String GDYY_BATCH_NO_PREFIX = "GDYY";

    /**
     * 纤维压延批次号前缀
     */
    public static String XWYY_BATCH_NO_PREFIX = "XWYY";

    /**
     * 钢丝圈批次号前缀
     */
    public static String GSQ_BATCH_NO_PREFIX = "GSQ";

    /**
     * 参数code：中班总量和夜班总量差额百分比
     */
    public static String PLAN_DIFFERENCE_RATE = "PLAN_DIFFERENCE_RATE";

    /**
     * 参数code：库存供应时长小时数
     */
    public static String SUPPLY_TIME_PASS = "SUPPLY_TIME_PASS";

    /**
     * 参数code：损耗率
     */
    public static String LOSS_RATE = "LOSS_RATE";

    /**
     * 参数code：仅投产阶段规格排产标识（值为1时，表示仅投产阶段的规格才进行自动排程；其他值的时候表示自动排程的排产全部规格）
     */
    public static String PRODUCTION_STAGE_PRODUCE = "PRODUCTION_STAGE_PRODUCE";

    /**
     *  参数code：标准卷曲长度（部件卷曲到一个工件上的标准长度）
     */
    public static String STANDARD_CRIMP_LENGTH = "STANDARD_CRIMP_LENGTH";

    /**
     *  参数code：卷曲数小数取整值（小数部分大于等于该值的进位，否则舍弃）
     */
    public static String CURL_DECIMAL_ROUNDING = "CURL_DECIMAL_ROUNDING";

    /**
     * 夜班产量参考值
     */
    public static String MID_PLAN_QTY_REFERENCE = "MID_PLAN_QTY_REFERENCE";

    /**
     *  参数code：共用规格收尾判断天数（判断共用规格是否收尾时会按照配置的天数往回看是否有生产）
     */
    public static String CLOSE_OUT_DAYS = "CLOSE_OUT_DAYS";

    /**
     * 参数code：往前一班合并计划量阈值
     */
    public static String MERGE_PLAN_THRESHOLD = "MERGE_PLAN_THRESHOLD";

    /**
     * 各班计划量均分阈值
     */
    public static String EQUAL_SHARE_THRESHOLD = "EQUAL_SHARE_THRESHOLD";

    /**
     * 交接班库存基准值
     */
    public final static String CLASS_STOCK_REFERENCE = "CLASS_STOCK_REFERENCE";

    /**
     * 同胶料合并生产预计库存可供应时长
     */
    public static String GLUE_MERGE_THRESHOLD = "GLUE_MERGE_THRESHOLD";

    /**
     * 同胶料归并生产可供应时长(MAX)
     */
    public static String GLUE_MERGE_THRESHOLD_MAX = "GLUE_MERGE_THRESHOLD_MAX";

    /**
     * 排程不计算库存开关
     */
    public static String SCHEDULE_WITH_OUT_STOCK = "SCHEDULE_WITH_OUT_STOCK";

    /**
     * 库存损耗率
     */
    public static String STOCK_LOSS_RATE = "STOCK_LOSS_RATE";

    /**
     * 参数code：幅宽
     */
    public static String BREADTH = "BREADTH";

    /**
     * 参数code：标准长度
     */
    public static String STANDARD_SIZE = "STANDARD_SIZE";

    /**
     * 参数code：预留库存系数
     */
    public static String STOCK_RATIO = "STOCK_RATIO";

    /**
     * 参数Code：钢丝卷长
     */
    public static String WIRE_COIL_LENGTH = "WIRE_COIL_LENGTH";

    /**
     * 参数Code：纤维原丝卷长
     */
    public final static String ORIGINAL_LINE_LENGTH = "ORIGINAL_LINE_LENGTH";

    /**
     * 参数code：纤维压延默认可破大卷数
     */
    public final static String XWYY_BREAK_ROLL_NUM = "XWYY_BREAK_ROLL_NUM";

    /**
     * 参数code：卷曲大卷数取整规则
     */
    public final static String MIN_ROUND_ROLL_NUM = "MIN_ROUND_ROLL_NUM";

    /**
     * 工装容量
     */
    public final static String TOOL_CAPACITY = "TOOL_CAPACITY";

    /**
     * 参数code：卷曲长度
     */
    public final static String CRIMP_LENGTH = "CRIMP_LENGTH";

    /**
     * 半部件临近收尾阈值
     */
    public static String CLOSE_OUT_NUM = "CLOSE_OUT_NUM";

    /**
     * 库存预生产小时数
     */
    public final static String PRODUCT_STOCK_HOUR = "PRODUCT_STOCK_HOUR";

    /**
     * 库存预生产小时数（一次法）
     */
    public final static String PRODUCT_STOCK_HOUR_ONE_METHOD = "PRODUCT_STOCK_HOUR_ONE_METHOD";

    /**
     * 大需求量规格阈值
     */
    public final static String LARGE_DEMAND = "LARGE_DEMAND";

    /**
     * 胶料大需求量规格阈值
     */
    public final static String GLUE_LARGE_DEMAND = "GLUE_LARGE_DEMAND";

    /**
     * 大需求量规格阈值扣减量
     */
    public final static String LARGE_DEMAND_REDUCE = "LARGE_DEMAND_REDUCE";

    /**
     * 低于参数的计划量可以推迟到下个班做
     */
    public final static String DELAY_PLAN_QTY = "DELAY_PLAN_QTY";

    /**
     * 工装包含大卷数
     */
    public final static String TOOL_ROLL_NUM = "TOOL_ROLL_NUM";

    /**
     * 供应成型规格数集中生成产阈值
     */
    public final static String SUPPLY_SPEC_CONCENTRATE = "SUPPLY_SPEC_CONCENTRATE";

    /**
     * 供应成型规格数分散生成产阈值
     */
    public final static String SUPPLY_SPEC_DISTRIBUTE = "SUPPLY_SPEC_DISTRIBUTE";

    /**
     * 单班最少排产量
     */
    public final static String MIN_PLAN_QTY = "MIN_PLAN_QTY";

    /**
     * 大尺寸规格阈值
     */
    public final static String BIG_SIZE_SPEC = "BIG_SIZE_SPEC";
    /**
     * 强制夜班规格
     */
    public final static String NIGHT_SPEC = "NIGHT_SPEC";
    /**
     * 强制早班规格
     */
    public final static String MID_SPEC = "MID_SPEC";
    /**
     * 机台产能时长
     */
    public final static String MACHINE_QUATA_HOUR = "MACHINE_QUATA_HOUR";

    /**
     * 硫化机空闲天数
     */
    public static String LH_MACHINE_FREE_DAY = "LH_MACHINE_FREE_DAY";
	/**
	 * 钢带压延按大卷计算库存开关
	 */
    public final static String GDYY_STOCK_ROLL_SWITCH = "GDYY_STOCK_ROLL_SWITCH";
    /**
     * 预估库存开关
     */
    public final static String ESTIMATE_STOCK_SWITCH = "ESTIMATE_STOCK_SWITCH";
	/**
	 * 钢带压延按大卷计算库存开关：打开
	 */
    public final static String GDYY_STOCK_ROLL_SWITCH_ON = "1";

    /**
     * 生产状态：未生产
     */
    public static String PRODUCTION_STATUS_NOT = "0";

    /**
     * 生产状态：生产中
     */
    public static String PRODUCTION_STATUS_ING = "1";

    /**
     * 生产状态：生产完成
     */
    public static String PRODUCTION_STATUS_FINISH = "2";

    /**
     * 收尾提示标识（提示收尾）
     */
    public static String CLOSE_TIP_NEED = "0";

    /**
     * 收尾提示标识（不需要提示）
     */
    public static String CLOSE_TIP_NOT = "1";

    /**
     * 作业类型：限制作业
     */
    public static String JOB_TYPE_CAN = "0";

    /**
     * 作业类型：不可作业
     */
    public static String JOB_TYPE_NOT = "1";

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
     * 默认硫化时长参数code
     */
    public static final  String DEFAULT_LH_TIRE_TIME="DEFAULT_LH_TIRE_TIME";

    /**
     * 是否配套胎类型：是
     */
    public static final String ASSORT_TYPE_YES="0";

    /**
     * 是否配套胎类型：否
     */
    public static final String ASSORT_TYPE_NO="1";

    /**
     * 是否定稿：定稿
     */
    public static final String  IS_FINALIZED_YES="0";

    /**
     * 是否定稿：初稿
     */
    public static final String  IS_FINALIZED_NO="1";
    /**
     * 排产数据来源：自动排产
     */
    public static final String SCHEDULE_DATA_SOURCE_AUTO = "0";
    /**
     * 排产数据来源：插单
     */
    public static final String SCHEDULE_DATA_SOURCE_INSERT = "1";
    /**
     * 排产数据来源：导入
     */
    public static final String SCHEDULE_DATA_SOURCE_IMPORT = "2";

    /**
     * 数据来源：成型排程
     */
    public static final String CHANGE_MACHINE_DATA_SOURCE_SCHEDULE="0";

    /**
     * 数据来源：增补计划
     */
    public static final String CHANGE_MACHINE_DATA_SOURCE_SUPPLE="1";

    /**
     * 机台开机班次：夜班，对应字典：CLASS_NUM
     */
    public static final String NIGHT_CLASS_SHIFT = "2";

    /**
     * 机台开机班次：早班，对应字典：CLASS_NUM
     */
    public static final String DAY_CLASS_SHIFT = "3";

    /**
     * 一次生产卷数
     */
    public static final String ONE_ROLL_NUM = "ONE_ROLL_NUM";

    /**
     * 限制早班生产胶料
     */
    public static final String DAY_PRODUCT_GLUE = "DAY_PRODUCT_GLUE";

    /**
     * 胎面：成型合并计划顺序
     */
    public static final String CX_MERGE_MIN_SORT = "CX_MERGE_MIN_SORT";

    /**
     * 胎面：成型合并计划顺序
     */
    public static final String CX_MERGE_MAX_SORT = "CX_MERGE_MAX_SORT";

    /**
     * 胎面：早合到夜最大计划量(卷)
     */
    public static final String MERGE_MAX_ROLL = "MERGE_MAX_ROLL";

    /**
     * 胎侧：二次法最小取整卷数
     */
    public static final String TC_VM_MIN_ROLL_NUM = "TC_VM_MIN_ROLL_NUM";

    /**
     * 限制早班生产规格前缀
     */
    public static final String DAY_PRODUCT_CODE_PREFIX = "DAY_PRODUCT_CODE_PREFIX";

    /**
     * 胎侧：成型顺序合并最大卷数(夜班早班合计卷数)
     */
    public static final String CX_SORT_MERGE_ROLL_NUM = "CX_SORT_MERGE_ROLL_NUM";

    /**
     * 胎面：总工装数量，如果夜班计划+库存超过总工装数量，则不提前生产早班的计划
     */
    public static final String TOTAL_ROLL_NUM = "TOTAL_ROLL_NUM";

    /**
     * 胎面：成型时长小于参数的排产顺序优先
     */
    public static String LESS_SUPPLY_TIME = "LESS_SUPPLY_TIME";
}
