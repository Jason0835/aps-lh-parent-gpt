package com.zlt.aps.lh.context;

import com.zlt.aps.lh.engine.strategy.support.ContinuationRemainderFinishGroup;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.common.engine.domain.LhMonthStartDayResult;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.*;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.SingleControlMachineModeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.StructureEarlyProductionAdmission;
import com.zlt.aps.lh.component.StructureShiftInMachineIndex;
import com.zlt.aps.lh.engine.strategy.support.*;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.handler.SkuMonthPlanCalculator;
import com.zlt.aps.lh.service.ILhDailyMouldCalcService;
import com.zlt.aps.lh.engine.strategy.support.ContinuationEndingAllocationSnapshot;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.SkuConstructionRefResolverUtil;
import com.zlt.aps.mdm.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化排程上下文。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>贯穿一次硫化排程从 S4.1 到 S4.6 的可变数据总线；</li>
 *   <li>承载月计划、日计划额度、机台、模具、胎胚库存、MES在机、工作日历、保养/清洗等基础数据；</li>
 *   <li>承载 SKU 归集结果、续作列表、新增列表、机台分配状态、结果列表、未排列表和模具交替计划；</li>
 *   <li>为排序、选机、收尾、换模、换活字块、班次分配和结果校验策略共享同一运行态。</li>
 * </ul>
 *
 * <p>注意：该对象会被多个 Handler/Strategy 原地修改。新增字段时必须同时确认初始化入口、消费策略、
 * 结果落库和回归测试，避免上下文字段只有写入没有消费，或只有消费没有初始化。</p>
 *
 * @author APS
 */
@Data
public class LhScheduleContext {

    /**
     * 历史交替计划缺少产品状态时统一使用的正规状态
     */
    private static final String FORMAL_PRODUCT_STATUS = "S";

    // ========== 排程基本参数 ==========
    /**
     * 计划量计算起始日：默认为月份第一天
     * 当下个月定稿后，则为定稿需求的库存取值日
     * 以及是否需要含上个月超欠产标记
     */
    private LhMonthStartDayResult lhMonthStartDayInfo;
    /**
     * 分厂编号
     */
    private String factoryCode;
    /**
     * 分厂名称
     */
    private String factoryName;
    /**
     * 排程目标日/业务保存日期（与请求体日期一致，业务口径为 T+1），仅用于结果保存、查询、日志等业务归属
     */
    private Date scheduleTargetDate;
    /**
     * 排程窗口起点 T 日：由 {@link #scheduleTargetDate} 减去 (排程天数 - 2) 得到，
     * 排程天数来自硫化参数 {@code SCHEDULE_DAYS}（默认见 {@link com.zlt.aps.lh.api.constant.LhScheduleConstant#SCHEDULE_DAYS}），
     * 供班次计算、基础数据加载等引擎时间轴使用
     */
    private Date scheduleDate;
    /**
     * 排程窗口结束日期 T+2 日：由 {@link #scheduleDate} + 2 得到，
     * 用于 day1/day2/day3 月计划映射、产能计算、加机台、收尾、欠产追补、换模日上限、
     * 跨月检测、班次日期反推等排程核心逻辑，
     * 与仅用于业务保存/查询的 {@link #scheduleTargetDate}（T+1）分离。
     */
    private Date windowEndDate;

    /**
     * 本次排程允许拉取提前生产 SKU 的最晚原始计划日期。
     * <p>统一按“排程窗口结束日 + SYS0304028”在排程上下文初始化时计算，
     * S4.2 基础数据加载、S4.4 换活字块和 S4.5 新增排产必须共用该边界，
     * 禁止再按当前业务日分别计算可提前范围。</p>
     */
    private Date earlyProductionMaxDate;

    /**
     * 当前排程日期
     */
    private Date currentScheduleDate;
    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 月计划需求版本
     */
    private String monthPlanVersion;
    /**
     * 月计划排产版本
     */
    private String productionVersion;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 本次排程配置快照
     */
    private LhScheduleConfig scheduleConfig;
    /**
     * 硫化开模时间
     */
    private Date curingOpenMoldTime;
    /**
     * 硫化停锅时间
     */
    private Date curingStopPotTime;
    /**
     * 开产班次
     */
    private ShiftProductionControlDTO openProductionShift;
    /**
     * 停产班次
     */
    private ShiftProductionControlDTO stopProductionShift;
    /**
     * 是否启用开停产管控
     */
    private boolean enableOpenStopProductionControl;
    /**
     * 是否处于开产模式
     */
    private boolean openProductionMode;
    /**
     * 是否处于停产模式
     */
    private boolean stopProductionMode;
    /**
     * 开产欠产阈值比例
     */
    private BigDecimal openProductionShortageThresholdRate;

    // ========== 硫化参数(从LhParams加载) ==========

    /**
     * 硫化参数Map, key=paramCode, value=paramValue
     */
    private Map<String, String> lhParamsMap = new HashMap<>();

    // ========== 基础数据(S4.2加载) ==========

    /**
     * 月生产计划列表，来源于月计划最终结果表，是 SKU 归集和 day1/day2/day3 窗口计划量的主数据来源
     */
    private List<FactoryMonthPlanProductionFinalResult> monthPlanList = new ArrayList<>();
    /**
     * 本次排程加载的全部月计划列表，跨月时包含多个自然月；供按业务日期解析 dayN 使用
     */
    private List<FactoryMonthPlanProductionFinalResult> loadedMonthPlanList = new ArrayList<>();
    /**
     * 月-硫化日计划调整信息
     */
    private List<LhDayPlanAdjustVo> allLhDayPlanAdjustList = Lists.newArrayList();
    /**
     * 物料+产品状态+年月 -> 月计划记录索引，跨月或同物料多产品状态时避免误取其他计划
     */
    private Map<String, FactoryMonthPlanProductionFinalResult> monthPlanByMaterialMonthMap = new LinkedHashMap<>();
    /**
     * 续作降模尺寸排序专用月计划索引，key=物料+产品状态+年月。
     * <p>仅加载排程窗口结束日后的 T+3～T+7 月计划，禁止合并到通用月计划列表或索引，
     * 避免尺寸排序观察范围影响新增、增机、停产保机、提前生产等其他规则。</p>
     */
    private Map<String, FactoryMonthPlanProductionFinalResult>
            continuationReduceDimensionMonthPlanByMaterialMonthMap = new LinkedHashMap<>();
    /**
     * 年月 -> 定稿需求版本，跨月加载月计划和周程调整时按自然月取版本
     */
    private Map<String, String> monthPlanVersionByYearMonthMap = new LinkedHashMap<>();
    /**
     * 年月 -> 定稿排产版本，跨月加载月计划和结构机台统计时按自然月取版本
     */
    private Map<String, String> productionVersionByYearMonthMap = new LinkedHashMap<>();
    /**
     * 结构名称 -> 结构转产配置中的最大收尾自然日。
     * <p>S4.2在月计划加载完成后，按排程窗口[T,T+2]覆盖的每个自然月及该月排产版本查询
     * {@code T_MP_STRUCTURE_ALLOCATION}，将非空END_DAY还原为完整自然日后按结构取最大值。
     * S4.5新增SKU选机只读该快照，最大日期不在窗口内或结构无记录时不触发结构收尾对齐；
     * SKU排序不得读取或扩大该快照，应使用独立的structurePriorityMaxEndingDateMap。</p>
     */
    private Map<String, LocalDate> structureMaxEndingDateMap = new LinkedHashMap<>(16);
    /**
     * 结构名称 -> SKU排序使用的结构转产最大收尾自然日。
     * <p>S4.2按参数{@code SYS0304002}确定严格小于阈值时可能命中的日期范围，复用结构转产表
     * 的工厂、年月、排产版本、正常计划类型和结构名称查询口径，将非空END_DAY还原为完整
     * 自然日并按结构取最大值。S4.4/S4.5排序仅从该快照按结构计算一次距离天数，再把命中结果
     * 应用于当前参与排产的同结构全部SKU；该快照与三天结构收尾对齐快照隔离，禁止用于选机。</p>
     */
    private Map<String, LocalDate> structurePriorityMaxEndingDateMap = new LinkedHashMap<>(16);
    /**
     * 物料+产品状态+年月 -> 月累计完成量，避免同一物料不同产品状态或跨月时完成量串月
     */
    private Map<String, Integer> materialMonthFinishedQtyByMonthMap = new HashMap<>();
    /**
     * 工作日历列表
     */
    private List<MdmWorkCalendar> workCalendarList = new ArrayList<>();
    /**
     * 日产能限制VoMap, key=日期(支持跨月不冲突)；由 {@link com.zlt.aps.lh.service.ILhDailyMouldCalcService#loadDailyCapacityLimitMap} 按年月合并加载
     * <p>用于 {@link com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine#getMouldByDay} 的 dailyCapacityLimitVo 参数</p>
     */
    private Map<LocalDate, MpDailyCapacityLimitVo> dailyCapacityLimitVoMap = new HashMap<>();
    /**
     * 模具计算排产参数Map, key=paramCode(如 SYS0203003~SYS0203006)；由 {@link com.zlt.aps.lh.service.ILhDailyMouldCalcService#loadMouldAdjustParamMap} 加载
     * <p>用于 {@link com.zlt.aps.mp.engine.adjust.MpWeekRollAdjustEngine#getMouldByDay} 的 paramMap 参数</p>
     */
    private Map<String, Object> mouldAdjustParamMap = new HashMap<>();
    /**
     * 排程日期(T日) ~ T+2日窗口日模具计算结果Map, key=物料编码|产品状态, value=窗口内逐日模具数/机台数汇总；
     * 由 {@link com.zlt.aps.lh.service.ILhDailyMouldCalcService#loadDailyMouldSummary} 在数据加载环节预计算。
     * <p>供后续判断是否需要加机台或获取机台数时，直接按物料编码+产品状态（+日期）查询</p>
     */
    private Map<String, ILhDailyMouldCalcService.DailyMouldSummary> dailyMouldResultMap = new HashMap<>();
    /**
     * SKU日硫化产能Map, key=materialCode
     */
    private Map<String, MdmSkuLhCapacity> skuLhCapacityMap = new HashMap<>();
    /**
     * 设备停机计划列表
     */
    private List<MdmDevicePlanShut> devicePlanShutList = new ArrayList<>();
    /** 05 原始计划快照：与固定时刻转换后的运行态列表分离，扩展窗口时禁止重复转换。 */
    private Map<Long, MdmDevicePlanShut> plannedRepairSourcePlanMap = new LinkedHashMap<>(16);
    /** 06 独立故障窗口，按来源计划主键去重；不进入普通设备停机业务列表。 */
    private Map<Long, MachineFaultWindowDTO> temporaryFaultWindowMap = new LinkedHashMap<>(16);
    /**
     * 达到连续两班禁产阈值的续作临时故障迁移事件，key=运行态续作机台编码。
     * <p>该事件贯穿续作解绑、历史机台优先、正常候选回流和最终交替计划生成。</p>
     */
    private Map<String, ContinuationTemporaryFaultTransferEvent> continuationTemporaryFaultTransferEventMap =
            new LinkedHashMap<String, ContinuationTemporaryFaultTransferEvent>(4);
    /**
     * 本次排程已加载的清洗类设备停机候选列表。
     * <p>仅保存计划开始时间不早于排程日期 T 日、尚未实际完成且未删除的干冰/喷砂清洗候选。
     * 清洗候选转换为运行态清洗窗口后会从 {@link #devicePlanShutList} 剥离，但该只读快照仍供续作降模
     * 判断“机台是否存在有效清洗计划”使用，不参与普通停机产能扣减，也不改变清洗每日上限和班次安排。</p>
     */
    private List<MdmDevicePlanShut> loadedCleaningPlanShutList = new ArrayList<>();
    /**
     * 清洗计划排程日期回填项列表。
     * <p>清洗实际安排成功时记录实际清洗开始时间；因 SKU 3 天内收尾跳过清洗时记录收尾日期。
     * 该列表在排程结果落库事务（{@code replaceScheduleAtomically}）内统一回填到
     * {@code T_MDM_DEVICE_PLAN_SHUT.SCHEDULE_DATE}，按设备停机计划主键 id 去重更新。
     * 配对侧派生窗口、超窗口上限、配置非法、最晚日期超限等未安排场景不收集回填项。</p>
     */
    private List<CleaningScheduleDateFillItem> cleaningScheduleDateFillList = new ArrayList<>();
    /**
     * SKU与模具关系Map, key=materialCode
     */
    private Map<String, List<MdmSkuMouldRel>> skuMouldRelMap = new HashMap<>();
    /**
     * 模具台账Map, key=mouldCode
     */
    private Map<String, MdmModelInfo> modelInfoMap = new HashMap<>();
    /**
     * 硫化机台信息Map, key=machineCode
     */
    private Map<String, LhMachineInfo> machineInfoMap = new LinkedHashMap<>();
    /** 年月与排产版本 → 结构 → 只读专供快照，禁止跨月合并关系。 */
    private Map<String, Map<String, StructureDedicatedMachineContext>> structureDedicatedMachineContextMap = new LinkedHashMap<>(4);
    /** 成型机 → 有效专供硫化机物理编码集合。 */
    private Map<String, Set<String>> formingMachineSupplyLhMachineMap = new LinkedHashMap<>(16);
    /** 物理硫化机 → 专供成型机集合，支持多对多关系。 */
    private Map<String, Set<String>> supplyFormingMachinesByLhMachineMap = new LinkedHashMap<>(16);
    /**
     * 新增排产机台资源作用域。
     * <p>空集合表示沿用原主流程全部机台；班次9独立上下文写入班次8真实释放机台编码，
     * 只限制机台驱动竞争入口，不删除其它机台，确保模具占用等全局硬约束仍可读取完整状态。</p>
     */
    private Set<String> newSpecMachineResourceScopeCodeSet = new LinkedHashSet<>();
    /**
     * 旧模具清洗计划兼容列表；干冰/喷砂清洗排程不再使用该列表作为来源
     */
    private List<LhMouldCleanPlan> cleaningPlanList = new ArrayList<>();
    /**
     * 因有可换模具而跳过的喷砂清洗计划, key=machineCode, value=计划清洗时间
     */
    private Map<String, Date> skippedSandblastCleaningMap = new HashMap<>();
    /**
     * 胎胚实时库存Map, key=embryoCode；始终保存原始库存，内部排产额度分摊不得回写到该原始库存口径
     */
    private Map<String, Integer> embryoRealtimeStockMap = new HashMap<>();

    /**
     * 结构胎胚最早可供硫化时间Map, key=structureName
     */
    private Map<String, Date> structureEarliestLhTimeMap = new HashMap<>();

    /** 后结构对应的有效供胚来源记录；时间与两个标识必须来自同一行。 */
    private Map<String, CxEmbryoLhTime> structureSwitchSourceMap =
            new LinkedHashMap<>(16);

    /** 同一次结构切换的已提交首班状态，键为后结构，生命周期限定本次配置快照。 */
    private Map<String, StructureSwitchRuntimeState> structureSwitchRuntimeMap = new LinkedHashMap<>(16);

    /** 逐班审计标量快照，不参与业务回滚；原窗口保存前汇总，班次9副本禁止写入。 */
    private Map<String, StructureSwitchShiftAuditEntry> structureSwitchShiftAuditMap = new LinkedHashMap<>(32);

    /** 独立窗口复制的已提交首班基线，原窗口为空；不得因副本清空结果而重置S0。 */
    private Map<String, StructureSwitchRuntimeState> structureSwitchBaselineRuntimeMap = Collections.emptyMap();

    /** 已落地结果对应的冻结切换计划；单控两侧可引用同一计划。 */
    private Map<LhScheduleResult, StructureSwitchPlan> structureSwitchResultPlanMap = new IdentityHashMap<>(16);

    /** 正式提交作用域内的冻结计划，仅供分量调用，退出提交作用域即恢复。 */
    private Map<String, StructureSwitchPlan> structureSwitchAttemptPlanMap = new LinkedHashMap<>(4);

    /** 仅在无供胚等待的选料基准预演中置位，不参与业务资源或首班状态。 */
    private boolean structureSwitchSelectionProbe;


    /**
     * 胎胚收尾标识Map, key=embryoCode, value=1-收尾/0-非收尾；以胎胚维度合并硫化余量后按主销参与情况判定
     */
    private Map<String, Integer> embryoEndingFlagMap = new HashMap<>();
    /**
     * 日完成量Map（按物料+产品状态+完成日期聚合）, key=materialCode_productStatus_finishDate(yyyy-MM-dd)
     */
    private Map<String, Integer> materialDayFinishedQtyMap = new HashMap<>();
    /**
     * 本月日完成量Map（按物料+产品状态+完成日期聚合）, key=materialCode_productStatus_finishDate(yyyy-MM-dd)，仅覆盖当前排程月份截至T-1
     */
    private Map<String, Integer> materialMonthDailyFinishedQtyMap = new HashMap<>();
    /**
     * 月累计完成量Map（按月计划所属月份统计，截至排程窗口T日前一日）, key=materialCode_productStatus
     */
    private Map<String, Integer> materialMonthFinishedQtyMap = new HashMap<>();
    /**
     * T日排程班次完成量Map, key=materialCode+产品状态, value=T日class1FinishQty按物料汇总值
     */
    private Map<String, Integer> materialScheDayFinishQtyMap = new HashMap<>();
    /**
     * 物料信息Map, key=materialCode
     */
    private Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
    /**
     * 胶囊规格分组Map, key=规格, value=归一化后的分组编码
     */
    private Map<String, String> capsuleSpecPeerMap = new HashMap<>();
    /**
     * 胶囊英寸分组Map, key=英寸, value=归一化后的分组编码
     */
    private Map<String, String> capsuleProSizePeerMap = new HashMap<>();
    /**
     * 胎胚描述对应物料数量Map, key=胎胚描述
     */
    private Map<String, Integer> embryoDescMaterialCountMap = new HashMap<>();
    /**
     * MES硫化在机信息Map, key=machineCode
     */
    private Map<String, LhMachineOnlineInfo> machineOnlineInfoMap = new HashMap<>();
    /**
     * 硫化定点机台Map, key=materialCode（表字段SPEC_CODE实际维护物料编码）
     */
    private Map<String, List<LhSpecifyMachine>> specifyMachineMap = new HashMap<>();
    /**
     * 硫化机胶囊已使用次数Map, key=machineCode
     */
    private Map<String, LhRepairCapsule> capsuleUsageMap = new HashMap<>();
    /**
     * 本批排程胶囊运行态使用次数，key=物理机台编码。
     * <p>初值取 {@link #capsuleUsageMap} 左右模次数最大值，后续按物理机台实际总产量累计；
     * 本批首次严格跨限后不重置，不回写胶囊维修表，下一批仍重新接受MES快照。</p>
     */
    private Map<String, Integer> capsuleRuntimeUsageMap = new LinkedHashMap<String, Integer>();
    /**
     * 已执行换胶囊的物理机台班次键集合，key=物理机台编码 + 工作日期 + 班次索引。
     * <p>用于定位本批首次严格跨限实际发生的班次，并为后置产能防回扩提供事实依据。</p>
     */
    private Set<String> capsuleReplacementShiftKeySet = new LinkedHashSet<String>();
    /**
     * 本批已处理胶囊上限的物理机台集合，key=物理机台编码。
     * <p>初始左右最大值已达到上限，或本批已执行首次严格跨限扣量时登记；集合内机台
     * 后续只累计物理总产量，不再重复扣量或备注。</p>
     */
    private Set<String> capsuleThresholdHandledMachineSet = new LinkedHashSet<String>();
    /**
     * 已换胶囊结果班次允许写入的最大计划量，key=物理机台班次键 + 结果业务键。
     * <p>首次换胶囊时记录扣减后的精确上限，供日标准收敛、班次重分配和补量复用，
     * 防止后置逻辑按理论班产重新补回固定损失，也避免重复查询时再次扣减。</p>
     */
    private Map<String, Integer> capsuleReplacementShiftCapacityLimitMap =
            new LinkedHashMap<String, Integer>();
    /**
     * 换胶囊时间占用窗口，key=物理机台编码+工作日期+班次序号。
     * <p>仅班次未满产的首次严格跨限写入；产能、开产与完工时间通过该窗口统一向后推进，
     * 不再追加固定扣减量，避免同一次换胶囊重复扣产能。</p>
     */
    private Map<String, CapsuleReplacementTimeWindowDTO> capsuleReplacementTimeWindowMap =
            new LinkedHashMap<String, CapsuleReplacementTimeWindowDTO>();
    /**
     * 硫化精度保养计划Map, key=machineCode
     */
    private Map<String, LhPrecisionPlan> maintenancePlanMap = new HashMap<>();
    /**
     * 当前批次待处理的精度计划有序列表。
     * <p>列表只包含数据源 daysToDue 不为空且进入预警窗口的未完成计划，统一按
     * daysToDue、计划日期、物理机台编码升序排列；原 maintenancePlanMap 继续供历史调用点查询。</p>
     */
    private List<LhPrecisionPlan> orderedMaintenancePlanList = new ArrayList<>();
    /**
     * 是否由本批精度中心统一决策。
     * <p>续作前登记，阻止逐SKU挂窗；续作最终收口后统一按优先级分配日期和每日额度。</p>
     */
    private boolean maintenancePreDecisionCompleted;
    /**
     * 中心决策时尚未形成真实收尾时间的物理机台。
     * <p>本轮不挂窗、不截断续作；后续滚动获得真实收尾后再由中心统一决策，逐SKU入口不得抢占额度。</p>
     */
    private Set<String> maintenanceDeferredPhysicalMachineCodeSet =
            new LinkedHashSet<String>(8);
    /**
     * 排程年度精准计划条数：machineCode -> 当年有效记录数。
     * <p>包含已完成与未完成计划，仅用于“一机一年一条”完整性告警；实际排程仍只读取
     * maintenancePlanMap 中未完成且实际完成时间为空的计划。</p>
     */
    private Map<String, Integer> annualMaintenancePlanCountMap = new HashMap<>();
    /**
     * 特殊物料清单配置列表
     */
    private List<LhSpecialMaterialBom> specialMaterialBomList = new ArrayList<>();
    /**
     * 特殊物料分类Map, key=物料编码, value=分类集合
     */
    private Map<String, Set<String>> specialMaterialCategoryByMaterialCode = new HashMap<>();
    /**
     * 特殊物料分类Map, key=结构名称, value=分类集合
     */
    private Map<String, Set<String>> specialMaterialCategoryByStructureName = new HashMap<>();
    /**
     * SKU与示方书关系Map, key=materialCode
     */
    private Map<String, MdmSkuConstructionRef> skuConstructionRefMap = new HashMap<>();
    /**
     * SKU与示方书关系Map(复合key), key=materialCode + "::" + trialStatus
     */
    private Map<String, MdmSkuConstructionRef> skuConstructionRefCompositeKeyMap = new HashMap<>();

    // ========== 中间计算结果(S4.3) ==========

    /**
     * 前日排程结果列表(修正后)
     */
    private List<LhScheduleResult> previousScheduleResultList = new ArrayList<>();
    /**
     * 业务目标日前一日排程结果列表，仅供新增历史欠产跳过规则兜底判断
     */
    private List<LhScheduleResult> targetPreviousScheduleResultList = new ArrayList<>();
    /**
     * 当前排程目标日上一轮排程结果（用于硫化示方历史保护）
     */
    private List<LhScheduleResult> previousCureFormulaResultList = new ArrayList<>();
    /**
     * 业务目标日前一日模具交替计划列表，仅供独立的前次交替计划复用阶段使用。
     * <p>该列表固定按 {@link #scheduleTargetDate} 前一日查询，与前日排程结果加载的
     * 窗口起点口径互相隔离。</p>
     */
    private List<LhMouldChangePlan> historicalReverseMouldChangePlanList = new ArrayList<>();

    /** 前日交替强制下机快照；只影响事件时刻之后的资源。 */
    private Map<String, PreviousAlternatePlanReleaseEvent> previousAlternateReleaseEventMap = new LinkedHashMap<>(16);
    /** 已实际退出的机台时刻，包含明确允许的等台数承载置换。 */
    private Map<String, Date> previousAlternateReleasedMachineTimeMap = new LinkedHashMap<>(16);
    /** 已退出物料状态键，供新开机需求计数使用，不修改生产事实索引。 */
    private Map<String, String> previousAlternateReleasedSkuKeyMap = new LinkedHashMap<>(16);
    /** 历史动作与最终结果精确关联，同料同模具也必须生成交替计划。 */
    private Map<LhScheduleResult, LhMouldChangePlan> previousAlternateResultPlanMap = new IdentityHashMap<>(16);
    /** 当前指定交替指令，只在一次预演和正式提交的作用域内存在。 */
    private PreviousAlternatePlanReleaseEvent activePreviousAlternateEvent;
    /** 仅标识历史关联预演日志，不参与排程决策。 */
    private boolean previousAlternateGroupPreview;
    /** 同日关联预演冻结的目标模具，仅在重放提交作用域内使用。 */
    private Map<String, List<String>> previousAlternatePlannedMouldMap = new LinkedHashMap<>(16);
    /** 仅历史释放衍生候选的真实可用时刻，不限制同SKU其他正常候选。 */
    private Map<SkuScheduleDTO, Date> previousAlternateCandidateAvailableTimeMap = new IdentityHashMap<>(16);

    /**
     * 当前预演或提交是否为指定的历史交替动作。
     * @param sku 待排物料
     * @param machineCode 运行态机台
     * @return 是否命中当前动作
     */
    public boolean isPreviousAlternateAction(SkuScheduleDTO sku, String machineCode) {
        return Objects.nonNull(activePreviousAlternateEvent) && Objects.nonNull(sku)
                && StringUtils.equals(activePreviousAlternateEvent.getMachineCode(), machineCode)
                && StringUtils.equals(activePreviousAlternateEvent.getPlan().getAfterMaterialCode(), sku.getMaterialCode());
    }

    /**
     * 获取当前业务日已经退出的同物料物理机台，用于承载需求扣除。
     * @param sku 当前候选
     * @param businessDate 需求计算日期
     * @return 已释放的物理机台，历史生产记录保持不变
     */
    public Set<String> resolvePreviousAlternateReleasedMachines(SkuScheduleDTO sku, LocalDate businessDate) {
        Set<String> machines = new LinkedHashSet<>(4);
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus());
        previousAlternateReleasedMachineTimeMap.forEach((machineCode, time) -> {
            if (StringUtils.equals(skuKey, previousAlternateReleasedSkuKeyMap.get(machineCode))
                    && !time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isAfter(businessDate)
                    && !this.hasPreviousAlternateReplacementResult(sku, machineCode, time, businessDate)) {
                machines.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
            }
        });
        return machines;
    }

    /**
     * 原机台重新上机同物料后属于新的有效承载，不能继续沿用旧释放排除。
     * @param sku 待判断物料
     * @param machineCode 原运行态机台
     * @param releasedTime 原下机时刻
     * @param businessDate 当前业务日
     * @return 是否已有本日有效重新上机结果
     */
    private boolean hasPreviousAlternateReplacementResult(SkuScheduleDTO sku, String machineCode,
            Date releasedTime, LocalDate businessDate) {
        return scheduleResultList.stream()
                .filter(result -> StringUtils.equals(machineCode, result.getLhMachineCode()))
                .filter(result -> StringUtils.equals(sku.getMaterialCode(), result.getMaterialCode())
                        && StringUtils.equals(sku.getProductStatus(), result.getProductStatus()))
                .filter(result -> Objects.nonNull(result.getMouldChangeStartTime())
                        && !result.getMouldChangeStartTime().before(releasedTime))
                .anyMatch(result -> scheduleWindowShifts.stream()
                        .filter(shift -> businessDate.equals(shift.getWorkDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate()))
                        .anyMatch(shift -> {
                            Integer qty = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                            return Objects.nonNull(qty) && qty > 0;
                        }));
    }

    /**
     * 按天换活字块机台反选指令（S4.5 新增排产当天有效）。
     * <p>由每天正常资源竞争阶段开始前的“换活字块检测 + 机台反选物料”生成，
     * 只保存机台→物料配对与对账信息，不复制换活字块时间；当天正常竞争阶段结束统一结算并清空，
     * 次日按最新机台运行态重新检测。</p>
     */
    private List<DayTypeBlockReverseSelectionDirective> dayTypeBlockReverseSelectionDirectiveList =
            new ArrayList<DayTypeBlockReverseSelectionDirective>();
    /**
     * 按天换活字块反选机台预留，key=机台编码，value=物料+产品状态复合键。
     * <p>预留只对当天正常资源竞争阶段生效：命中物料已前置到当天工作队列并优先尝试预留机台，
     * 其他物料不会先于它占用该机台；命中物料成功落地或该阶段结束时立即释放，
     * 避免机台被重复锁定或跨日残留。</p>
     */
    private Map<String, String> dayTypeBlockReverseSelectedSkuKeyMap =
            new LinkedHashMap<String, String>(8);
    /**
     * SKU按结构归集, key=structureName, value=SKU排程DTO列表
     */
    private Map<String, List<SkuScheduleDTO>> structureSkuMap = new LinkedHashMap<>();
    /**
     * 结构最低机台规则使用的全量结构SKU快照。
     * <p>该快照在S4.3按现有结构分组一次性冻结，不受后续待排结构视图出队影响；
     * 结构收尾对齐规则统一从该快照解析结构归属，并比较待排SKU与候选机台前物料的结构。</p>
     */
    private Map<String, List<SkuScheduleDTO>> structureMinMachineSkuSnapshotMap = new LinkedHashMap<>();
    /**
     * 结构最低硫化机台数，key=结构名称，value=周期结构配置或常规结构工厂参数解析值
     */
    private Map<String, Integer> structureMinVulcanizingMachineMap = new LinkedHashMap<>();
    /**
     * S4.4 共用胎胚收尾均衡可调整物理机台快照，用于过程对账和最终未均衡原因分类。
     * <p>只登记仍满足均衡适用范围的机台，不含非共用胎胚或不足两台组内的机台。</p>
     */
    private Set<String> sharedEmbryoEndingBalanceEligibleMachineCodeSet =
            new LinkedHashSet<String>(8);
    /**
     * 结构班次在机机台统计缓存（内存态，不落库）。
     * <p>S4.4换活字块前基于续作稳定结果构建并随换活字块结果增量更新，S4.5新增选机前
     * 再按续作+换活字块最终结果重建；结构收尾对齐和全部提前生产入口共用同一实例。</p>
     */
    private StructureShiftInMachineIndex structureShiftInMachineIndex;
    /**
     * 业务日期 -> 产品结构 -> 计划硫化机台数，来源于月计划统计表 dayN.maxLhMachines
     */
    private Map<LocalDate, Map<String, Integer>> structurePlanMachineCountMap =
            new LinkedHashMap<LocalDate, Map<String, Integer>>(4);
    /**
     * 业务日期 -> 产品结构 -> 当天提前生产资格快照。
     * <p>资格只按当天最后一个班次生成一次；S4.4换活字块、S4.5新增和跨日在机续排
     * 共同读取该快照，禁止因候选实际落到早班或中班而重复执行结构机台数判断。</p>
     */
    private Map<LocalDate, Map<String, StructureEarlyProductionAdmission>>
            structureEarlyProductionAdmissionMap =
            new LinkedHashMap<LocalDate, Map<String, StructureEarlyProductionAdmission>>(4);
    /**
     * 业务日期 -> 提前生产判断日志采集器。
     * <p>采集器只保存日志标量快照，S4.4 换活字块和 S4.5 新增排产共用同一份内存态日志上下文，
     * 最终仍通过既有过程日志批量落库，不参与任何排程决策。</p>
     */
    private Map<LocalDate, EarlyProductionDecisionLogCollector>
            earlyProductionDecisionLogCollectorMap =
            new LinkedHashMap<LocalDate, EarlyProductionDecisionLogCollector>(4);
    /**
     * 业务日期 -> 产品结构 -> 已排硫化机台运行态编码集合。
     * <p>集合保留 KxxxxL/KxxxxR 原码，保证单侧结果回滚时不会误删仍在生产的配对侧；
     * 结构计数和候选存在性判断时再统一按 Kxxxx 物理机台去重。</p>
     */
    private Map<LocalDate, Map<String, Set<String>>> structureScheduledMachineCodeMap =
            new LinkedHashMap<LocalDate, Map<String, Set<String>>>(4);
    /**
     * 业务日期 -> 物料状态复合键 -> 已排硫化机台编码集合，用于SKU级机台数判断
     */
    private Map<LocalDate, Map<String, Set<String>>> skuScheduledMachineCodeMap =
            new LinkedHashMap<LocalDate, Map<String, Set<String>>>(4);
    /**
     * 业务日期 -> 物料状态复合键 -> 当月截至前一日累计欠产量。
     * <p>提前生产按正在处理的业务日读取，禁止固定沿用窗口 T 日的历史欠产快照。</p>
     */
    private Map<LocalDate, Map<String, Integer>> monthlyHistoryShortageQtyMap =
            new LinkedHashMap<LocalDate, Map<String, Integer>>(4);
    /**
     * 续作SKU列表，来源于 MES 在机/前批次状态，S4.4 优先排产
     */
    private List<SkuScheduleDTO> continuousSkuList = new ArrayList<>();
    /**
     * 新增SKU列表，续作和换活字块未消费完的 SKU 会继续保留到 S4.5 新增链路
     */
    private List<SkuScheduleDTO> newSpecSkuList = new ArrayList<>();
    /**
     * 试制/量试虚拟机台兜底候选快照。
     * <p>S4.3 保留无日计划但有硫化余量的试制/量试，S4.5 再合并参数拦截前的正常新增候选；
     * S4.5.3 只读取该快照核对实际机台已排量和剩余硫化余量。该集合不写入机台资源池，
     * 也不持久化虚拟机台主数据。</p>
     */
    private List<SkuScheduleDTO> trialVirtualMachineCandidateList = new ArrayList<>();
    /**
     * 班次9独立后置计划候选快照。
     * <p>在真实新增排产和日计划调整排产消费候选前登记，仅作为后置服务恢复候选全集的只读来源；
     * 班次9实际可排量必须重新读取全部前置阶段完成后的生产剩余账本，禁止直接使用快照中的旧余量。</p>
     */
    private List<SkuScheduleDTO> nextShiftNewPlanCandidateList = new ArrayList<>();
    /** 班次9候选最终生效日期池，按物料和产品状态记录，不保存前置阶段数量。 */
    private Map<String, LocalDate> nextShiftNewPlanPoolDateMap = new LinkedHashMap<>(16);
    /** 仅班次9独立副本启用：沿用前置最终日期池，不重新归池或登记原窗口候选。 */
    private boolean isolatedNextShiftPlan;

    /**
     * 本月历史欠产向当前排程窗口传导的数量，key=materialCode_productStatus
     */
    private Map<String, Integer> carryForwardQtyMap = new HashMap<>();
    /**
     * 满班补齐超排量累加器，key=materialCode_productStatus，供最终汇总日志使用
     */
    private Map<String, Integer> skuShiftFillOverQtyMap = new LinkedHashMap<>();
    /**
     * SKU实际排产剩余账本，key=materialCode_productStatus；不同产品状态独立扣减
     */
    private Map<String, Integer> skuProductionRemainingQtyMap = new LinkedHashMap<>();
    /**
     * SKU实际排产目标账本，key=materialCode_productStatus。
     * <p>与剩余账本配套记录同一业务目标，用于收尾目标量重复同步时保留续作、新增、
     * 换活字块已经消费的数量，禁止后续阶段把已扣减数量重新加回。</p>
     */
    private Map<String, Integer> skuProductionTargetQtyMap = new LinkedHashMap<>();
    /**
     * 胎胚库存消费账本，key=embryoCode + "_" + T日业务日期；用于胎胚收尾T日硬目标扣减
     */
    private Map<String, EmbryoStockConsumeLedger> embryoStockConsumeLedgerMap = new LinkedHashMap<>();
    /**
     * 胎胚库存SKU级内部分摊额度，key=materialCode_productStatus；组级总量仍按胎胚账本控制
     */
    private Map<String, Integer> embryoStockSkuQuotaMap = new LinkedHashMap<>();
    /**
     * 命中胎胚库存T日硬目标的物料状态复合键集合，用于结果班次量按库存账本奇偶原样裁剪
     */
    private Set<String> embryoStockHardTargetMaterialSet = new LinkedHashSet<>();
    /**
     * 共用胎胚收尾错峰降模释放候选原收尾班次快照，使用对象身份避免结果行字段被清零后丢失释放来源
     */
    private Map<LhScheduleResult, Integer> sharedEmbryoEndingStaggerReleaseShiftIndexMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /**
     * 共用胎胚收尾错峰降模释放候选原班次计划量快照，用于选中后延时恢复原班次收尾产量
     */
    private Map<LhScheduleResult, Integer> sharedEmbryoEndingStaggerReleaseShiftQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /**
     * 共用胎胚收尾错峰后延允许超目标量，供严格收口、账本裁剪和校验识别“错峰补量”例外
     */
    private Map<LhScheduleResult, Integer> sharedEmbryoEndingStaggerAllowedOverQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /**
     * 主销/常规SKU收尾补满允许超目标量，供严格收口、账本裁剪和校验识别“补满夜班”例外
     */
    private Map<LhScheduleResult, Integer> endingFillAllowedOverQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /** 前置降模冻结的余量收尾组，后续计算只消费此集合。 */
    private List<ContinuationRemainderFinishGroup> continuationRemainderFinishGroups =
            new ArrayList<>(16);
    /** 同料多状态专用链锁定的来源键，传递给最终补偿收口。 */
    private Set<String> continuationFinishLockedFormalSkuKeys = new LinkedHashSet<>(4);
    /** 已由新服务接管的单机及多机结果身份，阻止旧补量、均衡与保存前归整。 */
    private Set<LhScheduleResult> continuationSurplusEndingAllocatedResults =
            Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>(16));
    /** 余量收尾成功提交后的轻量快照，供S4.6只读复核节点与正式交替，不能反向修改后料。 */
    private Map<LhScheduleResult, ContinuationEndingAllocationSnapshot> continuationSurplusEndingSnapshotMap =
            new IdentityHashMap<LhScheduleResult, ContinuationEndingAllocationSnapshot>(16);
    /**
     * SKU收尾补满动作前的机台结果基准量，用于多机台同SKU组级允许超量重算。
     * <p>键为结果对象身份，值是该机台结果在本次收尾补满前的计划总量；
     * 组级重算时按“最终量-补满前量”识别各机台实际保留的补满新增量。</p>
     */
    private Map<LhScheduleResult, Integer> endingFillBeforeQtyMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /**
     * S4.5当前待排正规新增SKU数量，供选机阶段判断普通机台让位规则
     */
    private int pendingFormalNewSpecSkuCount;
    /**
     * S4.5当前待排试制新增SKU数量，供单控机台内部资源竞争判断
     */
    private int pendingTrialNewSpecSkuCount;
    /**
     * S4.5当前待排量试新增SKU数量，供单控机台内部资源竞争判断
     */
    private int pendingMassTrialNewSpecSkuCount;
    /**
     * S4.5当前待排小批量新增SKU数量，供单控机台内部资源竞争判断
     */
    private int pendingSmallBatchNewSpecSkuCount;
    /**
     * 新增SKU最近一次选机是否被单控/普通机台让位规则清空候选，使用对象身份避免同物料编码互相覆盖
     */
    private Map<SkuScheduleDTO, Boolean> newSpecTypeRuleBlockedMap = new IdentityHashMap<>();
    /**
     * 新增SKU提前生产准入结果，供选机和首日排产判断识别提前生产场景，使用对象身份避免同物料编码互相覆盖
     */
    private Map<SkuScheduleDTO, Boolean> newSpecEarlyProductionAllowedMap = new IdentityHashMap<>();
    /**
     * 当前提前生产阶段的 SKU 中心化运行视图。
     * <p>使用对象身份作为 key，避免同物料不同产品状态或补偿副本共享错误的临时日计划账本。</p>
     */
    private Map<SkuScheduleDTO, EarlyProductionRuntimePlan> earlyProductionRuntimePlanMap =
            new IdentityHashMap<SkuScheduleDTO, EarlyProductionRuntimePlan>();
    /**
     * 新增SKU进入S4.5时是否命中结构五天内收尾层级快照，使用对象身份避免SKU出队后判定漂移
     */
    private Map<SkuScheduleDTO, Boolean> newSpecSingleControlStructureEndingLayerMap = new IdentityHashMap<>();
    /**
     * 单控模式初始目标量快照，key=materialCode_productStatus；S4.3结束时冻结，后续禁止随剩余量变化
     */
    private Map<String, Integer> singleControlInitialTargetQtyMap = new LinkedHashMap<>();
    /**
     * 单控模式快照，key=materialCode_productStatus；统一供新增、续作、换活字块、降模和校验消费
     */
    private Map<String, SingleControlMachineModeEnum> singleControlModeSnapshotMap = new LinkedHashMap<>();
    /**
     * 单控模式快照是否已完成初始化；完成后禁止再次按动态运行态覆盖
     */
    private boolean singleControlModeSnapshotInitialized;
    /**
     * 续作结果日额度账本是否已完成最终同步，防止同一上下文重复扣账
     */
    private boolean continuousDailyQuotaSynced;
    /**
     * 续作首日/窗口无计划释放的机台集合，仅用于S4.5选机降优先级，不代表禁止生产
     */
    private Set<String> releasedContinuousMachineCodeSet = new LinkedHashSet<>();
    /**
     * 续作阶段因“仅喷砂清洗”主动下机的实际清洗窗口，key=运行态机台编码。
     * <p>该快照同时承载三项运行态事实：旧SKU及机台从cleanStartTime释放、原模具到cleanEndTime后
     * 才能重新参与资源匹配、剩余需求必须转入后续换活字块/新增排产。喷砂与其他停机重叠时禁止写入。</p>
     */
    private Map<String, MachineCleaningWindowDTO> onlySandBlastContinuationReleaseWindowMap =
            new LinkedHashMap<String, MachineCleaningWindowDTO>(4);
    /** 续作仅喷砂统一处置事件，保留从机台有效停机窗口移出的清洗事实。 */
    private Map<String, MachineCleaningWindowDTO> continuationSandBlastWindowMap =
            new LinkedHashMap<String, MachineCleaningWindowDTO>(4);
    /** 仅喷砂实际下机时仍需转入后续排产的真实余量，key=运行态机台编码。 */
    private Map<String, Integer> onlySandBlastContinuationRemainingQtyMap =
            new LinkedHashMap<String, Integer>(4);
    /**
     * 已按降模规则释放过续作机台的物料集合，避免后续补偿链路把降模机台重新补回
     */
    private Set<String> reducedContinuationGroupKeySet = new LinkedHashSet<>();
    /**
     * 续作逐日降模分组最后释放机台的业务日，key=物料+产品状态复合键，value=最后一次真正下机的业务日。
     * <p>补偿增机判断必须从该业务日起重新评估保留机台的 dayN 节奏，不能把释放日前的高计划日
     * 继续按最终机台数计算，避免“先降模释放、再补偿加回”的机台回流重叠。</p>
     */
    private Map<String, LocalDate> reducedContinuationGroupLastReleaseDateMap =
            new LinkedHashMap<String, LocalDate>(4);
    /**
     * 续作降模下机机台对应的前物料 SKU 快照。
     * <p>第一层 key=机台编码，第二层 key=降模前物料编码，value=实际触发降模的来源 SKU。
     * 该快照只记录续作降模规则实际选出的下机机台，不包含窗口无计划、首日无计划、收尾小余量跳过等
     * 其他释放原因。S4.6 使用来源 SKU 精确读取“物料+产品状态”的本次排程剩余账本，判断前物料能否在
     * 本次排程中收尾；该快照本身不参与选机、排量或余量扣减。</p>
     */
    private Map<String, Map<String, SkuScheduleDTO>> reducedContinuationMachineBeforeSkuMap =
            new LinkedHashMap<String, Map<String, SkuScheduleDTO>>(8);
    /**
     * 已按降模规则只保留单台续作机台的分组集合，避免后续补偿链路把已释放机台重新补回
     */
    private Set<String> singleMachineReducedContinuationGroupKeySet = new LinkedHashSet<>();
    /**
     * 续作收尾小余量释放后可优先进入换活字块匹配的机台集合
     */
    private Set<String> typeBlockReleasedContinuousMachineCodeSet = new LinkedHashSet<>();
    /**
     * 首日无计划但后续有计划的续作释放机台集合，供S4.4/S4.5稳定识别占位结果，不受后续账本扣减影响
     */
    private Set<String> firstDayNoPlanReleasedContinuousMachineCodeSet = new LinkedHashSet<>();
    /**
     * 续作停产保机日期，key=机台编码，value=该机台存在停产保机班次且仍保持原SKU和模具占用的业务日集合。
     * <p>该状态只属于本次排程运行态，不代表机台释放，也不参与续作降模END_TYPE判断。</p>
     */
    private Map<String, Set<LocalDate>> continuousStopHoldDateMap =
            new LinkedHashMap<String, Set<LocalDate>>(8);
    /**
     * 续作停产保机实际班次，key=机台编码，value=本次窗口内必须置零的班次序号集合。
     * <p>首个保机业务日只登记早班、中班；连续保机后续日登记夜班、早班、中班，
     * 使排量、后处理和结构在机统计共享同一份精确班次边界。</p>
     */
    private Map<String, Set<Integer>> continuousStopHoldShiftIndexMap =
            new LinkedHashMap<String, Set<Integer>>(8);
    /**
     * 当前仍处于停产保机占用的机台集合。
     * <p>历史保机日期保留在continuousStopHoldDateMap；计划恢复生产或后续真正降模后会从本集合移除，
     * 使候选过滤只约束当前仍被原SKU占用的机台。</p>
     */
    private Set<String> activeContinuousStopHoldMachineCodeSet = new LinkedHashSet<String>(8);
    /**
     * 曾停产保机、但后续业务日重新判断后已真正降模的机台集合。
     * <p>历史保机日期仍需保留用于班次清零，但这些机台在真实释放边界后可以重新进入后续资源候选。</p>
     */
    private Set<String> releasedContinuousStopHoldMachineCodeSet = new LinkedHashSet<String>(8);
    /**
     * 真正降模机台的最后允许生产班次，key=机台编码，value=本窗口最后允许保留正计划量的班次序号。
     * <p>该边界来自真实降模决策结果，供日标准收敛、收尾补量和尾量归集后统一清理释放边界后的误补量。</p>
     */
    private Map<String, Integer> continuousReducedMachineReleaseBoundaryShiftIndexMap =
            new LinkedHashMap<String, Integer>(8);
    /**
     * 运行态结果来源SKU映射，使用对象身份避免结果行可变字段影响Map命中，供后置校验回到原始日计划账本
     */
    private Map<LhScheduleResult, SkuScheduleDTO> scheduleResultSourceSkuMap = new IdentityHashMap<>();
    /**
     * 新增 SKU 按业务日真实命中的首次选机顺序。
     * <p>外层使用对象身份区分同物料不同运行态 SKU；内层 key 为 T 日偏移、value 为当天稳定顺序。
     * 同一 SKU 同一天多机台拆量只保留首次顺序，候选机台失败重试不会重复递增。</p>
     */
    private Map<SkuScheduleDTO, Map<Integer, Integer>> newSpecRealtimeSelectionOrderMap =
            new IdentityHashMap<SkuScheduleDTO, Map<Integer, Integer>>();
    /**
     * 已回写新增选机实时快照字段的结果集合。
     * <p>使用结果对象身份区分同物料多机台结果，供同一 SKU 跨日再次命中时统一刷新
     * {@code SKU_REALTIME_SELECTION_ORDER}，不会把续作或 S4.4 换活字块结果误纳入新增专用字段。</p>
     */
    private Set<LhScheduleResult> newSpecRealtimeSnapshotResultSet =
            Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>());
    /**
     * 已正式提交的精度前插排结果集合。
     * <p>使用对象身份精确标记结果，保存前时间轴复核只能撤销真正的插排结果，
     * 不能把同机台在06:00前自然收尾的前SKU误识别为插排。</p>
     */
    private Set<LhScheduleResult> precisionPreInsertResultSet =
            Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>());
    /**
     * 精度前插排结果实际占用的首检均衡时间。
     * <p>仅记录真正消费早/中班首检均衡额度的新增规格主结果，供保存前最终撤销时精确释放。</p>
     */
    private Map<LhScheduleResult, Date> precisionPreInsertInspectionTimeMap =
            new IdentityHashMap<LhScheduleResult, Date>();
    /**
     * 精度前插排结果实际占用的换模均衡时间。
     * <p>只有新增规格换模成功占用早/中班换模名额时才登记；换活字块结果虽然也有切换开始时间，
     * 但没有消费换模名额，最终撤销时不得仅凭结果时间误减其他SKU的换模计数。</p>
     */
    private Map<LhScheduleResult, Date> precisionPreInsertMouldChangeTimeMap =
            new IdentityHashMap<LhScheduleResult, Date>();
    /**
     * 精度前插排结果占用的首检数量归属班次索引。
     * <p>换模和换活字块均会登记班次首检顺序，最终撤销时必须按原班次回退一次。</p>
     */
    private Map<LhScheduleResult, Integer> precisionPreInsertInspectionShiftIndexMap =
            new IdentityHashMap<LhScheduleResult, Integer>();
    /**
     * 前置指定组合与S4.5新增共用模具资源运行态，不反向裁剪续作结果
     */
    private MouldResourceContext mouldResourceContext;

    /** 前置指定组合已提交的在机绑定；后续日驱动直接接续同一结果与需求对象。 */
    private List<ActiveMachineBinding> preScheduledMachineBindingList = new ArrayList<ActiveMachineBinding>(16);

    /** 前置结果换下的旧模具最早释放时刻，防止后续从窗口首日开始排产时提前使用未来资源。 */
    private Map<String, Date> preScheduledMouldReleaseTimeMap = new LinkedHashMap<String, Date>(16);

    // ========== 机台分配状态 ==========

    /**
     * 机台排程DTO Map, key=machineCode
     */
    private Map<String, MachineScheduleDTO> machineScheduleMap = new LinkedHashMap<>();
    /**
     * 机台初始状态快照，供换模计划和回归校验使用
     */
    private Map<String, MachineScheduleDTO> initialMachineScheduleMap = new LinkedHashMap<>();
    /**
     * 机台剩余产能Map, key=machineCode, value=各班次剩余产能
     */
    private Map<String, int[]> machineShiftCapacityMap = new LinkedHashMap<>();
    /**
     * 班次运行态，key=班次索引 1～N（N≤8），承载开停产、工作日历和历史班次保护后的可排状态
     */
    private Map<Integer, ShiftRuntimeState> shiftRuntimeStateMap = new LinkedHashMap<>(8);
    /**
     * 本次排程解析后的班次窗口
     */
    private List<LhShiftConfigVO> scheduleWindowShifts = new ArrayList<>();
    /**
     * 班次排产管控，key=班次索引
     */
    private Map<Integer, ShiftProductionControlDTO> shiftProductionControlMap = new LinkedHashMap<>(8);
    /**
     * 机台已分配SKU Map, key=machineCode, value=已分配的排程结果
     */
    private Map<String, List<LhScheduleResult>> machineAssignmentMap = new LinkedHashMap<>();
    /**
     * 定点机台挤量预留切换开始时间, key=machineCode；用于续作非收尾给后续定点新增留出换模窗口
     */
    private Map<String, Date> specifyMachineReservedSwitchStartTimeMap = new LinkedHashMap<>();
    /**
     * 定点机台挤量预留物料编码, key=machineCode
     */
    private Map<String, String> specifyMachineReservedMaterialMap = new LinkedHashMap<>();
    /**
     * 每日模具切换计数, key=dateString, value=[早班切换数, 中班切换数]
     */
    private Map<String, int[]> dailyMouldChangeCountMap = new LinkedHashMap<>();

    /** 本次原窗口的历史限额快照；只读共享，不预占真实或模拟次数。 */
    private MouldChangeQuotaSnapshot mouldChangeQuotaSnapshot;
    /** 前置选机冻结的按时间下机需求，顺序与原下机优先级一致。 */
    private List<TimedMachineOffRequest> timedMachineOffRequests = new ArrayList<>(8);
    /** 已提交时间边界以结果身份索引，零量生产行移除后仍保留资源释放事实。 */
    private Map<LhScheduleResult, OffMachineDecision> timedMachineOffDecisionMap = new IdentityHashMap<>(8);

    /** @param result 原始结果 @return 是否已登记独立按时间下机需求 */
    public boolean hasTimedMachineOffRequest(LhScheduleResult result) {
        return timedMachineOffRequests.stream().anyMatch(request -> request.getProfile().getOriginals().stream()
                .anyMatch(original -> original == result));
    }

    /**
     * 获取物理机台尚待首次承接的按时间下机占用截止，供正式换模入口统一遵守。
     * @param machineCode 当前物理机台或单控侧编码
     * @return 已提交占用截止；不属于本规则或已经正式承接时为空
     */
    public Date getTimedMachineOffBoundary(String machineCode) {
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        Date boundary = timedMachineOffDecisionMap.entrySet().stream()
                .filter(entry -> StringUtils.equals(physicalCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(entry.getKey().getLhMachineCode())))
                .map(entry -> entry.getValue().getOccupancyEndTime()).max(Date::compareTo).orElse(null);
        if (Objects.isNull(boundary)) {
            return null;
        }
        // 只约束此次下机后的首次承接；后料已正式生产后，其再次收尾仍沿用自己的业务类型。
        boolean handedOver = scheduleResultList.stream()
                .filter(result -> !timedMachineOffDecisionMap.containsKey(result))
                .filter(result -> StringUtils.equals(physicalCode,
                        LhSingleControlMachineUtil.resolvePhysicalMachineCode(result.getLhMachineCode())))
                .anyMatch(result -> Objects.nonNull(result.getMouldChangeStartTime())
                        && !result.getMouldChangeStartTime().before(boundary)
                        && ShiftFieldUtil.resolveScheduledQty(result) > 0);
        return handedOver ? null : boundary;
    }

    /**
     * 生产日前跨日准备换模事件，key=物理机台编码+切换开始时间戳。
     * <p>事件仍计入每日15次硬上限，但最终复核早8/中7参考分布时需单独识别，
     * 避免把已确认贴近下一业务日生产下限的合法准备误报为均衡异常。</p>
     */
    private Set<String> crossDayPreparationMouldChangeEventKeySet = new LinkedHashSet<>();
    /**
     * 同胎胚换模班次占用, key=胎胚编码, value=已安排换模班次索引集合
     */
    private Map<String, Set<Integer>> greenTireChangeoverShiftMap = new LinkedHashMap<>();
    /**
     * 本月待排物料胎胚共用关系, key=materialCode, value=true表示与其他待排物料共用胎胚
     */
    private Map<String, Boolean> materialSharedEmbryoMap = new LinkedHashMap<>();
    /**
     * 当前仍有效参与排产的胎胚SKU集合, key=embryoCode, value=有效待排物料编码列表
     */
    private Map<String, List<String>> activeEmbryoSkuMap = new LinkedHashMap<>();
    /**
     * 共用胎胚剔除零余量SKU后动态转为单胎胚收尾的物料编码集合
     */
    private Set<String> dynamicSingleEmbryoEndingMaterialSet = new LinkedHashSet<>();
    /**
     * 换模/换活字块日上限阻塞原因, key=materialCode, value=未排原因
     */
    private Map<String, String> mouldChangeLimitBlockedReasonMap = new LinkedHashMap<>();
    /**
     * 每日首检计数, key=dateString, value=[早班首检数, 中班首检数]
     */
    private Map<String, int[]> dailyFirstInspectionCountMap = new LinkedHashMap<>();
    /**
     * 班次首检数量顺序计数, key=业务日期#班次索引, value=已计入首检数量的机台数
     */
    private Map<String, Integer> shiftFirstInspectionCountMap = new LinkedHashMap<>(8);
    /**
     * 每日精度保养计数, key=dateString, value=已安排保养机台数
     */
    private Map<String, Integer> dailyMaintenanceCountMap = new LinkedHashMap<>();
    /**
     * 每日已占用保养额度的物理机台集合，key=dateString，value=物理机台编码集合。
     * <p>单控 L/R 两侧属于同一物理机台，只能在集合中登记一次；清除运行时保养窗口时也通过
     * 该集合释放额度，避免仅递减数字导致重复占用或误释放其他机台额度。</p>
     */
    private Map<String, Set<String>> dailyMaintenancePhysicalMachineSetMap = new LinkedHashMap<>();
    /**
     * 已记录的精度保养就绪时间顺延日志键集合。
     * <p>键由“机台 + 原就绪时间 + 最早开产时间”组成。同一候选在选机、产能预演和最终排产阶段
     * 可能被重复计算，本集合只抑制完全相同的过程日志，不改变任何机台时间和排程判断。</p>
     */
    private Set<String> maintenanceResumeDelayLogKeySet = new LinkedHashSet<>();
    /**
     * S4.4 完成后冻结的续作在机结果快照。
     * <p>使用对象身份保存，只允许 S4.5.1 从这些真实续作结果中选择被置换机台；S4.5 新增排产、
     * 换活字块及后续生成的结果即使落在同一物理机台，也不得被特殊材料置换链删除或截断。</p>
     */
    private Set<LhScheduleResult> specialMaterialContinuationResultSnapshot =
            Collections.newSetFromMap(new IdentityHashMap<LhScheduleResult, Boolean>());
    /**
     * 特殊材料指定机台排产指令中的目标机台。
     * <p>仅在 S4.5.1 单台置换提交期间临时设置，新增排产主链据此只校验和尝试该机台；
     * 提交完成或失败后必须立即清空，禁止影响普通 S4.5 新增排产。</p>
     */
    private String specialMaterialSpecifiedMachineCode;
    /**
     * 特殊材料指定机台排产指令中的“物料+产品状态”复合键
     */
    private String specialMaterialSpecifiedSkuKey;
    /**
     * 特殊材料指定机台排产允许的最早换模时间
     */
    private Date specialMaterialEarliestSwitchTime;
    /**
     * 特殊材料置换成功记录。
     * <p>S4.6 按实际换模结果精确追加备注，不再使用“机台编码 -> 备注”的粗粒度 Map。</p>
     */
    private List<SpecialMaterialSubstitutionRecord> specialMaterialSubstitutionRecordList = new ArrayList<>();
    /**
     * 共用模具联动置换临时排产指令。
     *
     * <p>只在 S4.5.1 的单候选预演或正式提交期间设置。新增主链通过该指令区分
     * “A 原机台无换模接管”和“B 携剩余模具重新选机”，调用结束后必须清空。</p>
     */
    private ScheduleSubstitutionDirective scheduleSubstitutionDirective;
    /**
     * 共用模具联动置换成功记录。
     *
     * <p>记录 A/B、原新机台、转交及迁移模具和完整时间轴，不新增数据库表；
     * 最终通过排程过程日志持久化审计信息。</p>
     */
    private List<SharedMouldSubstitutionRecord> sharedMouldSubstitutionRecordList =
            new ArrayList<SharedMouldSubstitutionRecord>();
    /**
     * 全量SKU排程信息索引Map，key=materialCode_productStatus，供后置阶段精确查找来源SKU
     */
    private Map<String, SkuScheduleDTO> allSkuScheduleDtoMap = new LinkedHashMap<>();
    /** S4.3清理待排SKU之前按物料汇总的硫化余量，避免历史计划后料状态误当作前料状态。 */
    private Map<String, Integer> initialMaterialSurplusQtyMap = new LinkedHashMap<>();
    /**
     * SKU减量清单索引集合，key=year+SEP+month+SEP+materialCode+SEP+productStatus（归一化）。S4.2批量加载，S4.3归集后统一过滤命中SKU
     */
    private Set<String> skuDecrementKeySet = new HashSet<>();
    /**
     * 已处理减量命中SKU去重集合，key=materialCode+SEP+productStatus+SEP+yearMonth，保证同一SKU多入口只写一次未排结果
     */
    private Set<String> decrementHandledSkuKeySet = new HashSet<>();


    // ========== 排程输出结果 ==========

    /**
     * 硫化排程结果列表
     */
    private List<LhScheduleResult> scheduleResultList = new ArrayList<>();
    /**
     * 硫化未排结果列表
     */
    private List<LhUnscheduledResult> unscheduledResultList = new ArrayList<>();
    /**
     * 本批次未排需求快照、原因事件及SKU身份绑定。
     * <p>该运行态只服务未排分类和诊断，不参与机台选择、排量或资源扣账。</p>
     */
    private UnscheduledResultRuntime unscheduledResultRuntime = new UnscheduledResultRuntime();
    /**
     * 模具交替计划列表
     */
    private List<LhMouldChangePlan> mouldChangePlanList = new ArrayList<>();
    /**
     * 排程日志列表
     */
    private List<LhScheduleProcessLog> scheduleLogList = new ArrayList<>();

    // ========== 流程控制 ==========

    /**
     * 是否中断排程
     */
    private boolean interrupted = false;
    /**
     * 中断原因
     */
    private String interruptReason;
    /**
     * 当前执行步骤
     */
    private String currentStep;
    /**
     * 校验错误信息集合
     */
    private List<String> validationErrorList = new ArrayList<>();
    /**
     * 校验错误明细（结构化，如模具禁用/缺失的详细信息）
     */
    private List<MouldValidationErrorDetail> validationErrorDetailList = new ArrayList<>();
    /**
     * 非阻断提示信息列表（如跨月排程时下月未定稿）。
     * <p>仅用于回传前端提示，不中断排程，本月排产正常继续。</p>
     */
    private List<String> warningMessageList = new ArrayList<>();
    /**
     * 优先级跟踪日志静默深度（局部搜索模拟分支时递增）
     */
    private int priorityTraceMuteDepth = 0;
    /**
     * 新增排产机台驱动提案预演深度。
     * <p>只用于静默Machine×SKU批量试算中的逐班明细日志，不改变任何排产判断。</p>
     */
    private int newSpecProposalPreviewDepth = 0;
    /**
     * 新增排产SKU选机日志次数，key=物料编码+产品状态。
     * <p>仅用于当前排程上下文内的过程日志编号，不参与候选过滤、选机排序和排产结果计算。</p>
     */
    private Map<String, Integer> newSpecMachineSelectionCountMap = new LinkedHashMap<String, Integer>(16);

    /**
     * 判断当前 SKU 是否命中特殊材料指定机台排产指令。
     *
     * @param sku 待排 SKU
     * @return true-当前 SKU 必须只尝试置换指令中的指定机台；false-走普通新增选机
     */
    public boolean isSpecialMaterialSpecifiedSku(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(specialMaterialSpecifiedSkuKey)) {
            return false;
        }
        return StringUtils.equals(specialMaterialSpecifiedSkuKey,
                MonthPlanDateResolver.buildMaterialStatusKey(sku.getMaterialCode(), sku.getProductStatus()));
    }

    /**
     * 判断当前 SKU 是否命中联动置换临时指令。
     *
     * @param sku 待排 SKU
     * @return true-复用新增主链执行置换；false-执行原新增排产逻辑
     */
    public boolean isScheduleSubstitutionSku(SkuScheduleDTO sku) {
        return Objects.nonNull(scheduleSubstitutionDirective)
                && scheduleSubstitutionDirective.matches(sku);
    }

    /**
     * 解析置换模式指定机台，兼容已有特殊材料指定机台指令。
     *
     * @param sku 待排 SKU
     * @return 指定机台编码；未指定时返回 null
     */
    public String resolveSubstitutionSpecifiedMachineCode(SkuScheduleDTO sku) {
        if (isScheduleSubstitutionSku(sku)) {
            return scheduleSubstitutionDirective.getSpecifiedMachineCode();
        }
        return isSpecialMaterialSpecifiedSku(sku)
                ? specialMaterialSpecifiedMachineCode : null;
    }

    /**
     * 解析置换模式允许的最早切换时间，兼容已有特殊材料置换链。
     *
     * @param sku 待排 SKU
     * @return 最早切换时间；普通新增返回 null
     */
    public Date resolveSubstitutionEarliestSwitchTime(SkuScheduleDTO sku) {
        if (isScheduleSubstitutionSku(sku)) {
            return scheduleSubstitutionDirective.getEarliestSwitchTime();
        }
        return isSpecialMaterialSpecifiedSku(sku)
                ? specialMaterialEarliestSwitchTime : null;
    }

    /**
     * 解析 B 迁移必须精确承接的续作截断尾量。
     *
     * @param sku 待排 SKU
     * @return 正截断尾量；普通新增、特殊材料置换和 A 接管均返回 0
     */
    public int resolveSubstitutionExactScheduleQty(SkuScheduleDTO sku) {
        if (!isScheduleSubstitutionSku(sku)
                || !scheduleSubstitutionDirective
                .isContinuationRelocation()) {
            return 0;
        }
        return Math.max(
                0, scheduleSubstitutionDirective
                        .getExactScheduleQty());
    }

    /**
     * 清空共用模具联动置换临时指令。
     *
     * <p>调用处必须放在 finally 中，确保预演失败、正式提交失败和异常分支均不会污染
     * 后续特殊材料兜底或 S4.6 结果校验。</p>
     */
    public void clearScheduleSubstitutionDirective() {
        scheduleSubstitutionDirective = null;
    }

    /**
     * 清空特殊材料指定机台排产指令。
     *
     * <p>该方法只清理 S4.5.1 临时指令，不清理续作结果快照和已成功置换记录。</p>
     */
    public void clearSpecialMaterialSpecifiedMachineDirective() {
        specialMaterialSpecifiedMachineCode = null;
        specialMaterialSpecifiedSkuKey = null;
        specialMaterialEarliestSwitchTime = null;
    }

    /**
     * 20260701+ 判断当前排程周期是否存在跨月
     * true 跨月 false 不跨月
     *
     * @return
     */
    public boolean isCrossMonthByProductionDateInfo() {
        List<Date> allProductionDateList = Lists.newArrayList(getAllProductionDateInfo());
        if (CollectionUtils.isEmpty(allProductionDateList)) {
            return false;
        }
        return SkuMonthPlanCalculator.isCrossMonthByProductionDateInfo(allProductionDateList);
    }

    /**
     * 下个月是否定稿
     *
     * @return
     */
    public boolean isNextMonthFinal() {
        Date planStartDate = null;
        if (null != lhMonthStartDayInfo) {
            planStartDate = lhMonthStartDayInfo.getPlanStartDate();
        }
        return null == planStartDate ? false : true;
    }

    /**
     * 获取起始天
     *
     * @return
     */
    public Date getPlanStartDate() {
        YearMonth productionYearMonth = MonthPlanSurplusCalculator.getProductionYearAndMonth(scheduleDate);
        Date monthStartDate = SkuMonthPlanCalculator.getDate(productionYearMonth.atDay(BigDecimal.ONE.intValue()));
        if (null == lhMonthStartDayInfo) {
            return monthStartDate;
        }
        Date planStartDate = lhMonthStartDayInfo.getPlanStartDate();
        if (null == planStartDate) {
            return monthStartDate;
        }
        return planStartDate;
    }

    /**
     * 获取硫化月起始日信息
     *
     * @return
     */
    public LhMonthStartDayResult getMonthStartInfo() {
        if (null == lhMonthStartDayInfo) {
            return LhMonthStartDayResult.EMPTY;
        }
        return lhMonthStartDayInfo;
    }

    /**
     * 20260701+ 当前所有排产日集合
     *
     * @return
     */
    public Set<Date> getAllProductionDateInfo() {
        Set<Date> allProductionDateSet = Sets.newHashSet();
        if (null != scheduleDate) {
            allProductionDateSet.add(scheduleDate);
        }
        if (null != scheduleTargetDate) {
            allProductionDateSet.add(scheduleTargetDate);
        }
        if (null != windowEndDate) {
            allProductionDateSet.add(windowEndDate);
        }
        return allProductionDateSet;
    }

    /**
     * 累加结构计划硫化机台数。
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @param machineCount   计划硫化机台数
     */
    public void addStructurePlanMachineCount(LocalDate productionDate, String structureName, int machineCount) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(structureName)) {
            return;
        }
        Map<String, Integer> structureMap = structurePlanMachineCountMap.computeIfAbsent(
                productionDate, key -> new LinkedHashMap<String, Integer>(8));
        Integer oldCount = structureMap.get(structureName);
        structureMap.put(structureName, Math.max(0, Objects.isNull(oldCount) ? 0 : oldCount)
                + Math.max(0, machineCount));
    }

    /**
     * 获取指定业务日、指定结构的计划硫化机台数。
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @return 计划硫化机台数
     */
    public int getStructurePlanMachineCount(LocalDate productionDate, String structureName) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(structureName)
                || CollectionUtils.isEmpty(structurePlanMachineCountMap)) {
            return 0;
        }
        Map<String, Integer> structureMap = structurePlanMachineCountMap.get(productionDate);
        if (CollectionUtils.isEmpty(structureMap)) {
            return 0;
        }
        Integer machineCount = structureMap.get(structureName);
        return Objects.isNull(machineCount) ? 0 : Math.max(0, machineCount);
    }

    /**
     * 获取指定业务日、指定结构已经固化的提前生产资格。
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @return 结构当天提前生产资格；尚未判断时返回null
     */
    public StructureEarlyProductionAdmission getStructureEarlyProductionAdmission(
            LocalDate productionDate,
            String structureName) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(structureName)
                || CollectionUtils.isEmpty(structureEarlyProductionAdmissionMap)) {
            return null;
        }
        Map<String, StructureEarlyProductionAdmission> structureAdmissionMap =
                structureEarlyProductionAdmissionMap.get(productionDate);
        return CollectionUtils.isEmpty(structureAdmissionMap)
                ? null : structureAdmissionMap.get(structureName);
    }

    /**
     * 固化指定结构当天唯一的提前生产资格。
     *
     * @param admission 结构当天提前生产资格
     */
    public void registerStructureEarlyProductionAdmission(
            StructureEarlyProductionAdmission admission) {
        if (Objects.isNull(admission) || Objects.isNull(admission.getBusinessDate())
                || StringUtils.isEmpty(admission.getStructureName())) {
            return;
        }
        Map<String, StructureEarlyProductionAdmission> structureAdmissionMap =
                structureEarlyProductionAdmissionMap.computeIfAbsent(
                        admission.getBusinessDate(),
                        key -> new LinkedHashMap<String, StructureEarlyProductionAdmission>(8));
        structureAdmissionMap.putIfAbsent(admission.getStructureName(), admission);
    }

    /**
     * 获取或创建指定业务日的提前生产判断日志采集器。
     *
     * @param businessDate 当前业务日期
     * @param dateOffset   相对窗口 T 日偏移
     * @return 业务日提前生产日志采集器
     */
    public EarlyProductionDecisionLogCollector getOrCreateEarlyProductionDecisionLogCollector(
            LocalDate businessDate,
            int dateOffset) {
        if (Objects.isNull(businessDate)) {
            return null;
        }
        return earlyProductionDecisionLogCollectorMap.computeIfAbsent(
                businessDate,
                key -> new EarlyProductionDecisionLogCollector(key, dateOffset));
    }

    /**
     * 清理提前生产判断日志采集器。
     *
     * <p>只清理内存态日志对象，不影响已经追加到 {@code scheduleLogList} 的过程日志实体。</p>
     */
    public void clearEarlyProductionDecisionLogCollectors() {
        earlyProductionDecisionLogCollectorMap.clear();
    }

    /**
     * 清空结构/SKU已排机台运行态。
     */
    public void clearScheduledMachineCountMaps() {
        structureScheduledMachineCodeMap.clear();
        skuScheduledMachineCodeMap.clear();
    }

    /**
     * 基于当前排程结果重建结构和 SKU 已排机台运行态。
     *
     * <p>S4.4 换活字块与 S4.5 新增排产都依赖该统计执行提前生产机台数门禁，
     * 必须在各阶段开始前纳入已经落地的续作、换活字块和新增结果。结构机台数在读取时
     * 统一按物理机台去重，单控 L/R 结果不会重复计数。目标日跨日准备结果还需读取
     * 运行态来源日期，使换模已完成但正产从下一夜班开始的机台仍计入来源日目标。</p>
     *
     * @param shifts 排程窗口班次
     * @return 实际登记的“结果业务日”数量
     */
    public int rebuildScheduledMachineCountMaps(List<LhShiftConfigVO> shifts) {
        StructureSwitchSchedulingPolicy.rebuildCommittedState(this);
        this.clearScheduledMachineCountMaps();
        if (CollectionUtils.isEmpty(scheduleResultList) || CollectionUtils.isEmpty(shifts)) {
            return 0;
        }
        int recordDateCount = 0;
        for (LhScheduleResult result : scheduleResultList) {
            if (Objects.isNull(result) || StringUtils.isEmpty(result.getLhMachineCode())) {
                continue;
            }
            Set<LocalDate> recordedDateSet = new LinkedHashSet<LocalDate>(3);
            for (LhShiftConfigVO shift : shifts) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())
                        || Objects.isNull(shift.getWorkDate())) {
                    continue;
                }
                Integer planQty = ShiftFieldUtil.getShiftPlanQty(
                        result, shift.getShiftIndex());
                if (Objects.nonNull(planQty) && planQty > 0) {
                    recordedDateSet.add(
                            SkuMonthPlanCalculator.getDate(shift.getWorkDate()));
                }
            }
            if (!recordedDateSet.isEmpty()
                    && Objects.nonNull(result.getSourceDayCrossDayPreparationDate())) {
                /*
                 * 目标日跨日准备结果从紧邻下一夜班才产生正计划量，不能只按正计划班次
                 * 重建已排机台；来源日已经完成选机、占模和换模，同样属于目标机台已落实。
                 */
                recordedDateSet.add(SkuMonthPlanCalculator.getDate(
                        result.getSourceDayCrossDayPreparationDate()));
            }
            for (LocalDate businessDate : recordedDateSet) {
                this.recordScheduledMachine(
                        businessDate, result.getStructureName(), result.getMaterialCode(),
                        result.getProductStatus(), result.getLhMachineCode());
            }
            recordDateCount += recordedDateSet.size();
        }
        return recordDateCount;
    }

    /**
     * 登记已排硫化机台。
     * <p>结构和 SKU 集合均保留运行态机台编码；结构计数时统一按物理机台去重，
     * SKU 计数同样按物理机台去重；保留侧别编码供模具分配、产能和释放使用。</p>
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @param materialCode   SKU物料编码
     * @param productStatus  产品状态
     * @param machineCode    机台编码
     */
    public void recordScheduledMachine(LocalDate productionDate,
                                       String structureName,
                                       String materialCode,
                                       String productStatus,
                                       String machineCode) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(machineCode)) {
            return;
        }
        if (StringUtils.isNotEmpty(structureName)) {
            this.recordMachine(
                    structureScheduledMachineCodeMap, productionDate, structureName, machineCode);
        }
        if (StringUtils.isNotEmpty(materialCode)) {
            // 已排统计与读取入口使用同一产品状态归一化规则，空状态统一按正规 S 处理。
            String normalizedProductStatus = StringUtils.isEmpty(productStatus)
                    ? FORMAL_PRODUCT_STATUS : productStatus;
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, normalizedProductStatus);
            this.recordMachine(skuScheduledMachineCodeMap, productionDate, skuKey, machineCode);
        }
    }

    /**
     * 移除指定业务日已登记的已排硫化机台。
     * <p>用于续作结果被释放边界置零后回滚补满登记的结构/SKU机台统计，
     * 避免后续同结构机台收尾补满被“结构机台数已达标”误拦。</p>
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @param materialCode   SKU物料编码
     * @param productStatus  产品状态
     * @param machineCode    机台编码
     */
    public void removeScheduledMachine(LocalDate productionDate,
                                       String structureName,
                                       String materialCode,
                                       String productStatus,
                                       String machineCode) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(machineCode)) {
            return;
        }
        if (StringUtils.isNotEmpty(structureName)) {
            this.removeMachine(
                    structureScheduledMachineCodeMap, productionDate, structureName, machineCode);
        }
        if (StringUtils.isNotEmpty(materialCode)) {
            // 与登记口径保持一致：空状态统一按正规 S 归一化后再移除
            String normalizedProductStatus = StringUtils.isEmpty(productStatus)
                    ? FORMAL_PRODUCT_STATUS : productStatus;
            String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(materialCode, normalizedProductStatus);
            this.removeMachine(skuScheduledMachineCodeMap, productionDate, skuKey, machineCode);
        }
    }

    /**
     * 获取指定业务日的动态历史欠产量。
     *
     * @param productionDate 当前业务日期
     * @param materialCode   物料编码
     * @param productStatus  产品状态
     * @return 当前月月初至业务日前一日的累计欠产量
     */
    public int getMonthlyHistoryShortageQty(LocalDate productionDate,
                                            String materialCode,
                                            String productStatus) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(materialCode)
                || CollectionUtils.isEmpty(monthlyHistoryShortageQtyMap)) {
            return 0;
        }
        Map<String, Integer> dateShortageMap = monthlyHistoryShortageQtyMap.get(productionDate);
        if (CollectionUtils.isEmpty(dateShortageMap)) {
            return 0;
        }
        // 月计划通常以 S 标识正规品，运行态 SKU 的空状态按项目既有口径归一化为 S 后再读取。
        String normalizedProductStatus = StringUtils.isEmpty(productStatus)
                ? FORMAL_PRODUCT_STATUS : productStatus;
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                materialCode, normalizedProductStatus);
        Integer shortageQty = dateShortageMap.get(skuKey);
        return Objects.isNull(shortageQty) ? 0 : Math.max(0, shortageQty);
    }

    /**
     * 登记当前提前生产阶段的中心化运行视图。
     *
     * @param sku         提前生产 SKU
     * @param runtimePlan 运行视图
     */
    public void registerEarlyProductionRuntimePlan(SkuScheduleDTO sku,
                                                   EarlyProductionRuntimePlan runtimePlan) {
        if (Objects.nonNull(sku) && Objects.nonNull(runtimePlan)) {
            earlyProductionRuntimePlanMap.put(sku, runtimePlan);
        }
    }

    /**
     * 登记试制/量试虚拟机台兜底候选。
     * <p>按“物料+产品状态”去重，保证S4.3无日计划过滤与S4.5参数拦截前合并候选时，
     * 同一SKU只分配一台虚拟机台。</p>
     *
     * @param sku 试制/量试候选
     * @return void
     */
    public void registerTrialVirtualMachineCandidate(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        String targetSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        for (SkuScheduleDTO candidate : trialVirtualMachineCandidateList) {
            if (Objects.isNull(candidate)) {
                continue;
            }
            String candidateSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    candidate.getMaterialCode(), candidate.getProductStatus());
            if (StringUtils.equals(targetSkuKey, candidateSkuKey)) {
                return;
            }
        }
        trialVirtualMachineCandidateList.add(sku);
    }

    /**
     * 登记班次9后置计划候选。
     * <p>按“物料编码+产品状态”去重，保证正规新增、续作转新增、日计划调整和试制/量试
     * 候选只保留一个业务身份；本方法不修改SKU数量、排序和原8班排产集合。</p>
     *
     * @param sku 班次9候选SKU
     * @return void
     */
    public void registerNextShiftNewPlanCandidate(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        String targetSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        for (SkuScheduleDTO candidate : nextShiftNewPlanCandidateList) {
            if (Objects.isNull(candidate)) {
                continue;
            }
            String candidateSkuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                    candidate.getMaterialCode(), candidate.getProductStatus());
            if (StringUtils.equals(targetSkuKey, candidateSkuKey)) {
                return;
            }
        }
        nextShiftNewPlanCandidateList.add(sku);
    }

    /**
     * 在现有候选池完成日期调整后登记最终归属；后续合法归池覆盖先前记录。
     *
     * @param sku 本轮候选对象，保留最新日计划等业务属性
     * @param poolDate 实际日期池 Map 的日期，不能使用调整前的原始日期
     */
    public void registerNextShiftNewPlanPool(SkuScheduleDTO sku, LocalDate poolDate) {
        if (isolatedNextShiftPlan || Objects.isNull(sku) || Objects.isNull(poolDate)
                || StringUtils.isEmpty(sku.getMaterialCode())) {
            return;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        this.registerNextShiftNewPlanCandidate(sku);
        // 只更新班次9信息，不改变原8班待排队列和候选运行态。
        nextShiftNewPlanCandidateList.replaceAll(candidate ->
                Objects.nonNull(candidate) && StringUtils.equals(skuKey,
                        MonthPlanDateResolver.buildMaterialStatusKey(
                                candidate.getMaterialCode(), candidate.getProductStatus())) ? sku : candidate);
        nextShiftNewPlanPoolDateMap.put(skuKey, poolDate);
    }

    /**
     * 获取 SKU 当前生效的提前生产运行视图。
     *
     * @param sku SKU
     * @return 当前运行视图；非提前生产阶段返回 null
     */
    public EarlyProductionRuntimePlan getEarlyProductionRuntimePlan(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(earlyProductionRuntimePlanMap)) {
            return null;
        }
        return earlyProductionRuntimePlanMap.get(sku);
    }

    /**
     * 判断 SKU 是否为当前月无总计划量、仅允许进入提前生产流程的候选。
     *
     * <p>该判断只负责路由隔离。候选态不代表已经通过结构、机台、模具或胎胚准入，
     * 正常新增阶段必须据此跳过，提前生产阶段再按业务日尝试激活。</p>
     *
     * @param sku SKU
     * @return true-仅允许走提前生产流程；false-沿用正常排产流程
     */
    public boolean isFutureOnlyEarlyProductionCandidate(SkuScheduleDTO sku) {
        EarlyProductionRuntimePlan runtimePlan = getEarlyProductionRuntimePlan(sku);
        return Objects.nonNull(runtimePlan) && runtimePlan.isFutureOnlyCandidate();
    }

    /**
     * 删除指定 SKU 的提前生产运行视图。
     *
     * <p>SKU 最终识别为续作、被减量规则移除或不再属于新增排产时调用，防止候选态
     * 残留后继续影响正常资源视图。</p>
     *
     * @param sku SKU
     */
    public void removeEarlyProductionRuntimePlan(SkuScheduleDTO sku) {
        if (Objects.nonNull(sku)) {
            earlyProductionRuntimePlanMap.remove(sku);
            newSpecEarlyProductionAllowedMap.remove(sku);
        }
    }

    /**
     * 解析当前排产调用应使用的日计划账本。
     *
     * <p>提前生产阶段返回临时前移账本，其他场景返回 SKU 原始运行态账本。该方法是选机、
     * 加机台、产能模拟和实际扣账的统一入口，调用方不得自行重新构造提前生产账本。</p>
     *
     * @param sku SKU
     * @return 当前生效的日计划账本
     */
    public Map<LocalDate, SkuDailyPlanQuotaDTO> resolveEffectiveDailyPlanQuotaMap(
            SkuScheduleDTO sku) {
        EarlyProductionRuntimePlan runtimePlan = getEarlyProductionRuntimePlan(sku);
        if (Objects.nonNull(runtimePlan)
                && runtimePlan.isActive()
                && !CollectionUtils.isEmpty(runtimePlan.getShiftedDailyPlanQuotaMap())) {
            return runtimePlan.getShiftedDailyPlanQuotaMap();
        }
        return Objects.isNull(sku) ? Collections.emptyMap() : sku.getDailyPlanQuotaMap();
    }

    /**
     * 清理当前提前生产阶段的全部临时运行视图。
     *
     * <p>只清理内存临时视图，不恢复或改写原始月计划和原始日计划账本。</p>
     */
    public void clearEarlyProductionRuntimePlans() {
        earlyProductionRuntimePlanMap.clear();
        newSpecEarlyProductionAllowedMap.clear();
    }

    /**
     * 获取指定业务日、指定结构的已排机台数。
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @return 已排结构物理机台数
     */
    public int getStructureScheduledMachineCount(LocalDate productionDate, String structureName) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(structureName)
                || CollectionUtils.isEmpty(structureScheduledMachineCodeMap)) {
            return 0;
        }
        Map<String, Set<String>> dateMachineMap =
                structureScheduledMachineCodeMap.get(productionDate);
        if (CollectionUtils.isEmpty(dateMachineMap)) {
            return 0;
        }
        Set<String> structureMachineCodeSet = dateMachineMap.get(structureName);
        if (CollectionUtils.isEmpty(structureMachineCodeSet)) {
            return 0;
        }
        return (int) structureMachineCodeSet.stream()
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .count();
    }

    /**
     * 判断指定物理机台是否已经计入当前业务日的结构机台集合。
     *
     * <p>该方法供提前生产候选机台级硬控使用：结构达到计划数后，已计入的物理机台仍可
     * 复用，新物理机台禁止加入。普通排产和真实历史欠产不调用该判断。</p>
     *
     * @param productionDate 业务日期
     * @param structureName  产品结构
     * @param machineCode    候选运行态机台编码
     * @return true-候选所属物理机台已计入该结构；false-尚未计入
     */
    public boolean hasStructureScheduledMachine(LocalDate productionDate,
                                                String structureName,
                                                String machineCode) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(structureName)
                || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(structureScheduledMachineCodeMap)) {
            return false;
        }
        Map<String, Set<String>> dateMachineMap =
                structureScheduledMachineCodeMap.get(productionDate);
        if (CollectionUtils.isEmpty(dateMachineMap)) {
            return false;
        }
        Set<String> structureMachineCodeSet = dateMachineMap.get(structureName);
        String physicalMachineCode =
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return StringUtils.isNotEmpty(physicalMachineCode)
                && !CollectionUtils.isEmpty(structureMachineCodeSet)
                && structureMachineCodeSet.stream()
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .anyMatch(physicalMachineCode::equals);
    }

    /**
     * 获取指定业务日、指定班次、指定结构的实时在机物理机台数。
     *
     * <p>该入口代理 S4.4换活字块前及S4.5新增排产前构建、并随结果提交实时更新的结构
     * 班次索引，供结构收尾对齐和全部提前生产入口共享；不会创建第二套统计账本。</p>
     *
     * @param productionDate 目标班次业务日期
     * @param shiftIndex     目标班次索引
     * @param structureName  产品结构
     * @return 当前班次已计入该结构的去重物理机台数
     */
    public int getStructureScheduledMachineCount(LocalDate productionDate,
                                                 int shiftIndex,
                                                 String structureName) {
        if (!this.isTargetShiftMatched(productionDate, shiftIndex)
                || StringUtils.isEmpty(structureName)
                || Objects.isNull(structureShiftInMachineIndex)) {
            return 0;
        }
        return structureShiftInMachineIndex.resolveInMachineCount(
                structureName, shiftIndex);
    }

    /**
     * 判断候选物理机台是否已经计入指定业务日、班次、结构。
     *
     * @param productionDate 目标班次业务日期
     * @param shiftIndex     目标班次索引
     * @param structureName  产品结构
     * @param machineCode    候选运行态机台编码
     * @return true-已计入，可在结构机台数达到上限时复用；false-会新增物理机台
     */
    public boolean hasStructureScheduledMachine(LocalDate productionDate,
                                                int shiftIndex,
                                                String structureName,
                                                String machineCode) {
        if (!this.isTargetShiftMatched(productionDate, shiftIndex)
                || StringUtils.isEmpty(structureName)
                || StringUtils.isEmpty(machineCode)
                || Objects.isNull(structureShiftInMachineIndex)) {
            return false;
        }
        return structureShiftInMachineIndex.containsPhysicalMachine(
                structureName, shiftIndex, machineCode);
    }

    /**
     * 校验班次索引与业务日期属于同一个排程窗口班次。
     *
     * @param productionDate 业务日期
     * @param shiftIndex     班次索引
     * @return true-班次存在且业务日期一致；false-不一致
     */
    private boolean isTargetShiftMatched(LocalDate productionDate, int shiftIndex) {
        if (Objects.isNull(productionDate)
                || shiftIndex < 1
                || CollectionUtils.isEmpty(scheduleWindowShifts)) {
            return false;
        }
        return scheduleWindowShifts.stream()
                .filter(Objects::nonNull)
                .filter(shift -> Objects.equals(shift.getShiftIndex(), shiftIndex))
                .filter(shift -> Objects.nonNull(shift.getWorkDate()))
                .map(shift -> shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate())
                .anyMatch(productionDate::equals);
    }

    /**
     * 获取指定业务日、指定 SKU 的已排机台数。
     *
     * @param productionDate 业务日期
     * @param materialCode   SKU物料编码
     * @param productStatus  产品状态
     * @return 已排物理机台数，同物料同状态的L/R双模只计一台
     */
    public int getSkuScheduledMachineCount(LocalDate productionDate,
                                           String materialCode,
                                           String productStatus) {
        return this.getSkuScheduledMachineCountExcluding(
                productionDate, materialCode, productStatus, Collections.<String>emptySet());
    }

    /**
     * 获取指定业务日、指定SKU排除部分物理机台后的已排机台数。
     *
     * @param productionDate 业务日期
     * @param materialCode SKU物料编码
     * @param productStatus 产品状态
     * @param excludedPhysicalMachineCodeSet 不再视为在机的物理机台编码
     * @return 排除后的已排物理机台数
     */
    public int getSkuScheduledMachineCountExcluding(
            LocalDate productionDate,
            String materialCode,
            String productStatus,
            Set<String> excludedPhysicalMachineCodeSet) {
        // 历史交替计划没有产品状态，项目统一口径要求空状态按正规S归一化。
        String normalizedProductStatus = StringUtils.isEmpty(productStatus)
                ? FORMAL_PRODUCT_STATUS : productStatus;
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                materialCode, normalizedProductStatus);
        if (Objects.isNull(productionDate)) {
            return 0;
        }
        Map<String, Set<String>> dayMachines = skuScheduledMachineCodeMap.get(productionDate);
        Set<String> machineCodes = CollectionUtils.isEmpty(dayMachines) ? null : dayMachines.get(skuKey);
        if (CollectionUtils.isEmpty(machineCodes)) {
            return 0;
        }
        // 目标Map使用物理机台份数，读取时统一去重，不能将同一整机的两侧当作两份需求。
        return (int) machineCodes.stream()
                .filter(StringUtils::isNotEmpty)
                .map(LhSingleControlMachineUtil::resolvePhysicalMachineCode)
                .filter(machineCode -> CollectionUtils.isEmpty(excludedPhysicalMachineCodeSet)
                        || !excludedPhysicalMachineCodeSet.contains(machineCode))
                .distinct()
                .count();
    }

    /**
     * 解析当前SKU因仅喷砂主动下机而释放的物理机台。
     *
     * @param sku 当前续作来源或同物料、同产品状态的补偿SKU
     * @return 已释放物理机台编码集合
     */
    public Set<String> resolveOnlySandBlastReleasedPhysicalMachineCodes(SkuScheduleDTO sku) {
        Set<String> physicalMachineCodeSet = new LinkedHashSet<String>(2);
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(continuousSkuList)
                || CollectionUtils.isEmpty(onlySandBlastContinuationReleaseWindowMap)) {
            return physicalMachineCodeSet;
        }
        for (SkuScheduleDTO continuousSku : continuousSkuList) {
            if (Objects.isNull(continuousSku)
                    || !StringUtils.equals(continuousSku.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(StringUtils.trimToEmpty(continuousSku.getProductStatus()),
                    StringUtils.trimToEmpty(sku.getProductStatus()))
                    || StringUtils.isEmpty(continuousSku.getContinuousMachineCode())
                    || !onlySandBlastContinuationReleaseWindowMap.containsKey(
                    continuousSku.getContinuousMachineCode())) {
                continue;
            }
            // 释放是同物料、同状态的机台事实，不依赖候选复制或回滚后的账本对象身份。
            physicalMachineCodeSet.add(LhSingleControlMachineUtil.resolvePhysicalMachineCode(
                    continuousSku.getContinuousMachineCode()));
        }
        return physicalMachineCodeSet;
    }

    /**
     * 解析续作因强制业务事件释放的物理机台。
     *
     * @param sku 当前续作来源或补偿SKU
     * @return 仅喷砂及两班临时故障已经释放的物理机台
     */
    public Set<String> resolveForcedReleasedPhysicalMachineCodes(SkuScheduleDTO sku) {
        Set<String> physicalMachineCodeSet = this.resolveOnlySandBlastReleasedPhysicalMachineCodes(sku);
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(continuationTemporaryFaultTransferEventMap)) {
            return physicalMachineCodeSet;
        }
        continuationTemporaryFaultTransferEventMap.values().stream()
                .filter(Objects::nonNull)
                .filter(event -> StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode()))
                .filter(event -> StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                        StringUtils.trimToEmpty(sku.getProductStatus())))
                .map(ContinuationTemporaryFaultTransferEvent::getOriginalPhysicalMachineCode)
                .filter(StringUtils::isNotEmpty)
                .forEach(physicalMachineCodeSet::add);
        return physicalMachineCodeSet;
    }

    /**
     * 判断候选机台是否为当前仅喷砂释放 SKU 已退出的原物理机台。
     *
     * @param sku 仅喷砂回流候选
     * @param machineCode 候选运行态机台
     * @return true-本次重新排产禁止选回的原喷砂物理机台
     */
    public boolean isOnlySandBlastOriginalMachine(SkuScheduleDTO sku, String machineCode) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(machineCode)) {
            return false;
        }
        return this.resolveOnlySandBlastReleasedPhysicalMachineCodes(sku).contains(
                LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode));
    }

    /**
     * 解析当前仅喷砂回流 SKU 必须携带的离机清洗事件。
     *
     * <p>无替换模具时，原 SKU 必须等待原模具清洗完成并携带该套模具转到其他机台。
     * 这里按清洗结束时间和来源机台稳定排序，供新增链路执行精确模具分配。</p>
     *
     * @param sku 仅喷砂回流候选
     * @return 同物料、同产品状态的离机喷砂事件
     */
    public List<MachineCleaningWindowDTO> resolveOnlySandBlastRequeueEvents(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)
                || CollectionUtils.isEmpty(onlySandBlastContinuationReleaseWindowMap)) {
            return Collections.emptyList();
        }
        return onlySandBlastContinuationReleaseWindowMap.values().stream()
                .filter(Objects::nonNull)
                .filter(MachineCleaningWindowDTO::isOffMachineCleaning)
                .filter(event -> StringUtils.isEmpty(event.getReplacementMouldCode()))
                .filter(event -> StringUtils.equals(
                        event.getContinuationMaterialCode(), sku.getMaterialCode()))
                .filter(event -> StringUtils.equals(
                        StringUtils.trimToEmpty(event.getContinuationProductStatus()),
                        StringUtils.trimToEmpty(sku.getProductStatus())))
                .sorted(Comparator
                        .comparing(MachineCleaningWindowDTO::getCleanEndTime,
                                Comparator.nullsLast(Date::compareTo))
                        .thenComparing(event -> StringUtils.defaultString(event.getLhCode())))
                .collect(Collectors.toList());
    }

    /**
     * 判断候选是否来源于达到两班阈值的临时故障续作迁移。
     *
     * @param sku 待排候选
     * @return true-故障释放SKU；false-普通候选
     */
    public boolean isTemporaryFaultTransferSku(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(continuationTemporaryFaultTransferEventMap)) {
            return false;
        }
        return continuationTemporaryFaultTransferEventMap.values().stream()
                .filter(Objects::nonNull)
                .anyMatch(event -> StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode())
                        && StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                        StringUtils.trimToEmpty(sku.getProductStatus())));
    }

    /**
     * 判断候选机台是否为当前故障SKU已经退出的原物理机台。
     *
     * @param sku 故障回流候选
     * @param machineCode 候选运行态机台
     * @return true-本次迁移禁止选回的原故障物理机台
     */
    public boolean isTemporaryFaultOriginalMachine(SkuScheduleDTO sku, String machineCode) {
        if (Objects.isNull(sku) || StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(continuationTemporaryFaultTransferEventMap)) {
            return false;
        }
        String physicalMachineCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        return continuationTemporaryFaultTransferEventMap.values().stream()
                .filter(Objects::nonNull)
                .anyMatch(event -> StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode())
                        && StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                        StringUtils.trimToEmpty(sku.getProductStatus()))
                        && StringUtils.equals(event.getOriginalPhysicalMachineCode(), physicalMachineCode));
    }

    /**
     * 解析仅喷砂剩余需求重新进入后续排产的最早时刻。
     *
     * @param sku 当前续作来源或共享同一日计划账本的补偿SKU
     * @return 所有关联释放机台中最晚的喷砂清洗结束时间；非本场景返回null
     */
    public Date resolveOnlySandBlastRequeueNotBeforeTime(SkuScheduleDTO sku) {
        Date requeueNotBeforeTime = null;
        for (MachineCleaningWindowDTO releaseWindow
                : this.resolveOnlySandBlastRequeueEvents(sku)) {
            if (Objects.isNull(releaseWindow.getCleanEndTime())) {
                continue;
            }
            if (Objects.isNull(requeueNotBeforeTime)
                    || releaseWindow.getCleanEndTime().after(requeueNotBeforeTime)) {
                // 原 SKU 携带离机喷砂模具重新排产，必须等待模具清洗完成；原机台仍从清洗开始时释放。
                requeueNotBeforeTime = releaseWindow.getCleanEndTime();
            }
        }
        return requeueNotBeforeTime;
    }

    /**
     * 解析续作被强制释放后重新参与资源竞争的最早时刻。
     *
     * @param sku 当前续作来源或补偿SKU
     * @return 仅喷砂清洗完成或临时故障释放时刻中的最晚值；非释放场景返回null
     */
    public Date resolveForcedRequeueNotBeforeTime(SkuScheduleDTO sku) {
        Date notBeforeTime = this.resolveOnlySandBlastRequeueNotBeforeTime(sku);
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(continuationTemporaryFaultTransferEventMap)) {
            return notBeforeTime;
        }
        for (ContinuationTemporaryFaultTransferEvent event
                : continuationTemporaryFaultTransferEventMap.values()) {
            if (Objects.isNull(event) || Objects.isNull(event.getFaultStartTime())
                    || !StringUtils.equals(event.getMaterialCode(), sku.getMaterialCode())
                    || !StringUtils.equals(StringUtils.trimToEmpty(event.getProductStatus()),
                    StringUtils.trimToEmpty(sku.getProductStatus()))) {
                continue;
            }
            if (Objects.isNull(notBeforeTime) || event.getFaultStartTime().after(notBeforeTime)) {
                notBeforeTime = event.getFaultStartTime();
            }
        }
        return notBeforeTime;
    }

    /**
     * 登记新增 SKU 在指定业务日真实命中的选机顺序。
     *
     * <p>只有排程结果正式提交后调用；同一 SKU 同一天因多机台拆量再次命中时保留首次顺序，
     * 防止候选机台重试或第二台机台重复增加当天 SKU 顺序。</p>
     *
     * @param sku            已正式命中的新增 SKU
     * @param dateOffset     当前业务日相对 T 日偏移
     * @param selectionOrder 当前业务日真实进入选机流程的顺序
     */
    public void recordNewSpecRealtimeSelectionOrder(
            SkuScheduleDTO sku,
            int dateOffset,
            int selectionOrder) {
        if (Objects.isNull(sku) || dateOffset < 0 || selectionOrder <= 0) {
            return;
        }
        Map<Integer, Integer> dailyOrderMap = newSpecRealtimeSelectionOrderMap.computeIfAbsent(
                sku, key -> new LinkedHashMap<Integer, Integer>(4));
        dailyOrderMap.putIfAbsent(dateOffset, selectionOrder);
    }

    /**
     * 构建新增 SKU 已真实命中的跨日选机顺序文本。
     *
     * @param sku 新增 SKU
     * @return {@code T=1,T+1=3}；尚未命中时返回空
     */
    public String buildNewSpecRealtimeSelectionOrderText(SkuScheduleDTO sku) {
        if (Objects.isNull(sku) || CollectionUtils.isEmpty(newSpecRealtimeSelectionOrderMap)) {
            return null;
        }
        Map<Integer, Integer> dailyOrderMap = newSpecRealtimeSelectionOrderMap.get(sku);
        if (CollectionUtils.isEmpty(dailyOrderMap)) {
            return null;
        }
        StringBuilder orderTextBuilder = new StringBuilder(24);
        dailyOrderMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (orderTextBuilder.length() > 0) {
                        orderTextBuilder.append(',');
                    }
                    orderTextBuilder.append(entry.getKey() == 0
                                    ? "T" : "T+" + entry.getKey())
                            .append('=').append(entry.getValue());
                });
        return orderTextBuilder.toString();
    }

    /**
     * 登记一条按天换活字块机台反选指令，并预留对应机台。
     *
     * @param directive 按天换活字块反选指令
     */
    public void registerDayTypeBlockReverseSelection(
            DayTypeBlockReverseSelectionDirective directive) {
        if (Objects.isNull(directive) || StringUtils.isEmpty(directive.getMachineCode())
                || StringUtils.isEmpty(directive.getMaterialCode())) {
            return;
        }
        dayTypeBlockReverseSelectionDirectiveList.add(directive);
        String normalizedProductStatus = StringUtils.isEmpty(directive.getProductStatus())
                ? FORMAL_PRODUCT_STATUS : directive.getProductStatus();
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                directive.getMaterialCode(), normalizedProductStatus);
        dayTypeBlockReverseSelectedSkuKeyMap.put(
                directive.getMachineCode(), skuKey);
    }

    /**
     * 释放指定机台的按天换活字块反选预留。
     *
     * @param machineCode 机台编码
     */
    public void releaseDayTypeBlockReverseSelectedMachine(String machineCode) {
        if (StringUtils.isEmpty(machineCode)) {
            return;
        }
        dayTypeBlockReverseSelectedSkuKeyMap.remove(machineCode);
    }

    /**
     * 判断机台是否已被按天换活字块反选预留。
     *
     * @param machineCode 机台编码
     * @return true-当天正常竞争阶段已被反选预留；false-未预留
     */
    public boolean isDayTypeBlockReverseSelectedMachine(String machineCode) {
        return StringUtils.isNotEmpty(machineCode)
                && dayTypeBlockReverseSelectedSkuKeyMap.containsKey(machineCode);
    }

    /**
     * 清空按天换活字块反选指令与机台预留。
     *
     * <p>必须在当天正常资源竞争阶段结束后调用，避免预留泄漏到提前生产阶段或下一业务日；
     * 下一业务日开始时按最新机台运行态重新检测并登记。</p>
     */
    public void clearDayTypeBlockReverseSelection() {
        dayTypeBlockReverseSelectionDirectiveList.clear();
        dayTypeBlockReverseSelectedSkuKeyMap.clear();
    }

    /**
     * 登记指定维度的机台编码。
     *
     * @param targetMap      目标统计Map
     * @param productionDate 业务日期
     * @param dimensionKey   结构或SKU编码
     * @param machineCode    机台编码
     */
    private void recordMachine(Map<LocalDate, Map<String, Set<String>>> targetMap,
                               LocalDate productionDate,
                               String dimensionKey,
                               String machineCode) {
        Map<String, Set<String>> dateMap = targetMap.computeIfAbsent(
                productionDate, key -> new LinkedHashMap<String, Set<String>>(8));
        Set<String> machineCodeSet = dateMap.computeIfAbsent(
                dimensionKey, key -> new LinkedHashSet<String>(4));
        machineCodeSet.add(machineCode);
    }

    /**
     * 从已排机台统计中移除指定维度的机台，并清理空集合。
     *
     * @param targetMap      结构或SKU已排机台统计Map
     * @param productionDate 业务日期
     * @param dimensionKey   结构或SKU编码
     * @param machineCode    机台编码
     */
    private void removeMachine(Map<LocalDate, Map<String, Set<String>>> targetMap,
                               LocalDate productionDate,
                               String dimensionKey,
                               String machineCode) {
        Map<String, Set<String>> dateMap = targetMap.get(productionDate);
        if (CollectionUtils.isEmpty(dateMap)) {
            return;
        }
        Set<String> machineCodeSet = dateMap.get(dimensionKey);
        if (CollectionUtils.isEmpty(machineCodeSet)) {
            return;
        }
        machineCodeSet.remove(machineCode);
        if (machineCodeSet.isEmpty()) {
            dateMap.remove(dimensionKey);
        }
        if (dateMap.isEmpty()) {
            targetMap.remove(productionDate);
        }
    }

    /**
     * 获取指定维度已排机台数。
     *
     * @param sourceMap      来源统计Map
     * @param productionDate 业务日期
     * @param dimensionKey   结构或SKU编码
     * @return 已排机台数
     */
    private int getScheduledMachineCount(Map<LocalDate, Map<String, Set<String>>> sourceMap,
                                         LocalDate productionDate,
                                         String dimensionKey) {
        if (Objects.isNull(productionDate) || StringUtils.isEmpty(dimensionKey)
                || CollectionUtils.isEmpty(sourceMap)) {
            return 0;
        }
        Map<String, Set<String>> dateMap = sourceMap.get(productionDate);
        if (CollectionUtils.isEmpty(dateMap)) {
            return 0;
        }
        Set<String> machineCodeSet = dateMap.get(dimensionKey);
        return CollectionUtils.isEmpty(machineCodeSet) ? 0 : machineCodeSet.size();
    }

    /**
     * 追加一条校验错误信息（空串或 null 将被忽略）
     *
     * @param message 错误描述
     */
    public void addValidationError(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        this.validationErrorList.add(message);
    }

    /**
     * 追加一条结构化校验错误明细
     *
     * @param detail 模具校验错误明细
     */
    public void addValidationErrorDetail(MouldValidationErrorDetail detail) {
        if (detail != null) {
            this.validationErrorDetailList.add(detail);
        }
    }

    /**
     * 批量追加结构化校验错误明细
     *
     * @param details 模具校验错误明细列表
     */
    public void addValidationErrorDetails(List<MouldValidationErrorDetail> details) {
        if (details != null) {
            this.validationErrorDetailList.addAll(details);
        }
    }

    /**
     * 追加一条非阻断提示信息（空串或 null 将被忽略，重复信息不重复添加）
     *
     * @param message 提示信息
     */
    public void addWarningMessage(String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (this.warningMessageList.contains(message)) {
            return;
        }
        this.warningMessageList.add(message);
    }

    /**
     * 登记生产日前跨日准备换模事件。
     *
     * @param machineCode          换模机台编码
     * @param mouldChangeStartTime 换模开始时间
     */
    public void registerCrossDayPreparationMouldChange(
            String machineCode,
            Date mouldChangeStartTime) {
        String eventKey = this.buildCrossDayPreparationMouldChangeEventKey(
                machineCode, mouldChangeStartTime);
        if (StringUtils.isNotEmpty(eventKey)) {
            this.crossDayPreparationMouldChangeEventKeySet.add(eventKey);
        }
    }

    /**
     * 判断模具交替计划是否属于已登记的生产日前跨日准备。
     *
     * @param machineCode          模具交替计划机台编码
     * @param mouldChangeStartTime 模具交替计划开始时间
     * @return true-跨日准备；false-普通换模或换活字块
     */
    public boolean isCrossDayPreparationMouldChange(
            String machineCode,
            Date mouldChangeStartTime) {
        String eventKey = this.buildCrossDayPreparationMouldChangeEventKey(
                machineCode, mouldChangeStartTime);
        return StringUtils.isNotEmpty(eventKey)
                && this.crossDayPreparationMouldChangeEventKeySet.contains(eventKey);
    }

    /**
     * 构建跨日准备物理换模事件键。
     *
     * @param machineCode          机台编码；单控L/R统一折算为物理整机
     * @param mouldChangeStartTime 换模开始时间
     * @return 事件键；参数不完整时返回空串
     */
    private String buildCrossDayPreparationMouldChangeEventKey(
            String machineCode,
            Date mouldChangeStartTime) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(mouldChangeStartTime)) {
            return StringUtils.EMPTY;
        }
        String physicalMachineCode = LhSingleControlMachineUtil
                .resolvePhysicalMachineCode(machineCode);
        return StringUtils.defaultString(physicalMachineCode)
                + "|" + mouldChangeStartTime.getTime();
    }

    /**
     * 获取硫化参数值
     *
     * @param paramCode    参数代码
     * @param defaultValue 默认值
     * @return 参数值
     */
    public String getParamValue(String paramCode, String defaultValue) {
        if (Objects.nonNull(scheduleConfig)) {
            return scheduleConfig.getParamValue(paramCode, defaultValue);
        }
        return lhParamsMap.getOrDefault(paramCode, defaultValue);
    }

    /**
     * 获取硫化参数值(整数)
     *
     * @param paramCode    参数代码
     * @param defaultValue 默认值
     * @return 参数值(整数)
     */
    public int getParamIntValue(String paramCode, int defaultValue) {
        if (Objects.nonNull(scheduleConfig)) {
            return scheduleConfig.getParamIntValue(paramCode, defaultValue);
        }
        String value = lhParamsMap.get(paramCode);
        if (StringUtils.isEmpty(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 登记续作停产保机业务日。
     *
     * @param machineCode    机台编码
     * @param productionDate 业务日期
     */
    public void registerContinuousStopHoldDate(String machineCode, LocalDate productionDate) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(productionDate)) {
            return;
        }
        continuousStopHoldDateMap
                .computeIfAbsent(machineCode, key -> new LinkedHashSet<LocalDate>(4))
                .add(productionDate);
        activeContinuousStopHoldMachineCodeSet.add(machineCode);
        releasedContinuousStopHoldMachineCodeSet.remove(machineCode);
    }

    /**
     * 登记续作停产保机实际班次。
     *
     * @param machineCode    机台编码
     * @param productionDate 业务日期
     * @param shiftIndexes   实际停产保机班次序号
     */
    public void registerContinuousStopHoldShifts(String machineCode,
                                                  LocalDate productionDate,
                                                  Collection<Integer> shiftIndexes) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(productionDate)
                || CollectionUtils.isEmpty(shiftIndexes)) {
            return;
        }
        this.registerContinuousStopHoldDate(machineCode, productionDate);
        continuousStopHoldShiftIndexMap
                .computeIfAbsent(machineCode, key -> new LinkedHashSet<Integer>(8))
                .addAll(shiftIndexes);
    }

    /**
     * 判断机台在指定业务日是否处于停产保机状态。
     *
     * @param machineCode    机台编码
     * @param productionDate 业务日期
     * @return true-该日停产保机；false-不是
     */
    public boolean isContinuousStopHoldDate(String machineCode, LocalDate productionDate) {
        if (StringUtils.isEmpty(machineCode) || Objects.isNull(productionDate)
                || CollectionUtils.isEmpty(continuousStopHoldDateMap)) {
            return false;
        }
        Set<LocalDate> holdDateSet = continuousStopHoldDateMap.get(machineCode);
        return !CollectionUtils.isEmpty(holdDateSet) && holdDateSet.contains(productionDate);
    }

    /**
     * 判断机台在指定班次是否处于停产保机状态。
     * <p>历史调用只登记业务日时继续按整日保机处理；新主链登记实际班次后严格按班次判断。</p>
     *
     * @param machineCode    机台编码
     * @param productionDate 业务日期
     * @param shiftIndex     班次序号
     * @return true-该班次停产保机；false-该班次允许按原规则生产
     */
    public boolean isContinuousStopHoldShift(String machineCode,
                                             LocalDate productionDate,
                                             Integer shiftIndex) {
        if (!this.isContinuousStopHoldDate(machineCode, productionDate) || Objects.isNull(shiftIndex)) {
            return false;
        }
        Set<Integer> holdShiftIndexSet = continuousStopHoldShiftIndexMap.get(machineCode);
        return CollectionUtils.isEmpty(holdShiftIndexSet) || holdShiftIndexSet.contains(shiftIndex);
    }

    /**
     * 判断机台在本次排程窗口内是否存在停产保机占用。
     *
     * @param machineCode 机台编码
     * @return true-存在停产保机日期；false-不存在
     */
    public boolean isContinuousStopHoldMachine(String machineCode) {
        if (StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(activeContinuousStopHoldMachineCodeSet)) {
            return false;
        }
        return activeContinuousStopHoldMachineCodeSet.contains(machineCode);
    }

    /**
     * 标记停产保机机台已按原续作SKU恢复生产。
     *
     * @param machineCode 机台编码
     */
    public void markContinuousStopHoldMachineProductionResumed(String machineCode) {
        if (StringUtils.isEmpty(machineCode)) {
            return;
        }
        activeContinuousStopHoldMachineCodeSet.remove(machineCode);
    }

    /**
     * 标记曾停产保机的机台已在后续业务日真正降模释放。
     *
     * @param machineCode 机台编码
     */
    public void markContinuousStopHoldMachineReleased(String machineCode) {
        if (StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(continuousStopHoldDateMap.get(machineCode))) {
            return;
        }
        activeContinuousStopHoldMachineCodeSet.remove(machineCode);
        releasedContinuousStopHoldMachineCodeSet.add(machineCode);
    }

    /**
     * 登记真正降模机台最后允许生产的班次序号。
     *
     * @param machineCode 机台编码
     * @param shiftIndex  最后允许生产班次序号；0表示本窗口全部班次均已释放
     */
    public void registerContinuousReducedMachineReleaseBoundary(String machineCode, int shiftIndex) {
        if (StringUtils.isEmpty(machineCode)) {
            return;
        }
        continuousReducedMachineReleaseBoundaryShiftIndexMap.put(machineCode, Math.max(0, shiftIndex));
    }

    /**
     * 获取真正降模机台最后允许生产的班次序号。
     *
     * @param machineCode 机台编码
     * @return 最后允许生产班次序号；未登记真正降模边界时返回null
     */
    public Integer getContinuousReducedMachineReleaseBoundaryShiftIndex(String machineCode) {
        if (StringUtils.isEmpty(machineCode)
                || CollectionUtils.isEmpty(continuousReducedMachineReleaseBoundaryShiftIndexMap)) {
            return null;
        }
        return continuousReducedMachineReleaseBoundaryShiftIndexMap.get(machineCode);
    }

    /**
     * 登记续作降模分组最后释放机台的业务日。
     * <p>同分组后续业务日再次降模时直接覆盖为更晚的业务日，保证取值始终是最后一次释放日。</p>
     *
     * @param groupKey       物料+产品状态复合键
     * @param productionDate 本次释放机台的业务日
     */
    public void registerReducedContinuationGroupLastReleaseDate(String groupKey, LocalDate productionDate) {
        if (StringUtils.isEmpty(groupKey) || Objects.isNull(productionDate)) {
            return;
        }
        reducedContinuationGroupLastReleaseDateMap.put(groupKey, productionDate);
    }

    /**
     * 获取续作降模分组最后释放机台的业务日。
     *
     * @param groupKey 物料+产品状态复合键
     * @return 最后一次真正下机的业务日；未发生逐日降模释放时返回null
     */
    public LocalDate getReducedContinuationGroupLastReleaseDate(String groupKey) {
        if (StringUtils.isEmpty(groupKey)
                || CollectionUtils.isEmpty(reducedContinuationGroupLastReleaseDateMap)) {
            return null;
        }
        return reducedContinuationGroupLastReleaseDateMap.get(groupKey);
    }

    /**
     * 中断排程流程
     *
     * @param reason 中断原因
     */
    public void interruptSchedule(String reason) {
        this.interrupted = true;
        this.interruptReason = reason;
    }

    /**
     * 进入优先级跟踪日志静默区间。
     * <p>用于局部搜索等模拟分支，避免输出非最终决策日志。</p>
     */
    public void enterPriorityTraceMuteScope() {
        priorityTraceMuteDepth++;
    }

    /**
     * 退出优先级跟踪日志静默区间。
     */
    public void exitPriorityTraceMuteScope() {
        if (priorityTraceMuteDepth > 0) {
            priorityTraceMuteDepth--;
        }
    }

    /**
     * 当前是否处于优先级跟踪日志静默区间。
     *
     * @return true-静默，false-正常输出
     */
    public boolean isPriorityTraceMuted() {
        return priorityTraceMuteDepth > 0;
    }

    /**
     * 进入新增排产机台驱动提案预演区间。
     */
    public void enterNewSpecProposalPreviewScope() {
        newSpecProposalPreviewDepth++;
    }

    /**
     * 退出新增排产机台驱动提案预演区间。
     */
    public void exitNewSpecProposalPreviewScope() {
        if (newSpecProposalPreviewDepth > 0) {
            newSpecProposalPreviewDepth--;
        }
    }

    /**
     * 判断当前是否处于新增排产机台驱动提案预演。
     *
     * @return true-预演批量试算；false-正式排产
     */
    public boolean isNewSpecProposalPreview() {
        return newSpecProposalPreviewDepth > 0;
    }

    /**
     * 累加并返回新增排产SKU本次选机日志序号。
     * <p>物料编码与产品状态共同构成计数维度，避免同物料不同产品状态共用序号。
     * 本方法只允许在真正写选机顺序日志前调用，局部搜索等静默分支不得调用。</p>
     *
     * @param sku 当前进入选机流程的SKU
     * @return 当前SKU本次选机序号；SKU为空时返回0
     */
    public int nextNewSpecMachineSelectionCount(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)) {
            return 0;
        }
        String skuKey = MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus());
        Integer currentCount = newSpecMachineSelectionCountMap.get(skuKey);
        int nextCount = Math.max(0, Objects.isNull(currentCount) ? 0 : currentCount) + 1;
        newSpecMachineSelectionCountMap.put(skuKey, nextCount);
        return nextCount;
    }

    /**
     * 将已移出待排队列的SKU同步从结构分组中剔除。
     * <p>structureSkuMap 在 S4.4 / S4.5 期间既用于顺序3结构收尾判断，也作为 SKU 兜底查询来源，
     * 因此需要与当前待排视图保持一致，避免已消费SKU继续影响后续排序与查询。</p>
     *
     * @param sku 已移出待排队列的SKU
     */
    public void removePendingSkuFromStructureMap(SkuScheduleDTO sku) {
        if (Objects.isNull(sku)
                || CollectionUtils.isEmpty(structureSkuMap)
                || StringUtils.isEmpty(sku.getStructureName())) {
            return;
        }
        List<SkuScheduleDTO> structureSkuList = structureSkuMap.get(sku.getStructureName());
        if (CollectionUtils.isEmpty(structureSkuList)) {
            structureSkuMap.remove(sku.getStructureName());
            return;
        }
        List<SkuScheduleDTO> mutableStructureSkuList = new ArrayList<>(structureSkuList);
        Iterator<SkuScheduleDTO> iterator = mutableStructureSkuList.iterator();
        while (iterator.hasNext()) {
            SkuScheduleDTO currentSku = iterator.next();
            if (isSameStructureSku(currentSku, sku)) {
                iterator.remove();
                break;
            }
        }
        if (CollectionUtils.isEmpty(mutableStructureSkuList)) {
            structureSkuMap.remove(sku.getStructureName());
            return;
        }
        structureSkuMap.put(sku.getStructureName(), mutableStructureSkuList);
    }

    /**
     * 判断结构分组中的SKU是否与目标SKU一致。
     * <p>同一物料可能同时存在正规、试制和量试月计划，非同一实例的降级匹配必须使用
     * “物料+产品状态”复合键，禁止移除其他产品状态的待排SKU。</p>
     *
     * @param currentSku 结构分组中的SKU
     * @param targetSku  目标SKU
     * @return true-同一SKU，false-不同SKU
     */
    private boolean isSameStructureSku(SkuScheduleDTO currentSku, SkuScheduleDTO targetSku) {
        if (currentSku == targetSku) {
            return true;
        }
        if (Objects.isNull(currentSku) || Objects.isNull(targetSku)) {
            return false;
        }
        return StringUtils.equals(currentSku.getMaterialCode(), targetSku.getMaterialCode())
                && StringUtils.equals(StringUtils.trimToEmpty(currentSku.getProductStatus()),
                StringUtils.trimToEmpty(targetSku.getProductStatus()));
    }

    /**
     * 基于当前待排SKU列表重建结构分组。
     * <p>用于阶段性收口结构视图，避免已消费SKU继续影响后续优先级判断。</p>
     *
     * @param pendingSkuList 当前待排SKU列表
     */
    public void rebuildStructureSkuMapFromPending(List<SkuScheduleDTO> pendingSkuList) {
        if (CollectionUtils.isEmpty(pendingSkuList)) {
            structureSkuMap = new LinkedHashMap<>();
            return;
        }
        Map<String, List<SkuScheduleDTO>> rebuiltStructureSkuMap = new LinkedHashMap<>(16);
        for (SkuScheduleDTO sku : pendingSkuList) {
            if (Objects.isNull(sku) || StringUtils.isEmpty(sku.getStructureName())) {
                continue;
            }
            rebuiltStructureSkuMap.computeIfAbsent(sku.getStructureName(), key -> new ArrayList<>()).add(sku);
        }
        structureSkuMap = rebuiltStructureSkuMap;
    }

    /**
     * 获取S4.4共用胎胚收尾均衡可调整物理机台快照。
     *
     * @return 可调整物理机台编码集合（单控整机已按物理机台去重）
     */
    public Set<String> getSharedEmbryoEndingBalanceEligibleMachineCodeSet() {
        return sharedEmbryoEndingBalanceEligibleMachineCodeSet;
    }

    /**
     * 获取工厂展示名称
     * <p>优先使用工厂名称，未设置时回退工厂编号。</p>
     *
     * @return 工厂展示名称
     */
    public String getFactoryDisplayName() {
        if (StringUtils.isNotEmpty(factoryName)) {
            return factoryName;
        }
        return factoryCode;
    }

    /**
     * 按物料编码 + 产品状态从SKU与示方书关系中查找（支持降级匹配）。
     * <p>降级规则：正规(S)→量试(T)→试制(X)；量试(T)→试制(X)；试制(X)不降级。</p>
     * <p>用于排产结果写入前回写文字/硫化/制造示方书号，未命中时返回 null，
     * 由调用方决定是否回退到其他来源或置空。</p>
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态（S-正规、T-量试、X-试制）
     * @return SKU与示方书关系，未命中返回 null
     */
    public MdmSkuConstructionRef findSkuConstructionRef(String materialCode, String productStatus) {
        return SkuConstructionRefResolverUtil.resolveCuringRecipeRef(
                materialCode, productStatus, skuConstructionRefCompositeKeyMap);
    }

}
