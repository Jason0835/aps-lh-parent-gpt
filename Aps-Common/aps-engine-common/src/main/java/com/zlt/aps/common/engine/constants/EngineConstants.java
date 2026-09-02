package com.zlt.aps.common.engine.constants;

import com.zlt.aps.common.engine.enums.ClassNumThreePlanEnums;

/**
 * 引擎常量类
 **/
public class EngineConstants {

    /**
     * 胎面批次号前缀
     */
    public static final String TM_BATCH_NO_PREFIX = "TM";

    /**
     * 胎面日期批次号前缀
     */
    public static final String TM_BATCH_DATE_PREFIX = "TM-";

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
     *  参数code：标准卷曲长度（部件卷曲到一个工件上的标准长度）
     */
    public static String STANDARD_CRIMP_LENGTH = "STANDARD_CRIMP_LENGTH";

    /**
     * 参数code：仅投产阶段规格排产标识（值为1时，表示仅投产阶段的规格才进行自动排程；其他值的时候表示自动排程的排产全部规格）
     */
    public static String PRODUCTION_STAGE_PRODUCE = "PRODUCTION_STAGE_PRODUCE";

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
     * 机台开机班次：夜班，对应 ClassNumThreePlanEnums.CLASS_NIGHT("01")
     */
    public static final String NIGHT_CLASS_SHIFT = ClassNumThreePlanEnums.CLASS_NIGHT.getClassIndex();

    /**
     * 机台开机班次：早班，对应 ClassNumThreePlanEnums.CLASS_MORNING("02")
     */
    public static final String MORNING_CLASS_SHIFT = ClassNumThreePlanEnums.CLASS_MORNING.getClassIndex();

    /**
     * 机台开机班次：中班，对应 ClassNumThreePlanEnums.CLASS_DAY("03")
     */
    public static final String DAY_CLASS_SHIFT = ClassNumThreePlanEnums.CLASS_DAY.getClassIndex();

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

    // ==================== 胎圈排程专用参数（SYS1101XXX 系列，按编码升序排列）====================

    /** SYS1101001：胎圈备库班数（保证成型的班次排产数），默认1 */
    public static String TQ_BACKUP_SHIFT_COUNT = "SYS1101001";

    /** SYS1101002：胎圈需求系数（胎圈消耗量=成型需求量×系数），默认2 */
    public static String TQ_DEMAND_COEFFICIENT = "SYS1101002";

    /** SYS1101003：单班时长（小时），默认8 */
    public static String TQ_CLASS_HOURS = "SYS1101003";

    /** SYS1101004：工装容量，默认500 */
    public static String TQ_TOOL_CAPACITY = "SYS1101004";

    /** SYS1101005：损耗率，默认0.02 */
    public static String TQ_LOSS_RATE = "SYS1101005";

    /** SYS1101006：往前一班合并计划量阈值，默认100 */
    public static String TQ_MERGE_THRESHOLD = "SYS1101006";

    /** SYS1101007：预生产库存天数，默认1 */
    public static String TQ_PRODUCT_STOCK_DAY = "SYS1101007";

    /** SYS1101008：大需求量阈值，默认3000 */
    public static String TQ_LARGE_DEMAND = "SYS1101008";

    /** SYS1101009：收尾提醒数量，默认50 */
    public static String TQ_CLOSE_OUT_NUM = "SYS1101009";

    /** SYS1101010：单最少排产量，默认10 */
    public static String TQ_MIN_PLAN_QTY = "SYS1101010";

    /** SYS1101011：班产上限，默认3000 */
    public static String TQ_MAX_CLASS_OUTPUT = "SYS1101011";

    /** SYS1101012：需求算法模式，1=算法1(三班最大值)，2=算法2(逐班对应)，默认2 */
    public static String TQ_DEMAND_CALC_MODE = "SYS1101012";

    /** SYS1101013：库存供应时长阈值（小时），达到后切换规格，默认24 */
    public static String TQ_SUPPLY_TIME_THRESHOLD = "SYS1101013";

    /** SYS1101014：钢丝圈切换时长（小时），默认0.5 */
    public static String TQ_SPEC_SWITCH_TIME = "SYS1101014";

    /** SYS1101015：三角胶切换时长（小时），默认0.8 */
    public static String TQ_APEX_SWITCH_TIME = "SYS1101015";

    /** SYS1101016：切英寸时长（小时），默认1.5 */
    public static String TQ_INCH_SWITCH_TIME = "SYS1101016";

    /** SYS1101017：库消比高阈值，默认2.0 */
    public static String TQ_STOCK_CONSUME_RATIO_HIGH = "SYS1101017";

    /** SYS1101018：库消比低阈值，默认0.5 */
    public static String TQ_STOCK_CONSUME_RATIO_LOW = "SYS1101018";

    /** SYS1101019：成型停产开产阈值天数，默认2 */
    public static String TQ_STOP_INTERSECTION_DAYS = "SYS1101019";

    /** SYS1101020：开产库存补量阈值，默认0 */
    public static String TQ_REOPEN_STOCK_THRESHOLD = "SYS1101020";

    /** SYS1101021：成型停产预排班数，默认2 */
    public static String TQ_MOLDING_STOP_PRE_SHIFT_COUNT = "SYS1101021";

    /** SYS1101022：工装车总数（全局统一值），默认50 */
    public static String TQ_TOOLING_TOTAL = "SYS1101022";

    /** SYS1101024：大尺寸规格阈值，寸口大于此值为大尺寸规格，默认35 */
    public static String TQ_BIG_SIZE_SPEC = "SYS1101024";

    /** SYS1101025：库存损耗率，默认0 */
    public static String TQ_STOCK_LOSS_RATE = "SYS1101025";

    /** SYS1101026：各班计划量均分阈值，默认300 */
    public static String TQ_EQUAL_SHARE_THRESHOLD = "SYS1101026";

    /** SYS1101027：交接班库存基准值，默认22500 */
    public static String TQ_CLASS_STOCK_REFERENCE = "SYS1101027";

    /** SYS1101028：一次生产卷数，默认220 */
    public static String TQ_ONE_ROLL_NUM = "SYS1101028";

    /** SYS1101029：胎圈规格班次最大班产阈值（多规格模式下，触发备库的胎圈当班初始排产上限），默认1000 */
    public static String TQ_BACKUP_SHIFT_THRESHOLD = "SYS1101029";

    /** SYS1101030：取整合并阈值（备库分摊时剩余量小于此值则合并到当前班次，不再新开一班向上取整），默认0不启用 */
    public static String TQ_ROUNDING_MERGE_THRESHOLD = "SYS1101030";

    /** SYS1101031：机台定额超排容忍阈值（计划量超出机台剩余产能，且超出部分≤此值时允许当班超排，不延后到下一班），默认0不启用 */
    public static String TQ_MACHINE_OVER_ASSIGN_TOLERANCE = "SYS1101031";

    /** SYS1101032：胎圈新规格回看天数（判定新规格时回看历史排程计划量的天数），默认7天 */
    public static String TQ_NEW_SPEC_LOOKBACK_DAYS = "SYS1101032";

    /** SYS1101033：胎圈新规格保底备库班数（新规格/试制规格主动备库时的保底班数，与备库配置班数取最大），默认2班 */
    public static String TQ_NEW_SPEC_BACKUP_SHIFT_COUNT = "SYS1101033";
}
