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

    /** SYS1101042：口型切换时长（小时），默认0.75，T1机台切换感知：英寸相同口型板不同时的切换时长 */
    public static String TQ_MOUTH_SWITCH_TIME = "SYS1101042";

    /** SYS1101043：机台切换感知总开关，1=启用（默认），0=关闭保持历史口径 */
    public static String TQ_MACHINE_SWITCH_AWARE = "SYS1101043";

    /** SYS1101046：选机顺序新口径开关，1=启用（默认，S3.1执行序按供应时长升序+候选机台去利用率按编号升序一台台塞），0=关闭保持历史口径（执行序按6班计划量降序+候选按预计利用率升序负载均衡），用于双跑对比与回退 */
    public static String TQ_ASSIGN_ORDER_MODE = "SYS1101046";

    /** SYS1101045：备库分摊均衡开关，1=窗口均摊模式（产出分布跟随成型消耗节奏，实现多规格机台各班均衡满排），0=前倾模式（默认，逐班贪心满排阈值后延后），默认0 */
    public static String TQ_BACKUP_BALANCE_MODE = "SYS1101045";

    /** SYS1101047：备库窗口外推开关，1=启用（备库窗口超出成型8班时按最后三班(CLASS6/7/8)平均消耗外推，并以成型余量汇总×2封顶，对齐胎面capGuardFormingQty口径），0=关闭（默认，超出8班按剩余成型班数截断不外推），默认0 */
    public static String TQ_BACKUP_EXTRAPOLATION = "SYS1101047";

    /** SYS1101048：库存覆盖窗口班数，正整数，默认2（W仅决定备库触发判定面：班初库存&lt;成型(N+1)班起连续W班需求合计且≥两班保供时触发B区备库，备库总量与分摊班数按备库班数N计算不随W截断（2026-10-29判定用W、深度用N口径回归）；同时作为S5.6被动建库天花板窗口上限min(备库班数N,W)（未触发B区规格的富余产能预建库收口线）；W=2=历史两班触发面零变化基线，越南厂配3提前建库） */
    public static String TQ_STOCK_START_SHIFT_COUNT = "SYS1101048";

    /** SYS1101049：胎圈工装整车率（工装可用量折算系数：有效工装上限=工装车总数×整车率，对齐胎侧SYS0904006），默认1 */
    public static String TQ_VEHICLE_RATE = "SYS1101049";

    /** SYS1101050：胎圈工装账本开关，1=启用全局工装账本（对齐胎侧：总量×整车率上限-14点库存占用+成型当班消耗释放，逐规格实时扣减、班次结转），0=关闭保持历史规格隔离口径，默认0 */
    public static String TQ_TOOLING_LEDGER_ENABLED = "SYS1101050";

    /** 【已下线】SYS1101051：胎圈计划量顺延开关，2026-10-26 统一不顺延改造中废弃（备库规格溢出直接登记未排、非备库规格班1~5溢出并入下一班的口径已在策略层固化），常量保留仅为参数下线追溯 */

    /** SYS1101052：胎圈被动备库建库开关（2026-10-23被动备库改造新增），1/未配置=启用常规规格被动建库（S5.6回填时已配置备库班数的常规规格按目标库存天花板被动建库），0=常规规格仅补未来缺口（与未配置规格同口径，缺口回填不可关），默认启用 */
    public static String TQ_PASSIVE_BACKUP_ON = "SYS1101052";

    /** SYS1101053：胎圈班次均衡调整开关（2026-10-23两班缺口收口改造新增），1=启用S5班次均衡（全场总量平滑，历史口径回退手段），0/未配置=跳过S5均衡（计划量保持S3两班缺口形态），默认关闭 */
    public static String TQ_BALANCE_ADJUST_ON = "SYS1101053";

    /**
     * SYS1101054：胎圈实验规格提前排产班数，默认2班。
     *
     * <p>2026-11-05 新增，对齐胎面 SYS0803004「实验规格提前生产班数」：</p>
     * <ul>
     *   <li>实验规格识别口径：成型排程行产品状态 PRODUCT_STATUS ∈ (T,X)，即试制/量试规格，
     *       对应引擎侧 EMBRYO_TYPE_FLAG=1（不区分试制 X 与量试 T）；</li>
     *   <li>胎圈为规格聚合 + 6 班字段模型、无目标班次概念，故等价转译为"备库窗口班数下限"：
     *       试制规格第 1 班主动触发备库，生效备库班数 =
     *       max(备库配置班数, SYS1101033 新规格保底班数, 本参数实验规格提前排产班数)；</li>
     *   <li>只对试制/量试规格生效，纯新规格（非试制）仍走"落点对齐+窗口全覆盖到第6班"口径，不受本参数影响。</li>
     * </ul>
     */
    public static String TQ_EXPERIMENT_SPEC_ADVANCE_SHIFT_COUNT = "SYS1101054";

    /** 胎圈实验规格提前排产班数默认值：2班（与 SYS1101033 默认值一致，保证不配置时行为零变化） */
    public static final int DEFAULT_TQ_EXPERIMENT_SPEC_ADVANCE_SHIFT_COUNT = 2;

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

    /** SYS1101032：胎圈新规格回看天数（判定新规格时回看历史排程计划量的天数），默认7天 */
    public static String TQ_NEW_SPEC_LOOKBACK_DAYS = "SYS1101032";

    /** SYS1101033：胎圈新规格保底备库班数（新规格/试制规格主动备库时的保底班数，与备库配置班数取最大），默认2班 */
    public static String TQ_NEW_SPEC_BACKUP_SHIFT_COUNT = "SYS1101033";

    // SYS1101034（胎圈备库二次触发阈值，原默认0.7）已废弃：胎圈按业务口径严格供需（阈值固定1.0，
    // 班初库存+本班已排 < 下班成型消耗即触发二次备库），0.7缓冲仅钢丝圈 SYS1601006 使用。逻辑见 TqSecondaryBackupHandler

    /** SYS1101035：胎圈收尾判定容差（月计划剩余量与6班成型毛需求比较的容差，差值≤容差时仍判定为收尾；收尾规格6班合计与月剩余差额≤容差时补排到月剩余），默认100 */
    public static String TQ_CLOSE_OUT_TOLERANCE = "SYS1101035";

    /** SYS1101036：胎圈规格级班产上限口径模式（1=参数口径：统一取SYS1101029；2=速度口径：MES机台×规格生产速度优先，未命中回退SYS1101029），默认1 */
    public static String TQ_SPEC_CLASS_LIMIT_MODE = "SYS1101036";

    /** 规格级班产上限口径模式：参数口径（统一取SYS1101029班产阈值） */
    public static final int SPEC_CLASS_LIMIT_MODE_PARAM = 1;

    /** 规格级班产上限口径模式：速度口径（MES机台×规格生产速度优先，未命中回退SYS1101029） */
    public static final int SPEC_CLASS_LIMIT_MODE_SPEED = 2;

    /** SYS1101037：胎圈保鲜期折算班数（S5.95保鲜校验FIFO模拟：产出班次到消耗完班次的班次差超过此值告警；期初库存视为新鲜免检；0=不校验；一期仅告警人工干预），默认9班（72h/8h班） */
    public static String TQ_SHELF_LIFE_SHIFT_COUNT = "SYS1101037";

    /** SYS1101038：胎圈工艺停放折算班数（班粒度声明口径：折算班数<1班=同班产出班末即可供成型使用，不产生班次约束；仅日志声明+三期严格化预留），默认0.5班（4h/8h班） */
    public static String TQ_PARKING_SHIFT_COUNT = "SYS1101038";

    /** SYS1101039：胎圈工厂总库容上限（S5.95库容校验：逐班次库存轨迹总量超过此值告警；0=不校验，数值待业务提供；一期仅告警），默认0 */
    public static String TQ_WAREHOUSE_CAPACITY_LIMIT = "SYS1101039";

    /** SYS1101040：胎圈自动排程中文过程日志级别（OFF=关闭不落库，SUMMARY=摘要，FULL=完整事件；对齐胎面 SYS0807001 口径），默认SUMMARY */
    public static String TQ_PROCESS_LOG_LEVEL = "SYS1101040";

    /** SYS1603009：钢丝圈自动排程中文过程日志级别（OFF=关闭不落库，SUMMARY=摘要，FULL=完整事件；对齐胎圈 SYS1101040 口径），默认SUMMARY */
    public static String GSQ_PROCESS_LOG_LEVEL = "SYS1603009";

    /** SYS1101041：胎圈成型数据来源模式（成型排程同一天可能并存算法生成版CXPC与手工导入版CXDR两个批次，胎圈取数按此参数选定版本），默认1 */
    public static String TQ_CX_SOURCE_MODE = "SYS1101041";

    /** 成型数据来源模式-自动导入优先（模式1）：当天存在CXDR导入版批次则取导入版，否则回退CXPC生成版 */
    public static final int CX_SOURCE_MODE_AUTO_IMPORT_FIRST = 1;

    /** 成型数据来源模式-仅算法生成版（模式2）：强制取CXPC前缀批次 */
    public static final int CX_SOURCE_MODE_GENERATED_ONLY = 2;

    /** 成型数据来源模式-仅导入版（模式3）：强制取CXDR前缀批次 */
    public static final int CX_SOURCE_MODE_IMPORT_ONLY = 3;

    /** 成型排程批次号前缀-手工导入版（CXDR + yyyyMMdd + 3位序号，DATA_SOURCE=2） */
    public static final String CX_BATCH_PREFIX_IMPORT = "CXDR";

    /** 成型排程批次号前缀-算法生成版（CXPC + yyyyMMdd + 序号，DATA_SOURCE=0/3） */
    public static final String CX_BATCH_PREFIX_GENERATED = "CXPC";

	/* ==================== 以下常量合并自 mix-engine-common（密炼排程） ==================== */

	public static String PROCEDURE_CODE_GLUE = "0";
	public static String PROCEDURE_CODE_MATERIAL = "1";
	public static String DEMAND_PREFIX = "DEMAND";
	public static String COLLECT_PREFIX = "COLLECT";
	public static String DECOMPOSE_PREFIX = "DECOMPOSE";
	public static String GLUE_SCHEDULE_PREFIX = "GLUE";
	public static String MATERIAL_SCHEDULE_PREFIX = "MATERIAL";
	public static String IS_FINISHING_YES = "1";
	public static String MIX_AREA_DEFAULT = "0";
	public static String RECIPE_TYPE_ZZ = "1";
	public static String COMMONLY_USED_DAY = "COMMONLY_USED_DAY";
	public static String SAFE_STOCK_RATE = "SAFE_STOCK_RATE";
	public static String MATERIAL_INTERVAL_TIME = "MATERIAL_INTERVAL_TIME";
	public static String PRE_PREPARE_TIME = "PRE_PREPARE_TIME";
	public static String MACHINE_DEFAULT_CAPACITY = "MACHINE_DEFAULT_CAPACITY";
	public static final String DINNER_TIME = "DINNER_TIME";
	public static final String DECOMPOSE_GLUE_TYPE = "DECOMPOSE_GLUE_TYPE";
	public static Integer CLASS_SHIFT_ONE = 1;
	public static Integer CLASS_SHIFT_TWO = 2;
	public static Integer CLASS_SHIFT_THREE = 3;
	public static String CLASS_MID = "MID";
	public static Integer COMMONLY_USED_YES = 1;
	public static Integer COMMONLY_USED_NO = 0;
	public static int ORDER_MULTIPLE = 10;
	public static String DEMAND_PLANNING_DIVISION = "--";
	public static String DECOMPOSE_GLUE_TYPE_0 = "0";

	/* ==================== 以下常量合并自 mix-engine-common（密炼排程） ==================== */

	public static String CLASS_NIGHT = "NIGHT";
	public static String CLASS_DAY = "DAY";
}
