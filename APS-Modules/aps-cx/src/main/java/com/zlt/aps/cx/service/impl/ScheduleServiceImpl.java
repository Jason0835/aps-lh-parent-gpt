package com.zlt.aps.cx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.engine.utils.MonthPlanSurplusCalculator;
import com.zlt.aps.cx.component.ScheduleExecutionGuard;
import com.zlt.aps.cx.constant.ScheduleConstants;
import com.zlt.aps.cx.entity.CxMaterialEnding;
import com.zlt.aps.cx.api.domain.entity.CxStock;
import com.zlt.aps.cx.entity.config.CxKeyProduct;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.config.CxShiftConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleDetail;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.entity.schedule.CxShiftMachineLoad;
import com.zlt.aps.cx.entity.schedule.LhScheduleResult;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.enums.DayVulcanizationModeEnum;
import com.zlt.aps.cx.mapper.*;
import com.zlt.aps.lh.api.domain.entity.LhParams;
import com.zlt.aps.cx.mapper.LhParamsMapper;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.cx.service.ConstraintCheckService;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.ScheduleService;
import com.zlt.aps.cx.service.engine.CoreScheduleAlgorithmService;
import com.zlt.aps.cx.service.engine.ProductionCalculator;
import com.zlt.aps.cx.service.engine.ScheduleDayTypeHelper;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidationResult;
import com.zlt.aps.cx.service.impl.validation.ScheduleDataValidator;
import com.zlt.aps.cx.vo.MonthPlanProductLhCapacityVo;
import com.zlt.aps.cx.vo.ScheduleContextVo;
import com.zlt.aps.cx.vo.ScheduleRequestVo;
import com.zlt.aps.cx.vo.ScheduleResult;
import com.zlt.aps.cx.vo.ShiftPlanResult;
import com.zlt.aps.cx.vo.StockTaskAllocation;
import com.zlt.aps.cx.vo.TaskDemand;
import com.zlt.aps.cx.vo.ValidationDetail;
import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.api.domain.entity.CxStructureTreadConfig;
import com.zlt.aps.cx.mapper.CxStructureTreadConfigMapper;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 排程服务实现 — HTTP/API 层入口（S5.1），负责上下文构建、校验、持久化。
 *
 * <h3>端到端流水线</h3>
 * <pre>
 * executeSchedule / reSchedule（本类）
 *   ├─ S5.1.6 buildScheduleContext     加载主数据 → ScheduleContextVo（步骤 1.1～1.20）
 *   ├─ validateScheduleData            策略模式校验（ERROR 阻断）
 *   ├─ CoreScheduleAlgorithmServiceImpl.executeSchedule   S5.2～S5.5
 *   ├─ deleteExistingScheduleResults     按中间天删旧主表+子表
 *   └─ saveScheduleResults               写入 T_CX_SCHEDULE_RESULT / DETAIL
 * </pre>
 *
 * <h3>buildScheduleContext 三层加载</h3>
 * <table>
 *   <tr><th>层</th><th>步骤</th><th>产出</th></tr>
 *   <tr><td>基础</td><td>1.1～1.7</td><td>班次、机台、硫化任务、在机、物料、库存</td></tr>
 *   <tr><td>计算</td><td>1.8～1.13</td><td>参数、产能映射、硫化/成型余量、库存分配</td></tr>
 *   <tr><td>配置</td><td>1.14～1.20</td><td>主销、精度、收尾日、结构机台、单车容量</td></tr>
 * </table>
 *
 * <h3>ScheduleContextVo 运行时字段</h3>
 * <ul>
 *   <li><b>快照</b>：{@code initialMonthSurplusMap} / {@code initialFormingRemainderMap} /
 *       {@code initialMaterialStockMap}（排程开始前，不被班次滚动覆盖）</li>
 *   <li><b>滚动</b>：{@code monthSurplusMap}、{@code formingRemainderMap}、{@code materialStockMap}、
 *       {@code machineOnlineEmbryoMap}（每班后由 CoreScheduleAlgorithmServiceImpl 更新）</li>
 * </ul>
 *
 * <h3>日期约定</h3>
 * <p>请求 {@code scheduleDate} = 前端「中间天」；{@code scheduleStartDate = scheduleDate - 1} 为排产起始日，
 * 余量/库存/在机等多以此日为基准。
 *
 * <h3>参数治理</h3>
 * <p>{@link #loadParamConfigs} 遵循：硫化/工厂源头表 &gt; T_CX_PARAM_CONFIG &gt; 代码默认值（见 AGENTS.md SYS04 体系）。
 *
 * @author APS Team
 * @see CoreScheduleAlgorithmServiceImpl
 * @see ScheduleDataValidator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    // ==================== 常量定义 ====================

    /** 默认工厂编号 */
    private static final String DEFAULT_FACTORY_CODE = "116";

    /** 机台类型：成型 */
    private static final String MACHINE_TYPE_MOLDING = "成型";

    /** 参数编码：日硫化量计算模式 */
    private static final String PARAM_CODE_DAY_VULCANIZATION_MODE = "SYS04010001";

    /** 参数编码：损耗率 */
    private static final String PARAM_CODE_LOSS_RATE = "SYS04020001";

    /** 参数编码：机台种类上限 */
    private static final String PARAM_CODE_MAX_TYPES_PER_MACHINE = "SYS04020002";

    /** 参数编码：机台默认最大硫化机数 */
    private static final String PARAM_CODE_MAX_LH_MACHINE_QTY = "SYS04020003";

    /** 参数编码：硫化机停锅时间（停产日硫化停止时刻，HH:mm格式） */
    private static final String PARAM_CODE_VULCANIZING_STOP_TIME = "SYS04030001";

    /** 参数编码：硫化开模时间（开产日硫化开始时刻，HH:mm格式） */
    private static final String PARAM_CODE_VULCANIZING_OPEN_TIME = "SYS04030002";

    /** 硫化参数编码：停锅时间（T_LH_PARAMS） */
    private static final String LH_PARAM_CODE_STOP_TIME = "SYS0310007";

    /** 硫化参数编码：开模时间（T_LH_PARAMS） */
    private static final String LH_PARAM_CODE_OPEN_TIME = "SYS0310006";

    /** 参数编码：预留消化时间（小时，成型停机早于硫化停锅的时长） */
    private static final String PARAM_CODE_RESERVED_DIGEST_HOURS = "SYS04030003";

    /** 参数编码：机台最大胎胚种类数（格式: H15,3;H14,5，每段=前缀,数量） */
    private static final String PARAM_CODE_MACHINE_MAX_EMBRYO_TYPES = "SYS04040001";

    /** 默认机台最大胎胚种类数（H15前缀） */
    private static final int DEFAULT_MAX_EMBRYO_TYPES = 3;
    private static final String DEFAULT_MAX_EMBRYO_PREFIX = "H15";

    /** 默认损耗率 */
    private static final BigDecimal DEFAULT_LOSS_RATE = new BigDecimal("0.02");

    /** 主销产品类型编码 */
    private static final String MAIN_PRODUCT_SCHEDULE_TYPE = "01";

    /** 启用状态 */
    private static final Integer ACTIVE_STATUS = 1;

    // ==================== 依赖注入 ====================

    private final CoreScheduleAlgorithmService coreScheduleAlgorithmService;
    private final ProductionCalculator productionCalculator;
    private final ScheduleDayTypeHelper scheduleDayTypeHelper;
    private final ConstraintCheckService constraintCheckService;
    private final ScheduleDataValidator scheduleDataValidator;

    @Autowired
    private ScheduleExecutionGuard scheduleExecutionGuard;

    private final MdmMoldingMachineMapper moldingMachineMapper;
    private final MdmMaterialInfoMapper materialInfoMapper;
    private final MdmSkuScheduleCategoryMapper skuScheduleCategoryMapper;
    private final MdmStructureLhRatioMapper structureLhRatioMapper;
    private final MdmDevicePlanShutMapper devicePlanShutMapper;
    private final MdmMonthPlanProductLhCapacityMapper monthPlanProductLhCapacityMapper;

    private final CxStockMapper stockMapper;
    private final CxScheduleResultMapper scheduleResultMapper;
    private final CxScheduleDetailService scheduleDetailService;
    private final CxParamConfigMapper paramConfigMapper;
    private final LhParamsMapper lhParamsMapper;
    private final FactoryParamMapper factoryParamMapper;
    private final CxStructureTreadConfigMapper structureShiftCapacityMapper;
    private final CxKeyProductMapper keyProductMapper;
    private final LhScheduleResultMapper lhScheduleResultMapper;
    private final CxMachineOnlineInfoMapper onlineInfoMapper;
    private final CxShiftConfigMapper shiftConfigMapper;
    private final FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;
    private final CxMaterialEndingMapper materialEndingMapper;
    private final MpCxCapacityConfigurationMapper capacityConfigurationMapper;
    private final MdmWorkCalendarMapper workCalendarMapper;
    private final CxPrecisionPlanMapper precisionPlanMapper;
    private final LhFinishQtyMapper lhFinishQtyMapper;
    private final CxShiftMachineLoadMapper cxShiftMachineLoadMapper;

    // ==================== S5.1 对外接口 ====================

    /**
     * 执行排程（首次排产）。
     *
     * <p>流程：构建上下文 → 数据校验 → 核心算法 → 删旧结果 → 保存 → 后置约束检查。
     * 校验失败时返回 {@code success=false} 及 validationErrors，不执行算法。
     */
    @Override
    public ScheduleResult executeSchedule(ScheduleRequestVo request) {
        ScheduleResult result = new ScheduleResult();
        result.setSuccess(false);
        result.setScheduleDate(request.getScheduleDate());

        String factoryCode = request.getFactoryCode() != null ? request.getFactoryCode() : DEFAULT_FACTORY_CODE;
        LocalDate scheduleDate = request.getScheduleDate();

        // 获取排程执行锁（工厂+日期维度）
        String lockToken = scheduleExecutionGuard.acquire(factoryCode, scheduleDate);
        if (lockToken == null) {
            result.setSuccess(false);
            result.setErrorCode(result.ERROR_CODE_LOCK_CONFLICT);
            result.setMessage(I18nUtil.getMessage("ui.data.column.cxScheduleResult.scheduleRunning"));
            log.warn("排程锁已被占用，拒绝重复执行。工厂: {}, 日期: {}", factoryCode, scheduleDate);
            return result;
        }

        try {
            log.info("开始执行排程，日期：{}，排程模式：{}", request.getScheduleDate(), request.getScheduleMode());

            // 1. 构建排程上下文 S5.1.6（20步加载：班次→机台→硫化→库存→余量→结构配置等）
            ScheduleContextVo context = buildScheduleContext(request);
            if (context == null) {
                result.setMessage("构建排程上下文失败");
                return result;
            }

            // 2. 数据完整性校验（ERROR 阻断排程，WARN 仅告警；见 validation 包策略）
            ScheduleDataValidationResult validationResult = validateScheduleData(context, request.getScheduleDate(), request.getFactoryCode());

            if (!validationResult.isPassed()) {
                result.setMessage("数据完整性校验不通过，共 " + validationResult.getErrorCount() + " 项错误");
                result.setValidationErrors(convertValidationDetails(validationResult,
                        ScheduleDataValidationResult.ValidationLevel.ERROR));
                result.setValidationWarnings(convertValidationDetails(validationResult,
                        ScheduleDataValidationResult.ValidationLevel.WARN));
                return result;
            }

            // 3. 执行核心排程算法 S5.2~S5.5（委托 CoreScheduleAlgorithmServiceImpl，按班次循环）
            List<CxScheduleResult> scheduleResults = coreScheduleAlgorithmService.executeSchedule(context);

            // 3.1 删除该排程日期已有结果（主表+子表），避免重复
            deleteExistingScheduleResults(request.getScheduleDate());

            // 3.2 保存排程结果（主表 T_CX_SCHEDULE_RESULT + 子表 T_CX_SCHEDULE_DETAIL）
            saveScheduleResults(scheduleResults, request.getScheduleDate(), context);

            // 4. 后置约束验证（与校验层不同，检查排程产出是否满足业务约束）
            boolean validated = validateScheduleResults(scheduleResults);

            result.setSuccess(validated);
            result.setMessage(validated ? "排程成功" : "排程完成，但存在约束冲突");
            result.setResults(scheduleResults);

            log.info("排程执行完成，日期：{}，结果数量：{}", request.getScheduleDate(), scheduleResults.size());

        } catch (Exception e) {
            log.error("排程执行失败", e);
            result.setMessage("排程失败：" + e.getMessage());
        } finally {
            // 释放排程执行锁
            scheduleExecutionGuard.release(factoryCode, scheduleDate, lockToken);
        }

        return result;
    }

    /**
     * 加载精度计划
     *
     * <p>查询条件：planDate <= 排程起始日期 + 班次排程天数 + 提前天数（可配置），且 actualDate 为空（未从MES回调），
     * 且 scheduleDate 为空（未执行），且 isDelete=0
     * <p>注意：此处使用排程起始日期一次性加载最大范围的数据，实际每日选择时会根据当天日期动态计算截止。
     * 例如：传入排程日期=5月20日，提前天数=3，查询 planDate <= 5月23日 的所有未执行精度计划，
     * 后续排5月18日时只选 planDate<=5月21日的，排5月19日只选 planDate<=5月22日的。
     *
     * @param context      排程上下文
     * @param scheduleDate 排程起始日期
     */
    private void loadPrecisionPlans(ScheduleContextVo context, LocalDate scheduleDate) {
        int precisionAdvanceDays = 3;
        CxParamConfig advanceDaysConfig = context.getParamConfigMap() != null
                ? context.getParamConfigMap().get("SYS04030004") : null;
        if (advanceDaysConfig != null && advanceDaysConfig.getParamValue() != null) {
            try {
                precisionAdvanceDays = Integer.parseInt(advanceDaysConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析精度提前天数配置失败，使用默认3天");
            }
        }
        log.info("精度提前天数配置：{}天", precisionAdvanceDays);

        context.setPrecisionAdvanceDays(precisionAdvanceDays);

        // 查询1：actualDate为空 AND scheduleDate为空 — 从未安排过的精度计划
        // 限制 planDate <= 排程起始日期 + 班次排程天数 + 精度提前天数，避免全量加载
        int planDateRangeDays = context.getScheduleDays() + precisionAdvanceDays;
        java.sql.Timestamp planDateCutoff = java.sql.Timestamp.valueOf(
                scheduleDate.plusDays(planDateRangeDays).atTime(23, 59, 59));
        List<CxPrecisionPlan> precisionPlans = precisionPlanMapper.selectList(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .isNull(CxPrecisionPlan::getActualDate)
                        .isNull(CxPrecisionPlan::getScheduleDate)
                        .le(CxPrecisionPlan::getPlanDate, planDateCutoff)
                        .eq(CxPrecisionPlan::getIsDelete, "0"));

        // 查询2：actualDate为空 AND scheduleDate已回填 AND scheduleDate >= 排程起始日期 AND planDate <= 截止日期
        // 这种情况是：前一天排程时已经安排了精度计划并回填了scheduleDate，
        // 当期排程会重排昨天的部分日期这，今天还需要重新纳入做精度（防止精度被提前消耗后当天无法再做）
        List<CxPrecisionPlan> reapplyPlans = precisionPlanMapper.selectList(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .isNull(CxPrecisionPlan::getActualDate)
                        .isNotNull(CxPrecisionPlan::getScheduleDate)
                        .ge(CxPrecisionPlan::getScheduleDate, java.sql.Timestamp.valueOf(scheduleDate.atTime(0, 0, 0)))
                        .le(CxPrecisionPlan::getPlanDate, planDateCutoff)
                        .eq(CxPrecisionPlan::getIsDelete, "0"));

        if (reapplyPlans != null && !reapplyPlans.isEmpty()) {
            // 重置scheduleDate为null，让这些记录能被重新安排
            reapplyPlans.forEach(p -> p.setScheduleDate(null));
            precisionPlans.addAll(reapplyPlans);
            log.info("精度计划重新纳入: {} 条已回填但planDate > {} 的记录将被重新安排",
                    reapplyPlans.size(), scheduleDate);
        }

        context.setPrecisionPlans(precisionPlans);

        log.info("加载精度计划，排程日期={}，提前天数={}天，未执行精度计划 {} 条(含重新纳入 {} 条)",
                scheduleDate, precisionAdvanceDays,
                precisionPlans != null ? precisionPlans.size() : 0,
                reapplyPlans != null ? reapplyPlans.size() : 0);
    }

    /**
     * 删除指定日期的排程结果(主表+子表)
     */
    private void deleteExistingScheduleResults(LocalDate scheduleDate) {
        // 先查出该日期所有主表记录，获取ID用于删子表
        List<CxScheduleResult> existingResults = scheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
        );

        if (!existingResults.isEmpty()) {
            // 提取所有主表ID
            List<Long> mainIds = existingResults.stream()
                    .map(CxScheduleResult::getId)
                    .collect(Collectors.toList());

            // 批量删除子表：按主表ID列表一次性删除
            scheduleDetailService.deleteByMainIds(mainIds);
            log.info("批量删除日期 {} 的子表记录，共 {} 条主表关联", scheduleDate, mainIds.size());

            // 批量删除主表
            scheduleResultMapper.delete(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, scheduleDate)
            );
            log.info("批量删除日期 {} 的主表记录 {} 条", scheduleDate, existingResults.size());
        } else {
            log.info("日期 {} 无历史排程数据，跳过删除", scheduleDate);
        }
    }

    /**
     * S5.1.6 构建排程上下文 — 将 DB 主数据一次性装入 {@link ScheduleContextVo}。
     *
     * <p><b>步骤 1.1～1.20 有依赖顺序，不可调换</b>。例如：在机信息(1.5)先于物料(1.6)；
     * 参数(1.9)先于余量计算(1.13)；余量先于收尾日(1.16)与已收尾过滤(1.17)。
     *
     * <p>单步失败多数 catch 后 warn 并继续（非致命）；整体异常返回 null。
     *
     * <p>{@code scheduleStartDate = request.scheduleDate - 1}（中间天约定）。
     */
    private ScheduleContextVo buildScheduleContext(ScheduleRequestVo request) {
        try {
            ScheduleContextVo context = new ScheduleContextVo();
            LocalDate scheduleDate = request.getScheduleDate();
            // 前端传入的是中间天，排产起始日期需要往前推1天
            LocalDate scheduleStartDate = scheduleDate.minusDays(1);
            log.info("开始构建排程上下文，排产起始日期：{}，中间天：{}，工厂：{}",
                    scheduleStartDate, scheduleDate, request.getFactoryCode());

            // 1.1 加载班次配置（T_CX_SHIFT_CONFIG，计算 scheduleDays）
            String factoryCode = request.getFactoryCode() != null ? request.getFactoryCode() : DEFAULT_FACTORY_CODE;
            context.setFactoryCode(factoryCode);
            loadShiftConfigs(context, factoryCode);
            log.info("班次配置加载完成，班次数：{}", context.getShiftConfigList() != null ? context.getShiftConfigList().size() : 0);

            // 1.2 设备计划停机（T_MDM_DEVICE_PLAN_SHUT，覆盖排程区间）
            try {
                loadDevicePlanShuts(context, scheduleStartDate);
                log.info("设备计划停机信息加载完成");
            } catch (Exception e) {
                log.warn("加载设备计划停机信息失败，继续执行：{}", e.getMessage());
            }

            // 1.3 成型机台主数据（T_MDM_MOLDING_MACHINE）
            loadMoldingMachines(context);
            log.info("机台信息加载完成，机台数：{}", context.getAvailableMachines() != null ? context.getAvailableMachines().size() : 0);

            // 1.4 硫化排程结果（T_LH_SCHEDULE_RESULT）+ 提取 PRODUCTION_VERSION
            try {
                loadLhScheduleResults(context, scheduleDate);
                log.info("硫化排程结果加载完成");
                // 从硫化排程结果中提取排产版本
                extractProductionVersion(context);
            } catch (Exception e) {
                log.warn("加载硫化排程结果失败，继续执行：{}", e.getMessage());
            }

            // 1.5 成型在机信息（续作判定数据源，需在物料加载前）
            try {
                loadOnlineInfos(context, scheduleStartDate);
                log.info("成型在机信息加载完成");
            } catch (Exception e) {
                log.warn("加载成型在机信息失败，继续执行：{}", e.getMessage());
            }

            // 1.6 物料主数据（硫化任务+在机信息关联的 MdmMaterialInfo）
            try {
                loadMaterials(context);
                log.info("物料信息加载完成");
            } catch (Exception e) {
                log.warn("加载物料信息失败，继续执行：{}", e.getMessage());
            }

            // 1.7 胎胚库存（排产起始日早6点快照，T_CX_STOCK）
            try {
                loadStocks(context, scheduleStartDate);
                log.info("胎胚库存信息加载完成");
            } catch (Exception e) {
                log.warn("加载胎胚库存信息失败，继续执行：{}", e.getMessage());
            }

            // 1.8 机台在产胎胚映射（embryoCode→机台集合，供续作/均衡使用）
            try {
                buildMachineOnlineEmbryoMap(context);
            } catch (Exception e) {
                log.warn("构建机台在机胎胚映射失败，继续执行：{}", e.getMessage());
            }

            // 1.9 成型参数配置（SYS04 编码体系，含源头表优先级加载）
            try {
                loadParamConfigs(context);
            } catch (Exception e) {
                log.warn("加载参数配置失败，继续执行：{}", e.getMessage());
            }

            // 1.10 结构班次产能配置
            try {
                loadStructureShiftCapacities(context);
            } catch (Exception e) {
                log.warn("加载结构整车配置失败，继续执行：{}", e.getMessage());
            }

            // 1.11 关键产品配置（开产首班过滤用，T_CX_KEY_PRODUCT）
            try {
                loadKeyProducts(context);
            } catch (Exception e) {
                log.warn("加载关键产品配置失败，继续执行：{}", e.getMessage());
            }

            // 1.12 物料日硫化产能 + 结构硫化配比映射
            try {
                buildCapacityMaps(context);
            } catch (Exception e) {
                log.warn("构建产能映射失败，继续执行：{}", e.getMessage());
            }

            // 1.13 硫化余量 + 成型余量动态计算（月计划-完成量-库存分配，核心过滤依据）
            try {
                loadMonthSurplusAndCalculateFormingRemainder(context, scheduleStartDate);
            } catch (Exception e) {
                log.warn("加载月度计划余量失败，继续执行：{}", e.getMessage());
            }

            // 1.14 SKU 排产分类（主销/非主销判定）
            try {
                loadSkuCategories(context);
            } catch (Exception e) {
                log.warn("加载SKU排产分类失败，继续执行：{}", e.getMessage());
            }

            // 1.15 精度计划（planDate 筛选，供 CoreScheduleAlgorithmServiceImpl 扣量）
            try {
                loadPrecisionPlans(context, scheduleStartDate);
            } catch (Exception e) {
                log.warn("加载精度计划失败，继续执行：{}", e.getMessage());
            }

            // 1.16 物料收尾日计算（TaskGroupService.calculateEndingInfo 使用）
            try {
                loadMaterialEndings(context, scheduleStartDate);
            } catch (Exception e) {
                log.warn("加载物料收尾信息失败，继续执行：{}", e.getMessage());
            }

            // 1.17 过滤已收尾物料已移至校验之后执行（确保校验能看到全部含缺失字段的记录）

            // 1.18 结构排产配置（当日机台，NewTaskProcessor/BalancingService 候选机台来源）
            try {
                loadStructureAllocations(context, scheduleDate);
            } catch (Exception e) {
                log.warn("加载结构排产配置失败，继续执行：{}", e.getMessage());
            }

            // 1.20 写入排程日期与模式，上下文构建完成
            context.setScheduleDate(scheduleDate);
            context.setScheduleMode(request.getScheduleMode());

            // 1.21 加载前日最后班次的机台胎胚负荷映射（供动态保底预留）
            try {
                loadPreviousShiftMachineEmbryoLoadMap(context, scheduleDate);
            } catch (Exception e) {
                log.warn("加载前日班次负荷数据失败，继续执行：{}", e.getMessage());
            }

            log.info("排程上下文构建完成");
            return context;

        } catch (Exception e) {
            log.error("构建排程上下文失败", e);
            return null;
        }
    }

    /**
     * 加载前日最后班次的机台胎胚负荷映射。
     *
     * <p>从 T_CX_SHIFT_MACHINE_LOAD 查询排程日期前一天的最后一个班次记录，
     * 构建 previousShiftMachineEmbryoLoadMap 供 ContinueTaskProcessor/BalancingService 动态保底预留。
     *
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void loadPreviousShiftMachineEmbryoLoadMap(ScheduleContextVo context, LocalDate scheduleDate) {
        LocalDate previousDate = scheduleDate.minusDays(1);
        String factoryCode = context.getFactoryCode();
        List<CxShiftMachineLoad> loads = cxShiftMachineLoadMapper.selectLastShiftByDate(previousDate, factoryCode);
        if (loads == null || loads.isEmpty()) {
            log.info("无前日班次负荷数据(日期={})，动态保底预留将使用兜底值1", previousDate);
            context.setPreviousShiftMachineEmbryoLoadMap(new HashMap<>());
            return;
        }
        Map<String, Map<String, Integer>> loadMap = new HashMap<>();
        for (CxShiftMachineLoad load : loads) {
            loadMap.computeIfAbsent(load.getCxMachineCode(), k -> new HashMap<>())
                    .put(load.getEmbryoCode(), load.getLhMachineCount() != null ? load.getLhMachineCount() : 1);
        }
        context.setPreviousShiftMachineEmbryoLoadMap(loadMap);
        log.info("加载前日班次负荷数据: 日期={}, 班次={}, {}台机台, {}条记录",
                previousDate, loads.get(0).getShiftCode(), loadMap.size(),
                loadMap.values().stream().mapToInt(Map::size).sum());
    }

    /**
     * 1.1 加载班次配置（T_CX_SHIFT_CONFIG，并推算 scheduleDays）
     */
    private void loadShiftConfigs(ScheduleContextVo context, String factoryCode) {
        List<CxShiftConfig> allShiftConfigs = shiftConfigMapper.selectList(
                new LambdaQueryWrapper<CxShiftConfig>()
                        .eq(CxShiftConfig::getFactoryCode, factoryCode)
                        .eq(CxShiftConfig::getIsActive, ACTIVE_STATUS)
                        .orderByAsc(CxShiftConfig::getScheduleDay)
                        .orderByAsc(CxShiftConfig::getDayShiftOrder)
        );
        context.setShiftConfigList(allShiftConfigs);
        log.info("班次配置加载完成，班次数：{}，示例：{}",
                allShiftConfigs != null ? allShiftConfigs.size() : 0,
                allShiftConfigs != null && !allShiftConfigs.isEmpty()
                        ? allShiftConfigs.get(0).getShiftCode() : "无");

        // 按排程天数分组
        Map<Integer, List<CxShiftConfig>> dayShiftMap = allShiftConfigs.stream()
                .filter(c -> c.getScheduleDay() != null)
                .collect(Collectors.groupingBy(CxShiftConfig::getScheduleDay));

        int scheduleDays = dayShiftMap.isEmpty() ? ScheduleConstants.DEFAULT_SCHEDULE_DAYS
                : dayShiftMap.keySet().stream().max(Integer::compareTo).orElse(ScheduleConstants.DEFAULT_SCHEDULE_DAYS);
        context.setScheduleDays(scheduleDays);
        log.info("根据班次配置计算排程天数: {}, 班次分布: {}", scheduleDays,
                dayShiftMap.entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue().size() + "个")
                        .collect(Collectors.joining(", ")));
    }

    /**
     * 加载设备计划停机信息
     */
    private void loadDevicePlanShuts(ScheduleContextVo context, LocalDate scheduleDate) {
        int scheduleDays = context.getScheduleDays();
        LocalDate endDate = scheduleDate.plusDays(scheduleDays - 1);

        List<MdmDevicePlanShut> devicePlanShuts = devicePlanShutMapper.selectByMachineTypeAndDateRange(
                MACHINE_TYPE_MOLDING, scheduleDate, endDate);
        context.setDevicePlanShuts(devicePlanShuts);
        log.info("加载成型机台停机计划 {} 条", devicePlanShuts.size());
    }

    /**
     * 加载成型机台（只加载启用且未删除的机台）
     */
    private void loadMoldingMachines(ScheduleContextVo context) {
        List<MdmMoldingMachine> machines = moldingMachineMapper.selectList(
                new LambdaQueryWrapper<MdmMoldingMachine>()
                        .eq(MdmMoldingMachine::getIsActive, "1")
                        .eq(MdmMoldingMachine::getIsDelete, "0"));
        context.setAvailableMachines(machines);
        log.info("加载成型机台 {} 台（已过滤禁用和已删除）", machines.size());

        // 构建机台机型映射
        Map<String, String> machineTypeCodeMap = new HashMap<>();
        for (MdmMoldingMachine machine : machines) {
            if (machine.getCxMachineCode() != null && machine.getCxMachineTypeCode() != null) {
                machineTypeCodeMap.put(machine.getCxMachineCode(), machine.getCxMachineTypeCode());
            }
        }
        context.setMachineTypeCodeMap(machineTypeCodeMap);
        log.info("构建机台机型映射，共 {} 条", machineTypeCodeMap.size());
    }

    /**
     * 加载硫化排程结果
     */
    private void loadLhScheduleResults(ScheduleContextVo context, LocalDate scheduleDate) {
        log.info("查询硫化排程结果，日期: {}", scheduleDate);
        List<LhScheduleResult> lhScheduleResults = lhScheduleResultMapper.selectByDate(scheduleDate);
        log.info("硫化排程查询结果: {} 条", lhScheduleResults != null ? lhScheduleResults.size() : 0);
        context.setLhScheduleResults(lhScheduleResults);
    }

    /**
     * 从硫化排程结果中提取排产版本
     * <p>取所有硫化排程结果中不重复的 PRODUCTION_VERSION，通常只有一个版本
     * <p>用于后续过滤结构排产配置（T_MP_STRUCTURE_ALLOCATION）
     */
    private void extractProductionVersion(ScheduleContextVo context) {
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();
        if (lhScheduleResults == null || lhScheduleResults.isEmpty()) {
            log.info("硫化排程结果为空，无法提取排产版本");
            return;
        }

        Set<String> versions = lhScheduleResults.stream()
                .map(LhScheduleResult::getProductionVersion)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (versions.size() == 1) {
            String version = versions.iterator().next();
            context.setProductionVersion(version);
            log.info("从硫化排程结果提取排产版本: {}", version);
        } else if (versions.size() > 1) {
            String version = versions.iterator().next();
            context.setProductionVersion(version);
            log.warn("硫化排程结果包含多个排产版本: {}，使用第一个: {}", versions, version);
        } else {
            log.warn("硫化排程结果中排产版本均为空");
        }
    }

    /**
     * 加载物料信息
     *
     * <p>物料来源包括两部分：
     * 1. 硫化排程结果的物料
     * 2. 成型在机信息的物料（可能存在没有硫化任务但存在在机信息的物料）
     */
    private void loadMaterials(ScheduleContextVo context) {
        List<LhScheduleResult> lhScheduleResults = context.getLhScheduleResults();

        // 合并硫化任务物料和成型在机物料（同时收集 MATERIAL_CODE 和 EMBRYO_CODE）
        Set<String> materialCodes = new HashSet<>();
        Set<String> embryoCodes = new HashSet<>();

        // 1. 从硫化排程结果提取物料编码和胎胚编码
        if (lhScheduleResults != null && !lhScheduleResults.isEmpty()) {
            Set<String> lhMaterialCodes = lhScheduleResults.stream()
                    .map(LhScheduleResult::getMaterialCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<String> lhEmbryoCodes = lhScheduleResults.stream()
                    .map(LhScheduleResult::getEmbryoCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            materialCodes.addAll(lhMaterialCodes);
            embryoCodes.addAll(lhEmbryoCodes);
            log.debug("从硫化排程结果提取到 {} 个 MATERIAL_CODE、{} 个 EMBRYO_CODE",
                    lhMaterialCodes.size(), lhEmbryoCodes.size());
        }

        if (materialCodes.isEmpty() && embryoCodes.isEmpty()) {
            log.info("硫化排程结果和成型在机信息均为空，加载物料信息 0 条");
            context.setMaterials(new ArrayList<>());
            return;
        }

        log.info("合并后共有 {} 个 MATERIAL_CODE、{} 个 EMBRYO_CODE", materialCodes.size(), embryoCodes.size());

        // 查询物料详情（同时按 MATERIAL_CODE 和 EMBRYO_CODE 匹配，避免编码交叉遗漏）
        List<MdmMaterialInfo> materials = materialInfoMapper.selectList(
                new LambdaQueryWrapper<MdmMaterialInfo>()
                        .and(wrapper -> wrapper
                                .in(MdmMaterialInfo::getMaterialCode, materialCodes)
                                .or()
                                .in(MdmMaterialInfo::getEmbryoCode, embryoCodes))
                        .eq(MdmMaterialInfo::getIsDelete, "0"));
        log.info("加载物料信息 {} 条", materials.size());

        context.setMaterials(materials);
    }

    /**
     * 加载胎胚库存
     *
     * <p>根据排程日期获取早上6点那一刻的库存
     *
     * @param context       排程上下文
     * @param scheduleDate  排程日期
     */
    private void loadStocks(ScheduleContextVo context, LocalDate scheduleDate) {
        // 将 LocalDate 转换为 java.sql.Date 用于数据库查询
        Date stockDate = Date.from(scheduleDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        List<CxStock> stocks = stockMapper.selectList(
                new LambdaQueryWrapper<CxStock>()
                        .eq(CxStock::getStockDate, stockDate)
                        .gt(CxStock::getStockNum, 0)
                        .eq(CxStock::getIsDelete, "0"));
        context.setStocks(stocks);
        log.info("加载胎胚库存 {} 条 (库存日期: {})", stocks.size(), scheduleDate);
    }

    /**
     * 加载成型在机信息
     */
    private void loadOnlineInfos(ScheduleContextVo context, LocalDate scheduleDate) {
        List<CxMachineOnlineInfo> onlineInfos = onlineInfoMapper.selectByDateRange(
                scheduleDate, scheduleDate.minusDays(1));
        context.setOnlineInfos(onlineInfos);
        log.info("加载成型在机信息 {} 条", onlineInfos.size());
    }

    /**
     * 构建机台在机胎胚映射
     *
     * <p>使用胎胚编码作为 Key，便于快速查找续作机台：
     * <pre>
     * embryoCode → Set&lt;cxCode&gt;
     * </pre>
     *
     * <p>说明：由于MES数据时，materialCode字段被错误地填入了胎胚编号，
     * 因此只使用胎胚编码作为匹配键，避免组合键匹配失败。
     */
    private void buildMachineOnlineEmbryoMap(ScheduleContextVo context) {
        Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
        for (CxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
            String cxCode = onlineInfo.getCxCode();
            // KF_NICK 这里的 onlineInfo.getMaterialCode() 实际存的是胎胚号
            String embryoCode = onlineInfo.getMaterialCode();
            if (cxCode != null && embryoCode != null && !embryoCode.isEmpty()) {
                machineOnlineEmbryoMap.computeIfAbsent(embryoCode, k -> new HashSet<>()).add(cxCode);
            }
        }
        context.setMachineOnlineEmbryoMap(machineOnlineEmbryoMap);
        log.info("构建机台在机胎胚映射，共 {} 个胎胚有在机任务", machineOnlineEmbryoMap.size());
    }

    /**
     * 1.9 加载成型参数（T_CX_PARAM_CONFIG + 源头表覆盖）。
     *
     * <p>优先级：T_LH_PARAMS / T_MP_FACTORY_PARAM 覆盖同编码 SYS04 参数；
     * 并解析机台前缀最大胎胚种类数、试制 SKU 上限、日硫化模式等到 context 字段。
     */
    private void loadParamConfigs(ScheduleContextVo context) {
        List<CxParamConfig> paramConfigs = paramConfigMapper.selectList(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getIsDelete, "0"));
        log.info("加载参数配置，共 {} 条记录", paramConfigs != null ? paramConfigs.size() : 0);
        if (paramConfigs != null && !paramConfigs.isEmpty()) {
            for (CxParamConfig config : paramConfigs) {
                log.debug("参数配置：{} = {}", config.getParamCode(), config.getParamValue());
            }
        }
        Map<String, CxParamConfig> paramConfigMap = paramConfigs.stream()
                .collect(Collectors.toMap(CxParamConfig::getParamCode, p -> p, (a, b) -> a));
        context.setParamConfigMap(paramConfigMap);

        // 加载损耗率
        CxParamConfig lossRateConfig = paramConfigMap.get(PARAM_CODE_LOSS_RATE);
        BigDecimal lossRate = lossRateConfig != null
                ? new BigDecimal(lossRateConfig.getParamValue())
                : DEFAULT_LOSS_RATE;
        context.setLossRate(lossRate);

        // 加载机台种类上限
        CxParamConfig maxTypesConfig = paramConfigMap.get(PARAM_CODE_MAX_TYPES_PER_MACHINE);
        if (maxTypesConfig != null && maxTypesConfig.getParamValue() != null) {
            try {
                context.setMaxTypesPerMachine(Integer.parseInt(maxTypesConfig.getParamValue()));
                log.info("机台种类上限配置：{}", maxTypesConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析机台种类上限配置失败: {}", maxTypesConfig.getParamValue());
            }
        }

        // 加载机台默认最大硫化机数
        CxParamConfig maxLhConfig = paramConfigMap.get(PARAM_CODE_MAX_LH_MACHINE_QTY);
        if (maxLhConfig != null && maxLhConfig.getParamValue() != null) {
            try {
                context.setMaxLhMachineQty(Integer.parseInt(maxLhConfig.getParamValue()));
                log.info("机台默认最大硫化机数配置：{}", maxLhConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析机台默认最大硫化机数配置失败: {}", maxLhConfig.getParamValue());
            }
        }

        // 加载硫化机停锅时间（优先从 T_LH_PARAMS 取，兜底从 T_CX_PARAM_CONFIG 取）
        String stopTimeValue = loadLhParamValue(lhParamsMapper, context.getFactoryCode(), LH_PARAM_CODE_STOP_TIME);
        if (stopTimeValue == null) {
            CxParamConfig vulcanizingStopTimeConfig = paramConfigMap.get(PARAM_CODE_VULCANIZING_STOP_TIME);
            if (vulcanizingStopTimeConfig != null && vulcanizingStopTimeConfig.getParamValue() != null) {
                stopTimeValue = vulcanizingStopTimeConfig.getParamValue().trim();
                log.info("硫化停锅时间（来自成型参数配置）: {}", stopTimeValue);
            }
        } else {
            log.info("硫化停锅时间（来自硫化参数配置 SYS0310007）: {}", stopTimeValue);
        }
        if (stopTimeValue != null) {
            context.setVulcanizingStopTimeStr(stopTimeValue);
            try {
                if (stopTimeValue.contains("-") && stopTimeValue.contains(":")) {
                    LocalDateTime stopDateTime = productionCalculator.parseFlexibleDateTime(stopTimeValue);
                    if (stopDateTime != null) {
                        context.setVulcanizingStopDateTime(stopDateTime);
                    }
                }
            } catch (Exception e) {
                log.warn("解析硫化机停锅时间失败（非日期时间格式），使用原始值：{}", stopTimeValue);
            }
        }

        // 加载硫化开模时间（优先从 T_LH_PARAMS 取，兜底从 T_CX_PARAM_CONFIG 取）
        String openTimeValue = loadLhParamValue(lhParamsMapper, context.getFactoryCode(), LH_PARAM_CODE_OPEN_TIME);
        if (openTimeValue == null) {
            CxParamConfig vulcanizingOpenTimeConfig = paramConfigMap.get(PARAM_CODE_VULCANIZING_OPEN_TIME);
            if (vulcanizingOpenTimeConfig != null && vulcanizingOpenTimeConfig.getParamValue() != null) {
                openTimeValue = vulcanizingOpenTimeConfig.getParamValue().trim();
                log.info("硫化开模时间（来自成型参数配置）: {}", openTimeValue);
            }
        } else {
            log.info("硫化开模时间（来自硫化参数配置 SYS0310006）: {}", openTimeValue);
        }
        if (openTimeValue != null) {
            context.setVulcanizingOpenTimeStr(openTimeValue);
            try {
                if (openTimeValue.contains("-") && openTimeValue.contains(":")) {
                    LocalDateTime openDateTime = productionCalculator.parseFlexibleDateTime(openTimeValue);
                    if (openDateTime != null) {
                        context.setVulcanizingOpenDateTime(openDateTime);
                    }
                }
            } catch (Exception e) {
                log.warn("解析硫化开模时间失败（非日期时间格式），使用原始值：{}", openTimeValue);
            }
        }

        // 加载预留消化时间（小时）
        CxParamConfig reservedDigestConfig = paramConfigMap.get(PARAM_CODE_RESERVED_DIGEST_HOURS);
        if (reservedDigestConfig != null && reservedDigestConfig.getParamValue() != null) {
            try {
                context.setReservedDigestHours(Integer.parseInt(reservedDigestConfig.getParamValue()));
                log.info("预留消化时间配置：{}小时", reservedDigestConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析预留消化时间配置失败: {}", reservedDigestConfig.getParamValue());
            }
        }

        // 加载机台最大胎胚种类数（格式: H15,3;H14,5）
        Map<String, Integer> machineMaxTypesMap = loadMachineMaxEmbryoTypes(context, paramConfigMap);
        context.setMachineMaxEmbryoTypes(machineMaxTypesMap);
        log.info("机台最大胎胚种类数: {}", machineMaxTypesMap);

        // 加载库存可供硫化时长预警阈值（默认18小时）
        CxParamConfig stockHoursWarningConfig = paramConfigMap.get("SYS04070001");
        if (stockHoursWarningConfig != null && stockHoursWarningConfig.getParamValue() != null) {
            try {
                context.setStockHoursWarningThreshold(Integer.parseInt(stockHoursWarningConfig.getParamValue()));
                log.info("库存可供硫化时长预警阈值：{}h", stockHoursWarningConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析库存可供硫化时长预警阈值配置失败: {}", stockHoursWarningConfig.getParamValue());
            }
        }

        // 加载子表车次合并数（默认1车一条）
        CxParamConfig tripGroupSizeConfig = paramConfigMap.get("SYS04080006");
        if (tripGroupSizeConfig != null && tripGroupSizeConfig.getParamValue() != null) {
            try {
                context.setDetailTripGroupSize(Integer.parseInt(tripGroupSizeConfig.getParamValue()));
                log.info("子表车次合并数：{}车/条", tripGroupSizeConfig.getParamValue());
            } catch (NumberFormatException e) {
                log.warn("解析子表车次合并数配置失败: {}", tripGroupSizeConfig.getParamValue());
            }
        }

        // 加载单日试制/量试SKU上限（优先从T_MP_FACTORY_PARAM取）
        String trialSkuLimit = loadFactoryParamValue(factoryParamMapper, context.getFactoryCode(), "SYS0206003");
        if (trialSkuLimit != null) {
            try {
                context.setMaxTrialSkuPerDay(Integer.parseInt(trialSkuLimit));
                log.info("单日试制/量试SKU上限（来自T_MP_FACTORY_PARAM SYS0206003）: {}", trialSkuLimit);
            } catch (NumberFormatException e) {
                log.warn("解析试制SKU上限失败: {}", trialSkuLimit);
            }
        }
        if (context.getMaxTrialSkuPerDay() == null) {
            context.setMaxTrialSkuPerDay(2);
            log.info("单日试制/量试SKU上限（使用默认值）: 2");
        }

        // 加载试制/量试周日是否允许排产（优先从T_MP_FACTORY_PARAM取）
        String trialSundayAllowed = loadFactoryParamValue(factoryParamMapper, context.getFactoryCode(), "SYS0206005");
        if (trialSundayAllowed != null) {
            context.setTrialAllowedOnSunday("Y".equalsIgnoreCase(trialSundayAllowed));
            log.info("试制/量试周日是否允许（来自T_MP_FACTORY_PARAM SYS0206005）: {}", trialSundayAllowed);
        }
        if (context.getTrialAllowedOnSunday() == null) {
            context.setTrialAllowedOnSunday(false);
            log.info("试制/量试周日是否允许（使用默认值）: N");
        }
    }

    /**
     * 加载结构整车配置（T_CX_STRUCTURE_TREAD_CONFIG）
     *
     * <p>一次查询同时设置 structureShiftCapacities（供 ProductionCalculator/ShiftScheduleService 使用）
     * 和 structureTreadConfigs（供 LhScheduleResultValidationStrategy 使用），避免重复查表。
     */
    private void loadStructureShiftCapacities(ScheduleContextVo context) {
        List<CxStructureTreadConfig> treadConfigs = structureShiftCapacityMapper.selectList(
                new LambdaQueryWrapper<CxStructureTreadConfig>()
                        .eq(CxStructureTreadConfig::getIsDelete, "0"));
        context.setStructureShiftCapacities(treadConfigs);
        context.setStructureTreadConfigs(treadConfigs);
        log.info("加载结构整车配置 {} 条", treadConfigs.size());
    }



    /**
     * 加载关键产品配置
     */
    private void loadKeyProducts(ScheduleContextVo context) {
        List<CxKeyProduct> keyProducts = keyProductMapper.selectList(
                new LambdaQueryWrapper<CxKeyProduct>()
                        .eq(CxKeyProduct::getIsActive, ACTIVE_STATUS)
                        .eq(CxKeyProduct::getIsDelete, "0"));
        context.setKeyProducts(keyProducts);

        Set<String> keyProductCodes = new HashSet<>();
        for (CxKeyProduct product : keyProducts) {
            keyProductCodes.add(product.getEmbryoCode());
        }
        context.setKeyProductCodes(keyProductCodes);
    }

    /**
     * 加载结构排产配置
     *
     * <p>从 T_MP_STRUCTURE_ALLOCATION 表获取每个结构可分配的机台列表
     * <p>用于续作任务的均衡分配
     */
    private void loadStructureAllocations(ScheduleContextVo context, LocalDate scheduleDate) {
        // 以 T日（中间天-1）所在月份为当月
        LocalDate tDay = scheduleDate.minusDays(1);
        int year = tDay.getYear();
        int month = tDay.getMonthValue();
        String factoryCode = context.getFactoryCode() != null ? context.getFactoryCode() : DEFAULT_FACTORY_CODE;

        // 判断排程是否跨月
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : ScheduleConstants.DEFAULT_SCHEDULE_DAYS;
        LocalDate scheduleEndDate = tDay.plusDays(scheduleDays - 1);
        boolean crossMonth = scheduleEndDate.getMonth() != tDay.getMonth()
                || scheduleEndDate.getYear() != tDay.getYear();

        // 1. 加载当月结构排产配置
        List<MpCxCapacityConfiguration> allocations = new ArrayList<>(
                loadMonthAllocations(factoryCode, year, month));

        // 2. 跨月时加载次月结构排产配置
        if (crossMonth) {
            LocalDate nextMonthDate = scheduleEndDate.withDayOfMonth(1);
            int nextYear = nextMonthDate.getYear();
            int nextMonth = nextMonthDate.getMonthValue();
            List<MpCxCapacityConfiguration> nextAllocations =
                    loadMonthAllocations(factoryCode, nextYear, nextMonth);
            allocations.addAll(nextAllocations);
            log.info("跨月场景：合并当月({}-{})与次月({}-{})结构排产配置，共 {} 条",
                    year, month, nextYear, nextMonth, allocations.size());
        }

        context.setStructureAllocations(allocations);
        // 记录当月配置所属年月（year*100+month），供日志参考
        context.setStructureAllocationYearMonth(year * 100 + month);

        // 按结构分组（两个月配置合并，靠 year/month 字段区分）
        Map<String, List<MpCxCapacityConfiguration>> structureAllocationMap = allocations.stream()
                .filter(a -> a.getStructureName() != null)
                .collect(Collectors.groupingBy(
                        MpCxCapacityConfiguration::getStructureName,
                        LinkedHashMap::new,
                        Collectors.toList()));

        context.setStructureAllocationMap(structureAllocationMap);
        log.info("加载结构排产配置 {} 条，共 {} 个结构（跨月={}）", allocations.size(), structureAllocationMap.size(), crossMonth);

        // 加载提前生产备用结构排产配置
        loadFutureStructureAllocations(context, scheduleDate);
    }

    /**
     * 加载指定月份的结构排产配置（按排产版本过滤）
     *
     * <p>从 T_MP_STRUCTURE_ALLOCATION 表查询指定年月配置，并按该月月计划表的排产版本过滤。
     *
     * @param factoryCode 工厂编码
     * @param year        年份
     * @param month       月份
     * @return 按版本过滤后的结构排产配置列表
     */
    private List<MpCxCapacityConfiguration> loadMonthAllocations(String factoryCode, int year, int month) {
        List<MpCxCapacityConfiguration> allocations =
                capacityConfigurationMapper.selectByYearAndMonth(factoryCode, year, month);

        final String version = getReleasedVersion(factoryCode, year, month);

        if (version != null && !allocations.isEmpty()) {
            int before = allocations.size();
            allocations = allocations.stream()
                    .filter(a -> version.equals(a.getProductionVersion()))
                    .collect(Collectors.toList());
            log.info("结构排产配置 {}-{} 按排产版本 {} 过滤: {} 条 -> {} 条", year, month, version, before, allocations.size());
        } else if (!allocations.isEmpty()) {
            log.warn("未获取到 {}-{} 的排产版本，结构排产配置未按版本过滤，共 {} 条", year, month, allocations.size());
        }
        return allocations;
    }

    /**
     * 从月计划表获取指定年月的排产版本
     */
    private String getReleasedVersion(String factoryCode, int year, int month) {
        int yearMonth = year * 100 + month;
        List<FactoryMonthPlanProductionFinalResult> monthPlans =
                monthPlanMapper.selectByFactoryAndYearMonth(factoryCode, yearMonth);
        return monthPlans.stream()
                .map(FactoryMonthPlanProductionFinalResult::getProductionVersion)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * 加载提前生产备用的结构排产配置
     *
     * <p>用于提前生产能力：当结构在当日（BEGIN_DAY/END_DAY 日期范围内）无可配置机台但需要提前生产时，
     * 从后续日期或次月的排产配置中查找可用机台。
     *
     * <p>从已加载的 structureAllocationMap（含当月及跨月次月配置）中筛选未来机台：
     * <ul>
     *   <li><b>同月未来机台</b>：T日所在月份中 BEGIN_DAY > T日日期 的机台记录</li>
     *   <li><b>跨月未来机台</b>：排程跨月时，次月的全部配置（已按次月版本过滤）</li>
     * </ul>
     *
     * <p>查询结果存入 context.futureStructureAllocationMap，供 TaskGroupService 的
     * resolveAdvanceMachinesByActualStatus 方法按实际排程日期进一步过滤后使用。
     *
     * @param context      排程上下文
     * @param scheduleDate 排程日期（中间天）
     */
    private void loadFutureStructureAllocations(ScheduleContextVo context, LocalDate scheduleDate) {
        // 排产起始日期 = 中间天 - 1天（T日）
        LocalDate scheduleStartDate = scheduleDate.minusDays(1);
        int currentDay = scheduleStartDate.getDayOfMonth();
        int baseYearMonth = scheduleStartDate.getYear() * 100 + scheduleStartDate.getMonthValue();

        Map<String, List<MpCxCapacityConfiguration>> futureMap = new LinkedHashMap<>();

        // 从 structureAllocationMap 中筛选未来机台：
        // - T日同月：BEGIN_DAY > 当前日期
        // - 未来月（跨月场景）：全部包含
        if (context.getStructureAllocationMap() != null) {
            for (Map.Entry<String, List<MpCxCapacityConfiguration>> entry : context.getStructureAllocationMap().entrySet()) {
                String structureName = entry.getKey();
                List<MpCxCapacityConfiguration> futureConfigs = entry.getValue().stream()
                        .filter(c -> {
                            if (c.getBeginDay() == null || c.getYear() == null || c.getMonth() == null) {
                                return false;
                            }
                            int configYearMonth = c.getYear() * 100 + c.getMonth();
                            if (configYearMonth == baseYearMonth) {
                                // 同月：BEGIN_DAY > 当前日期
                                return c.getBeginDay() > currentDay;
                            }
                            // 未来月：全部包含
                            return configYearMonth > baseYearMonth;
                        })
                        .collect(Collectors.toList());
                if (!futureConfigs.isEmpty()) {
                    futureMap.computeIfAbsent(structureName, k -> new ArrayList<>()).addAll(futureConfigs);
                }
            }
        }

        context.setFutureStructureAllocationMap(futureMap);
        // 初始化提前生产机台分配映射（TaskGroupService 在分组过程中填充）
        context.setAdvanceProductionMachineMap(new HashMap<>());
        log.info("加载提前生产备用配置完成，共 {} 个结构有未来机台配置（基准={}-{}）",
                futureMap.size(), baseYearMonth / 100, baseYearMonth % 100);
    }

    /**
     * 构建产能映射
     */
    private void buildCapacityMaps(ScheduleContextVo context) {
        // 物料日硫化最大产能映射
        Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap = buildMaterialLhCapacityMap(context);
        context.setMaterialLhCapacityMap(materialLhCapacityMap);
        log.info("构建物料日硫化最大产能映射 {} 条", materialLhCapacityMap.size());

        // 结构硫化配比映射
        Map<String, MdmStructureLhRatio> structureLhRatioMap = buildStructureLhRatioMap();
        context.setStructureLhRatioMap(structureLhRatioMap);
        // 同时设置列表，供 BalancingService 使用
        context.setStructureLhRatios(getStructureLhRatios());
        log.info("构建结构硫化配比映射 {} 条", structureLhRatioMap.size());
    }

    /**
     * 1.13 动态计算硫化余量与成型余量（排程前快照 + 运行时基准）。
     *
     * <p><b>硫化余量</b>（按物料）：
     * {@code Max(月计划有效量(断点日前累加) − 月累计完成 − T日班次完成, 0)}。
     * 支持跨月：当月+次月分别计算后合并；断点日 = 月计划逐日有值→无值的前一日。
     *
     * <p><b>成型余量</b>：{@code Max(0, 硫化余量 − 按 lhId 比例分配的胎胚库存)}。
     * 写入 {@code monthSurplusMap}、{@code formingRemainderMap}、{@code materialStockMap} 及 initial* 快照。
     *
     * <p>数据源：t_mp_month_plan_prod_final、T_LH_DAY_FINISH_QTY、T_LH_SCHE_FINISH_QTY、T_CX_STOCK。
     */
    private void loadMonthSurplusAndCalculateFormingRemainder(ScheduleContextVo context, LocalDate scheduleDate) {
        String factoryCode = context.getFactoryCode();
        int scheduleDays = context.getScheduleDays() != null ? context.getScheduleDays() : ScheduleConstants.DEFAULT_SCHEDULE_DAYS;
        LocalDate scheduleEndDate = scheduleDate.plusDays(scheduleDays - 1);

        // 跨月判定：排程结束日的年月 ≠ T日年月
        boolean crossMonth = scheduleEndDate.getMonth() != scheduleDate.getMonth()
                || scheduleEndDate.getYear() != scheduleDate.getYear();

        // 当月（排程起始日所在月）年月
        int prevYear = scheduleDate.getYear();
        int prevMonth = scheduleDate.getMonthValue();
        int prevYearMonth = prevYear * 100 + prevMonth;

        // 次月（排程结束日所在月）年月
        LocalDate nextMonthBase = scheduleEndDate.withDayOfMonth(1);
        int nextYear = nextMonthBase.getYear();
        int nextMonth = nextMonthBase.getMonthValue();
        int nextYearMonth = nextYear * 100 + nextMonth;

        // 1. 查询月计划数据（跨月时查当月+次月两份）
        List<FactoryMonthPlanProductionFinalResult> prevMonthPlans = monthPlanMapper.selectByFactoryAndYearMonth(factoryCode, prevYearMonth);
        List<FactoryMonthPlanProductionFinalResult> nextMonthPlans = crossMonth
                ? monthPlanMapper.selectByFactoryAndYearMonth(factoryCode, nextYearMonth)
                : Collections.emptyList();

        // 按物料+产品状态分组，避免同一物料不同计划类型互相占用余量
        Map<String, List<FactoryMonthPlanProductionFinalResult>> prevPlansByStatusKey = prevMonthPlans.stream()
                .filter(p -> p.getMaterialCode() != null)
                .collect(Collectors.groupingBy(p -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        p.getMaterialCode(), p.getProductStatus())));
        Map<String, List<FactoryMonthPlanProductionFinalResult>> nextPlansByStatusKey = nextMonthPlans.stream()
                .filter(p -> p.getMaterialCode() != null)
                .collect(Collectors.groupingBy(p -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        p.getMaterialCode(), p.getProductStatus())));

        // 2. 收集全部余量账户及查询完成量所需的物料编码
        Set<String> allStatusKeys = new HashSet<>(prevPlansByStatusKey.keySet());
        if (crossMonth) {
            allStatusKeys.addAll(nextPlansByStatusKey.keySet());
        }
        if (context.getLhScheduleResults() != null) {
            context.getLhScheduleResults().stream()
                    .filter(result -> result.getMaterialCode() != null)
                    .map(result -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                            result.getMaterialCode(), result.getProductStatus()))
                    .forEach(allStatusKeys::add);
        }
        List<String> materialCodeList = Stream.concat(prevMonthPlans.stream(), nextMonthPlans.stream())
                .map(FactoryMonthPlanProductionFinalResult::getMaterialCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (context.getLhScheduleResults() != null) {
            context.getLhScheduleResults().stream()
                    .map(LhScheduleResult::getMaterialCode)
                    .filter(Objects::nonNull)
                    .filter(materialCode -> !materialCodeList.contains(materialCode))
                    .forEach(materialCodeList::add);
        }
        List<String> factoryCodeList = Collections.singletonList(factoryCode);

        // 3. 查询已完成量
        // 当月：当月1日 ~ T-1日（不含T日）的月累计完成量 + T日班次完成量
        Date prevMonthStart = Date.from(scheduleDate.withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date scheduleDateStart = Date.from(scheduleDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date nextDayStart = Date.from(scheduleDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        Map<String, Integer> prevDayFinishedQtyMap = new HashMap<>();
        Map<String, Integer> prevScheFinishedQtyMap = new HashMap<>();
        if (!materialCodeList.isEmpty()) {
            // 当月月累计完成量（当月1日 ~ T-1日）
            List<Map<String, Object>> dayFinishList = lhFinishQtyMapper.sumDayFinishQty(
                    factoryCodeList, materialCodeList, prevMonthStart, scheduleDateStart);
            for (Map<String, Object> row : dayFinishList) {
                String mc = (String) row.get("MATERIAL_CODE");
                String productStatus = (String) row.get("LH_TYPE");
                Object qtyObj = row.get("TOTAL_FINISH_QTY");
                int qty = qtyObj != null ? ((Number) qtyObj).intValue() : 0;
                String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(mc, productStatus);
                prevDayFinishedQtyMap.merge(statusKey, qty, Integer::sum);
            }
            // T日班次完成量
            List<Map<String, Object>> scheFinishList = lhFinishQtyMapper.sumScheFinishQty(
                    factoryCodeList, materialCodeList, scheduleDateStart, nextDayStart);
            for (Map<String, Object> row : scheFinishList) {
                String mc = (String) row.get("MATERIAL_CODE");
                String productStatus = (String) row.get("CLASS1_LH_TYPE");
                Object qtyObj = row.get("TOTAL_FINISH_QTY");
                int qty = qtyObj != null ? ((Number) qtyObj).intValue() : 0;
                String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(mc, productStatus);
                prevScheFinishedQtyMap.merge(statusKey, qty, Integer::sum);
            }
        }

        // 次月已完成量（仅当T日落在次月时才需要查询；T日=排程起始日，正常情况下在当月）
        Map<String, Integer> nextDayFinishedQtyMap = new HashMap<>();
        Map<String, Integer> nextScheFinishedQtyMap = new HashMap<>();
        boolean tInNextMonth = crossMonth && scheduleDate.getYear() == nextYear && scheduleDate.getMonthValue() == nextMonth;
        if (tInNextMonth && !materialCodeList.isEmpty()) {
            Date nextMonthStart = Date.from(nextMonthBase.atStartOfDay(ZoneId.systemDefault()).toInstant());
            // 次月月累计完成量（次月1日 ~ T-1日）
            List<Map<String, Object>> nextDayFinishList = lhFinishQtyMapper.sumDayFinishQty(
                    factoryCodeList, materialCodeList, nextMonthStart, scheduleDateStart);
            for (Map<String, Object> row : nextDayFinishList) {
                String mc = (String) row.get("MATERIAL_CODE");
                String productStatus = (String) row.get("LH_TYPE");
                Object qtyObj = row.get("TOTAL_FINISH_QTY");
                int qty = qtyObj != null ? ((Number) qtyObj).intValue() : 0;
                String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(mc, productStatus);
                nextDayFinishedQtyMap.merge(statusKey, qty, Integer::sum);
            }
            // T日班次完成量与当月查询结果相同（同一个T日）
            nextScheFinishedQtyMap = new HashMap<>(prevScheFinishedQtyMap);
        }

        log.info("硫化余量计算：跨月={}, 当月={}-{}, 次月={}, T日在次月={}, 物料数={}",
                crossMonth, prevYear, prevMonth, crossMonth ? nextYear + "-" + nextMonth : "无",
                tInNextMonth, materialCodeList.size());

        // 4. 计算每个物料的硫化余量（按断点日累加计划量，支持跨月）
        List<MdmMonthSurplus> monthSurplusList = new ArrayList<>();
        for (String statusKey : allStatusKeys) {
            List<FactoryMonthPlanProductionFinalResult> prevPlans = prevPlansByStatusKey.getOrDefault(statusKey, Collections.emptyList());
            List<FactoryMonthPlanProductionFinalResult> nextPlans = crossMonth
                    ? nextPlansByStatusKey.getOrDefault(statusKey, Collections.emptyList())
                    : Collections.emptyList();

            FactoryMonthPlanProductionFinalResult representativePlan = Stream.concat(prevPlans.stream(), nextPlans.stream())
                    .findFirst()
                    .orElse(null);
            LhScheduleResult representativeLhResult = context.getLhScheduleResults() == null
                    ? null
                    : context.getLhScheduleResults().stream()
                    .filter(result -> statusKey.equals(MonthPlanSurplusCalculator.buildMaterialStatusKey(
                            result.getMaterialCode(), result.getProductStatus())))
                    .findFirst()
                    .orElse(null);
            String materialCode = representativePlan != null
                    ? representativePlan.getMaterialCode()
                    : representativeLhResult != null ? representativeLhResult.getMaterialCode() : null;
            String productStatus = representativePlan != null
                    ? representativePlan.getProductStatus()
                    : representativeLhResult != null ? representativeLhResult.getProductStatus() : null;
            if (materialCode == null) {
                continue;
            }

            int prevDayFinishedQty = prevDayFinishedQtyMap.getOrDefault(statusKey, 0);
            int prevScheFinishedQty = prevScheFinishedQtyMap.getOrDefault(statusKey, 0);
            int nextDayFinishedQty = nextDayFinishedQtyMap.getOrDefault(statusKey, 0);
            int nextScheFinishedQty = nextScheFinishedQtyMap.getOrDefault(statusKey, 0);

            int surplusQty = this.calculateSurplusQtyBySharedCalculator(
                    prevPlans, nextPlans, scheduleDate, scheduleEndDate,
                    prevDayFinishedQty, prevScheFinishedQty, nextDayFinishedQty, nextScheFinishedQty);

            MdmMonthSurplus surplus = new MdmMonthSurplus();
            surplus.setMaterialCode(materialCode);
            surplus.setProductStatus(productStatus);
            surplus.setPlanSurplusQty(BigDecimal.valueOf(surplusQty));
            monthSurplusList.add(surplus);
        }

        context.setMonthSurplusList(monthSurplusList);

        Map<String, MdmMonthSurplus> monthSurplusMap = monthSurplusList.stream()
                .collect(Collectors.toMap(s -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        s.getMaterialCode(), s.getProductStatus()), s -> s));
        context.setMonthSurplusMap(monthSurplusMap);
        context.setInitialMonthSurplusMap(monthSurplusList.stream()
                .filter(s -> s.getMaterialCode() != null && s.getPlanSurplusQty() != null)
                .collect(Collectors.toMap(s -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                        s.getMaterialCode(), s.getProductStatus()), MdmMonthSurplus::getPlanSurplusQty)));
        log.info("加载月度计划余量 {} 条（按断点日累加计划量-完成量动态计算，支持跨月）", monthSurplusList.size());

        // 获取当前天的班次配置（用于获取硫化任务的班次计划量）
        List<CxShiftConfig> currentDayShifts = getCurrentDayShifts(context);

        // 计算成型余量映射（按物料的日硫化量比例分配库存）
        Map<String, Integer> formingRemainderMap = new HashMap<>();
        Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap = context.getMaterialLhCapacityMap();
        Map<String, Integer> materialStockMap = calculateFormingRemainderMap(
                context.getMaterials(),
                monthSurplusMap,
                context.getStocks(),
                context.getLhScheduleResults(),
                currentDayShifts,
                formingRemainderMap,
                context.getScheduleDate(),
                materialLhCapacityMap);
        context.setFormingRemainderMap(formingRemainderMap);
        context.setInitialFormingRemainderMap(new HashMap<>(formingRemainderMap));
        context.setMaterialStockMap(materialStockMap);
        context.setInitialMaterialStockMap(new HashMap<>(materialStockMap));
        log.info("计算成型余量映射 {} 条，物料库存分配 {} 条", formingRemainderMap.size(), materialStockMap.size());
    }

    /**
     * 计算单条月计划记录的有效计划量。
     *
     * <p>当"上月超欠产有效标志"({@code lastMonthValidFlag})为"1"时，
     * 月计划总量需合并上月欠产数据({@code lastMonthOverdueQty})，以纳入上个月的欠产
     * （此前未被考虑）；标志非"1"或为空时，仅使用本月计划总量({@code totalQty})。
     *
     * <p>异常处理：所有数值字段为 null 时按 0 处理，避免空指针；标志字段为 null 时按非"1"处理。
     *
     * @param plan 月计划记录
     * @return 有效计划量（totalQty，必要时叠加 lastMonthOverdueQty）
     */
    static int calcEffectivePlanQty(FactoryMonthPlanProductionFinalResult plan) {
        int totalQty = plan.getTotalQty() != null ? plan.getTotalQty() : 0;
        String validFlag = plan.getLastMonthValidFlag();
        if ("1".equals(validFlag)) {
            int overdueQty = plan.getLastMonthOverdueQty() != null ? plan.getLastMonthOverdueQty() : 0;
            log.info("物料 {} 上月超欠产有效标志=1，合并上月欠产：本月计划={} + 上月欠产={}",
                    plan.getMaterialCode(), totalQty, overdueQty);
            return totalQty + overdueQty;
        }
        return totalQty;
    }

    /**
     * 使用硫化、成型共享计算器计算单个物料状态账户的硫化余量。
     *
     * @param prevPlans           当月月计划记录（已按物料+产品状态过滤）
     * @param nextPlans           次月月计划记录（已按物料+产品状态过滤）
     * @param scheduleDate        排程起始日
     * @param scheduleEndDate     排程结束日
     * @param prevDayFinishedQty  当月月累计完成量
     * @param prevScheFinishedQty 当月排程班次完成量
     * @param nextDayFinishedQty  次月月累计完成量
     * @param nextScheFinishedQty 次月排程班次完成量
     * @return 硫化余量
     */
    private int calculateSurplusQtyBySharedCalculator(
            List<FactoryMonthPlanProductionFinalResult> prevPlans,
            List<FactoryMonthPlanProductionFinalResult> nextPlans,
            LocalDate scheduleDate, LocalDate scheduleEndDate,
            int prevDayFinishedQty, int prevScheFinishedQty,
            int nextDayFinishedQty, int nextScheFinishedQty) {
        List<FactoryMonthPlanProductionFinalResult> allMonthPlans = Stream.concat(
                prevPlans.stream(), nextPlans.stream()).collect(Collectors.toList());
        if (allMonthPlans.isEmpty()) {
            return 0;
        }

        FactoryMonthPlanProductionFinalResult plan = allMonthPlans.get(0);
        List<Date> productionDates = new ArrayList<>();
        for (LocalDate productionDate = scheduleDate;
             !productionDate.isAfter(scheduleEndDate);
             productionDate = productionDate.plusDays(1)) {
            productionDates.add(MonthPlanSurplusCalculator.getDate(productionDate));
        }

        YearMonth productionYearMonth = YearMonth.from(scheduleDate);
        Map<YearMonth, FactoryMonthPlanProductionFinalResult> hasProductionPlanMap =
                MonthPlanSurplusCalculator.getHasProductionPlan(
                        allMonthPlans, productionDates, plan.getFactoryCode(),
                        plan.getMaterialCode(), plan.getProductStatus());
        Map<YearMonth, Integer> monthOverdueQtyMap =
                MonthPlanSurplusCalculator.getOverdueProduction(
                        false, productionDates, allMonthPlans, plan);
        Map<YearMonth, Integer> monthPlanQtyMap =
                MonthPlanSurplusCalculator.getPlanQty(
                        productionDates, allMonthPlans, plan, 1);
        int finishedQty = prevDayFinishedQty + prevScheFinishedQty
                + nextDayFinishedQty + nextScheFinishedQty;
        int surplusQty = MonthPlanSurplusCalculator.getSurplusQty(
                productionYearMonth, productionDates, hasProductionPlanMap,
                monthOverdueQtyMap, monthPlanQtyMap, finishedQty);
        return Math.max(0, surplusQty);
    }

    /**
     * 查找排程范围内最晚有计划量的日期。
     *
     * @param prevPlans   当月月计划记录
     * @param nextPlans   次月月计划记录
     * @param rangeStart  排程起始日（T日）
     * @param rangeEnd    排程结束日
     * @param crossMonth  是否跨月
     * @return 最晚有计划量的日期；均无计划量返回 null
     */
    private LocalDate findLastPlanDateInRange(
            List<FactoryMonthPlanProductionFinalResult> prevPlans,
            List<FactoryMonthPlanProductionFinalResult> nextPlans,
            LocalDate rangeStart, LocalDate rangeEnd, boolean crossMonth,
            int prevYear, int prevMonth, int nextYear, int nextMonth) {
        LocalDate lastPlanDate = null;
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            boolean inNextMonth = crossMonth && d.getYear() == nextYear && d.getMonthValue() == nextMonth;
            List<FactoryMonthPlanProductionFinalResult> plans = inNextMonth ? nextPlans : prevPlans;
            if (sumDayQty(plans, d.getDayOfMonth()) > 0) {
                lastPlanDate = d;
            }
        }
        return lastPlanDate;
    }

    /**
     * 查找指定月份在排程范围内最晚有计划量的日期。
     * 用于情况B当月部分：在排程范围内找当月最晚有计划量的日期。
     *
     * @param plans      月计划记录
     * @param rangeStart 排程起始日
     * @param rangeEnd   排程结束日
     * @param year/month 目标月份
     * @return 最晚有计划量的日期；无返回 null
     */
    private LocalDate findLastPlanDateInMonthInRange(
            List<FactoryMonthPlanProductionFinalResult> plans,
            LocalDate rangeStart, LocalDate rangeEnd, int year, int month) {
        LocalDate lastPlanDate = null;
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            if (d.getYear() == year && d.getMonthValue() == month) {
                if (sumDayQty(plans, d.getDayOfMonth()) > 0) {
                    lastPlanDate = d;
                }
            }
        }
        return lastPlanDate;
    }

    /**
     * 查找断点日：从 startDay 开始往后扫描，跳过无值日，找到第一个"有值→无值"转折点。
     * <p>
     * 断点定义：月计划前日有值、当日无值 → 前日为断点日。
     * 一直有值到月末 → 断点日 = 月末。全无值 → 返回0。
     * </p>
     *
     * @param plans    月计划记录列表（同一物料，可能多条）
     * @param startDay 开始扫描的日序号(1-31)
     * @param year/month 用于确定月份天数
     * @return 断点日序号；全无值返回0
     */
    private int findBreakpointDay(List<FactoryMonthPlanProductionFinalResult> plans, int startDay, int year, int month) {
        if (plans == null || plans.isEmpty()) {
            return 0;
        }
        int maxDay = YearMonth.of(year, month).lengthOfMonth();
        // 跳过无值日，找第一个有值日
        int firstValidDay = 0;
        for (int day = startDay; day <= maxDay; day++) {
            if (sumDayQty(plans, day) > 0) {
                firstValidDay = day;
                break;
            }
        }
        if (firstValidDay == 0) {
            return 0;
        }
        // 从 firstValidDay 往后找断点：有值→无值 的转折点
        for (int day = firstValidDay; day < maxDay; day++) {
            if (sumDayQty(plans, day) > 0 && sumDayQty(plans, day + 1) <= 0) {
                return day;
            }
        }
        // 一直有值到月末
        return maxDay;
    }

    /**
     * 累加某日所有记录的计划量（同一物料可能有多条记录）。
     *
     * @param plans 月计划记录列表
     * @param day   日序号(1-31)
     * @return 该日计划量合计；无值返回0
     */
    private int sumDayQty(List<FactoryMonthPlanProductionFinalResult> plans, int day) {
        if (plans == null || plans.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (FactoryMonthPlanProductionFinalResult p : plans) {
            Integer qty = p.getDayQty(day);
            if (qty != null && qty > 0) {
                sum += qty;
            }
        }
        return sum;
    }

    /**
     * 累加 day1 ~ toDay 的计划量。
     *
     * @param plans 月计划记录列表
     * @param toDay 截止日序号(1-31)
     * @return 计划量合计
     */
    private int sumPlanQtyByDayRange(List<FactoryMonthPlanProductionFinalResult> plans, int toDay) {
        if (plans == null || plans.isEmpty() || toDay <= 0) {
            return 0;
        }
        int sum = 0;
        for (int day = 1; day <= toDay; day++) {
            sum += sumDayQty(plans, day);
        }
        return sum;
    }

    /**
     * 获取有效上月超欠产量（lastMonthValidFlag="1"时累加 lastMonthOverdueQty）。
     *
     * @param plans 月计划记录列表
     * @return 上月超欠产合计
     */
    private int getEffectiveOverdueQty(List<FactoryMonthPlanProductionFinalResult> plans) {
        if (plans == null || plans.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (FactoryMonthPlanProductionFinalResult p : plans) {
            if ("1".equals(p.getLastMonthValidFlag())) {
                sum += p.getLastMonthOverdueQty() != null ? p.getLastMonthOverdueQty() : 0;
            }
        }
        return sum;
    }

    /**
     * 获取当前排程日期的班次配置
     */
    private List<CxShiftConfig> getCurrentDayShifts(ScheduleContextVo context) {
        LocalDate scheduleDate = context.getScheduleDate();
        List<CxShiftConfig> allShifts = context.getShiftConfigList();
        if (allShifts == null || scheduleDate == null) {
            return new ArrayList<>();
        }
        // 获取第1天的班次配置
        return allShifts.stream()
                .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == 1)
                .collect(Collectors.toList());
    }

    /**
     * 加载SKU排产分类
     */
    private void loadSkuCategories(ScheduleContextVo context) {
        List<MdmSkuScheduleCategory> skuCategories = skuScheduleCategoryMapper.selectAllCategories();
        context.setSkuScheduleCategories(skuCategories);

        Set<String> mainProductCodes = skuCategories.stream()
                .filter(c -> MAIN_PRODUCT_SCHEDULE_TYPE.equals(c.getScheduleType()))
                .map(MdmSkuScheduleCategory::getMaterialCode)
                .collect(Collectors.toSet());
        context.setMainProductCodes(mainProductCodes);
        log.info("加载SKU排产分类 {} 条，其中主销产品 {} 个", skuCategories.size(), mainProductCodes.size());
    }

    // ==================== 校验与持久化 ====================

    /**
     * 委托 {@link ScheduleDataValidator} 执行策略模式校验（ERROR 阻断 / WARN 日志）。
     */
    private ScheduleDataValidationResult validateScheduleData(ScheduleContextVo context, LocalDate scheduleDate, String factoryCode) {
        ScheduleDataValidationResult validationResult = scheduleDataValidator.validate(context, scheduleDate, factoryCode);

        if (!validationResult.isPassed()) {
            log.error("数据完整性校验不通过：{}", validationResult.generateSummary());
            for (ScheduleDataValidationResult.ValidationDetail detail : validationResult.getDetails()) {
                if (detail.getLevel() == ScheduleDataValidationResult.ValidationLevel.ERROR) {
                    log.error("  [错误] {} - {} | 建议：{}", detail.getDataItem(), detail.getMessage(), detail.getSuggestion());
                }
            }
        }

        if (validationResult.getWarnCount() > 0) {
            log.warn("数据完整性校验存在警告，请检查日志：{}", validationResult.generateSummary());
        }

        return validationResult;
    }

    // ==================== 私有方法：排程结果相关 ====================

    /**
     * 将校验明细转换为API返回的ValidationDetail列表
     */
    private List<ValidationDetail> convertValidationDetails(
            ScheduleDataValidationResult validationResult,
            ScheduleDataValidationResult.ValidationLevel level) {
        List<ValidationDetail> result = new ArrayList<>();
        for (ScheduleDataValidationResult.ValidationDetail detail : validationResult.getDetails()) {
            if (detail.getLevel() == level) {
                result.add(new ValidationDetail(
                        detail.getDataItem(), detail.getMessage(), detail.getSuggestion()));
            }
        }
        return result;
    }

    /**
     * 持久化排程结果：主表 {@code T_CX_SCHEDULE_RESULT} + 子表 {@code T_CX_SCHEDULE_DETAIL}（随主表 details 级联写入）。
     *
     * @param results      排程结果列表
     * @param scheduleDate 排程日期
     * @param context      排程上下文（含 previousShiftMachineEmbryoLoadMap 最后班次分配结果）
     */
    public void saveScheduleResults(List<CxScheduleResult> results, LocalDate scheduleDate, ScheduleContextVo context) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        // 检查是否有重复的taskKey(机台+胎胚+物料+施工阶段)
        java.util.Map<String, java.util.List<CxScheduleResult>> keyMap = new java.util.HashMap<>();
        for (CxScheduleResult r : results) {
            String key = r.getCxMachineCode() + "|" + r.getEmbryoCode() + "|" + r.getMaterialCode() + "|" + r.getClass1RecipeType();
            keyMap.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(r);
        }
        for (java.util.Map.Entry<String, java.util.List<CxScheduleResult>> entry : keyMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                log.warn("主表重复taskKey: {}, 记录数={}", entry.getKey(), entry.getValue().size());
                for (CxScheduleResult r : entry.getValue()) {
                    log.warn("  重复记录: ID={}, machine={}, embryo={}, material={}, recipeType={}, class1Qty={}, lhIds={}",
                            r.getId(), r.getCxMachineCode(), r.getEmbryoCode(), r.getMaterialCode(),
                            r.getClass1RecipeType(), r.getClass1PlanQty(), r.getLhScheduleIds());
                }
            }
        }

        for (CxScheduleResult result : results) {
            result.setCreateTime(new Date());
            scheduleResultMapper.insert(result);

            // 保存子表明细
            List<CxScheduleDetail> details = result.getDetails();
            if (details != null && !details.isEmpty()) {
                for (CxScheduleDetail detail : details) {
                    detail.setMainId(result.getId());
                    detail.setCreateTime(new Date());
                }
                scheduleDetailService.batchSave(details);
                log.info("机台 {} 保存子表明细 {} 条", result.getCxMachineCode(), details.size());
            }
        }
        log.info("保存排程结果 {} 条（含子表）", results.size());

        // 持久化班次级机台胎胚负荷映射（供下次排程动态保底预留）
        saveShiftMachineLoad(scheduleDate, context);
    }

    /**
     * 持久化班次级机台胎胚负荷映射到 T_CX_SHIFT_MACHINE_LOAD。
     *
     * <p>先删除当日旧数据，再从 context.previousShiftMachineEmbryoLoadMap（最后一个班次的分配结果）
     * 构建记录并批量插入。
     *
     * @param scheduleDate 排程日期
     * @param context      排程上下文
     */
    private void saveShiftMachineLoad(LocalDate scheduleDate, ScheduleContextVo context) {
        String factoryCode = context.getFactoryCode();
        // 先删除当日旧数据
        cxShiftMachineLoadMapper.deleteByDate(scheduleDate, factoryCode);

        Map<String, Map<String, Integer>> loadMap = context.getPreviousShiftMachineEmbryoLoadMap();
        if (loadMap == null || loadMap.isEmpty()) {
            log.info("无班次负荷数据需要持久化");
            return;
        }

        // 获取班次信息（取最后一个班次的编码和序号）
        List<CxShiftConfig> shiftConfigs = context.getShiftConfigList();
        String lastShiftCode = "";
        int lastShiftOrder = 0;
        if (shiftConfigs != null && !shiftConfigs.isEmpty()) {
            CxShiftConfig lastShift = shiftConfigs.get(shiftConfigs.size() - 1);
            lastShiftCode = lastShift.getShiftCode();
            lastShiftOrder = shiftConfigs.size();
        }

        List<CxShiftMachineLoad> loads = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> machineEntry : loadMap.entrySet()) {
            for (Map.Entry<String, Integer> embryoEntry : machineEntry.getValue().entrySet()) {
                CxShiftMachineLoad load = new CxShiftMachineLoad();
                load.setScheduleDate(scheduleDate);
                load.setShiftCode(lastShiftCode);
                load.setShiftOrder(lastShiftOrder);
                load.setCxMachineCode(machineEntry.getKey());
                load.setEmbryoCode(embryoEntry.getKey());
                load.setLhMachineCount(embryoEntry.getValue());
                load.setFactoryCode(factoryCode);
                load.setCreateTime(new Date());
                loads.add(load);
            }
        }

        // 批量插入
        for (CxShiftMachineLoad load : loads) {
            cxShiftMachineLoadMapper.insert(load);
        }
        log.info("持久化班次负荷数据: 日期={}, 班次={}, {}条记录", scheduleDate, lastShiftCode, loads.size());
    }

    /**
     * 验证排程结果
     *
     * @param results 排程结果列表
     * @return 是否全部通过验证
     */
    private boolean validateScheduleResults(List<CxScheduleResult> results) {
        if (CollectionUtils.isEmpty(results)) {
            return false;
        }

        int validCount = 0;
        for (CxScheduleResult result : results) {
            ConstraintCheckService.ConstraintCheckResult checkResult = constraintCheckService.checkAllConstraints(result);
            if (checkResult.isPassed()) {
                validCount++;
            } else {
                log.warn("排程结果存在约束冲突，机台：{}，物料：{}，冲突：{}",
                        result.getCxMachineCode(), result.getEmbryoCode(), checkResult.getViolations());
            }
        }

        return validCount == results.size();
    }

    // ==================== 私有方法：产能映射构建 ====================

    /**
     * 构建物料日硫化产能映射
     *
     * @param context 排程上下文
     * @return 物料日硫化产能映射
     */
    private Map<String, MonthPlanProductLhCapacityVo> buildMaterialLhCapacityMap(ScheduleContextVo context) {
        Map<String, MonthPlanProductLhCapacityVo> resultMap = new HashMap<>();

        try {
            DayVulcanizationModeEnum mode = getDayVulcanizationMode(context);
            log.info("日硫化量计算模式: {}", mode.getDesc());

            String factoryCode = context.getFactoryCode();
            List<MonthPlanProductLhCapacityVo> baseCapacities = monthPlanProductLhCapacityMapper.selectByFactoryCode(factoryCode);

            for (MonthPlanProductLhCapacityVo vo : baseCapacities) {
                String materialCode = vo.getMaterialCode();
                if (materialCode == null) {
                    continue;
                }
                vo.calculateDayVulcanizationQty(mode);
                resultMap.put(materialCode, vo);
            }

            log.info("从基础表构建物料日硫化产能映射（工厂:{}），共 {} 个物料", factoryCode, resultMap.size());

        } catch (Exception e) {
            log.error("构建物料日硫化产能映射失败", e);
        }

        return resultMap;
    }

    /**
     * 获取日硫化量计算模式
     */
    private DayVulcanizationModeEnum getDayVulcanizationMode(ScheduleContextVo context) {
        String mpValue = loadFactoryParamValue(factoryParamMapper, context.getFactoryCode(), "SYS0202002");
        if (mpValue != null) {
            String converted = productionCalculator.convertDayVulcanizationMode(mpValue);
            log.info("日硫化量计算模式（来自T_MP_FACTORY_PARAM SYS0202002）: {} -> {}", mpValue, converted);
            return DayVulcanizationModeEnum.getByCode(converted);
        }

        Map<String, CxParamConfig> paramConfigMap = context.getParamConfigMap();
        if (paramConfigMap == null) {
            return DayVulcanizationModeEnum.STANDARD_CAPACITY;
        }

        CxParamConfig modeConfig = paramConfigMap.get(PARAM_CODE_DAY_VULCANIZATION_MODE);
        if (modeConfig != null && modeConfig.getParamValue() != null) {
            log.info("日硫化量计算模式（来自T_CX_PARAM_CONFIG）: {}", modeConfig.getParamValue());
            return DayVulcanizationModeEnum.getByCode(modeConfig.getParamValue());
        }

        return DayVulcanizationModeEnum.STANDARD_CAPACITY;
    }

    /**
     * 构建结构硫化配比映射
     *
     * <p>key = 机型编码 + "|" + 结构名称，兼容同一结构在不同机型上有不同配比的情况
     *
     * @return 结构硫化配比映射
     */
    private Map<String, MdmStructureLhRatio> buildStructureLhRatioMap() {
        Map<String, MdmStructureLhRatio> resultMap = new HashMap<>();

        try {
            List<MdmStructureLhRatio> ratios = structureLhRatioMapper.selectList(null);
            for (MdmStructureLhRatio ratio : ratios) {
                String structureName = ratio.getStructureName();
                String machineTypeCode = ratio.getCxMachineTypeCode();
                if (structureName != null && machineTypeCode != null) {
                    resultMap.put(machineTypeCode + "|" + structureName, ratio);
                }
            }
            log.info("从结构硫化配比表构建映射，共 {} 条（机型+结构维度）", resultMap.size());

        } catch (Exception e) {
            log.error("构建结构硫化配比映射失败", e);
        }

        return resultMap;
    }

    /**
     * 获取结构硫化配比列表
     */
    private List<MdmStructureLhRatio> getStructureLhRatios() {
        try {
            return structureLhRatioMapper.selectList(null);
        } catch (Exception e) {
            log.error("获取结构硫化配比列表失败", e);
            return Collections.emptyList();
        }
    }

    // ==================== 私有方法：成型余量计算 ====================

    /**
     * 计算成型余量映射
     *
     * <p>功能：
     * <ul>
     *   <li>按硫化任务的日硫化量比例分配共用胎胚库存</li>
     *   <li>最后一条物料用倒扣形式（总库存 - 已分配）</li>
     * </ul>
     *
     * @param materials              物料信息列表
     * @param monthSurplusMap        月度计划硫化余量映射
     * @param stocks                 胎胚库存列表
     * @param lhScheduleResults      硫化排程结果（用于获取班次计划量作为需求比例）
     * @param dayShifts              当前天班次配置
     * @param formingRemainderMap    成型余量映射（输出参数）
     * @param scheduleDate           排程日期
     * @param materialLhCapacityMap  物料日硫化产能映射（用于获取日硫化量）
     * @return 物料库存映射（按物料编码分配库存）
     */
    private Map<String, Integer> calculateFormingRemainderMap(
            List<MdmMaterialInfo> materials,
            Map<String, MdmMonthSurplus> monthSurplusMap,
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts,
            Map<String, Integer> formingRemainderMap,
            LocalDate scheduleDate,
            Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap) {

        // 用于返回的物料库存映射
        Map<String, Integer> materialStockMap = new HashMap<>();

        try {
            // 按硫化任务维度分配库存，共用胎胚按日硫化量比例分配
            materialStockMap = allocateStockByMaterialRatio(stocks, lhScheduleResults, dayShifts, scheduleDate, materialLhCapacityMap, monthSurplusMap);
            log.debug("按硫化任务维度分配胎胚库存 {} 条", materialStockMap.size());

            // 按物料+产品状态汇总库存（从 materialStockMap 按硫化任务汇总）
            Map<String, Integer> stockByMaterialStatus = new HashMap<>();
            if (lhScheduleResults != null) {
                for (LhScheduleResult lh : lhScheduleResults) {
                    if (lh.getMaterialCode() != null && lh.getId() != null) {
                        String taskKey = String.valueOf(lh.getId());
                        int stock = materialStockMap.getOrDefault(taskKey, 0);
                        String materialStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                lh.getMaterialCode(), lh.getProductStatus());
                        stockByMaterialStatus.merge(materialStatusKey, stock, Integer::sum);
                    }
                }
            }

            // 计算成型余量
            for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
                String materialStatusKey = entry.getKey();
                MdmMonthSurplus surplus = entry.getValue();

                int vulcanizingRemainder = surplus.getPlanSurplusQty() != null
                        ? surplus.getPlanSurplusQty().intValue() : 0;
                int materialStock = stockByMaterialStatus.getOrDefault(materialStatusKey, 0);
                int formingRemainder = Math.max(0, vulcanizingRemainder - materialStock);

                formingRemainderMap.put(materialStatusKey, formingRemainder);
            }

            log.info("计算成型余量映射完成，共 {} 条", formingRemainderMap.size());
            for (Map.Entry<String, Integer> entry : formingRemainderMap.entrySet()) {
                log.info("成型余量: materialCode={}, formingRemainder={}", entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Integer> entry : materialStockMap.entrySet()) {
                log.info("物料库存分配: taskKey={}, stock={}", entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, MdmMonthSurplus> entry : monthSurplusMap.entrySet()) {
                log.info("硫化余量: materialStatusKey={}, planSurplusQty={}", entry.getKey(),
                        entry.getValue().getPlanSurplusQty());
            }

        } catch (Exception e) {
            log.error("计算成型余量映射失败", e);
        }

        return materialStockMap;
    }

    /**
     * 单轮分配：将剩余库存按比例分配给尚有硫化余量容量的物料，最后一个倒扣，受硫化余量封顶。
     *
     * @param allocations    分配追踪列表
     * @param remainingStock 本轮可用库存
     * @param demandZero     总需求是否为0（是则等权分配）
     * @return 本轮实际分配量
     */
    private int distributeRound(List<StockTaskAllocation> allocations, int remainingStock, boolean demandZero) {
        Map<String, Integer> allocatedByStatusKey = allocations.stream()
                .collect(Collectors.groupingBy(allocation -> MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                allocation.getMaterialCode(), allocation.getProductStatus()),
                        Collectors.summingInt(StockTaskAllocation::getAllocated)));
        // 同一物料状态账户下的多个硫化任务共享余量上限
        List<StockTaskAllocation> withCapacity = allocations.stream()
                .filter(allocation -> {
                    String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                            allocation.getMaterialCode(), allocation.getProductStatus());
                    return allocatedByStatusKey.getOrDefault(statusKey, 0) < allocation.getSurplus();
                })
                .collect(Collectors.toList());

        if (withCapacity.isEmpty()) {
            return 0;
        }

        int totalCapacityDemand;
        if (demandZero) {
            totalCapacityDemand = withCapacity.size();
        } else {
            totalCapacityDemand = withCapacity.stream().mapToInt(StockTaskAllocation::getDemand).sum();
            if (totalCapacityDemand == 0) {
                totalCapacityDemand = withCapacity.size();
            }
        }

        int roundAllocated = 0;
        for (int i = 0; i < withCapacity.size(); i++) {
            StockTaskAllocation a = withCapacity.get(i);
            int add;
            if (i == withCapacity.size() - 1) {
                // 最后一个：倒扣，确保库存不丢失
                add = remainingStock - roundAllocated;
            } else {
                if (demandZero) {
                    add = remainingStock / withCapacity.size();
                } else {
                    add = (int) ((long) remainingStock * a.getDemand() / totalCapacityDemand);
                }
            }
            String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                    a.getMaterialCode(), a.getProductStatus());
            int cap = a.getSurplus() - allocatedByStatusKey.getOrDefault(statusKey, 0);
            int actual = Math.min(add, cap);
            a.setAllocated(a.getAllocated() + actual);
            allocatedByStatusKey.merge(statusKey, actual, Integer::sum);
            roundAllocated += actual;
        }
        return roundAllocated;
    }

    /**
     * 从硫化记录获取对应班次的计划量
     *
     * @param lhResult   硫化记录
     * @param dayShifts  当前天班次配置
     * @return 对应班次的硫化计划量
     */
    private int getShiftPlanQtyFromLhResult(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts,
                                            LocalDate scheduleDate) {
        return getShiftPlanQtyWithShiftName(lhResult, dayShifts, scheduleDate).getPlanQty();
    }

    /**
     * 从硫化记录中获取第一个非停产班次的计划量和班次名称
     * 按天遍历，遇到停产班次跳过，遇到停产天也跳过
     */
    private ShiftPlanResult getShiftPlanQtyWithShiftName(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts,
                                                         LocalDate scheduleDate) {
        List<MdmWorkCalendar> workCalendarList = workCalendarMapper.selectList(null);
        return getShiftPlanQtyWithShiftName(lhResult, dayShifts, scheduleDate, workCalendarList);
    }

    /**
     * 从硫化记录中获取第一个非停产班次的计划量和班次名称
     * 按天遍历，遇到停产班次跳过，遇到停产天也跳过
     */
    private ShiftPlanResult getShiftPlanQtyWithShiftName(LhScheduleResult lhResult, List<CxShiftConfig> dayShifts,
                                                         LocalDate scheduleDate, List<MdmWorkCalendar> workCalendarList) {
        int defaultQty = lhResult.getDailyPlanQty() != null ? lhResult.getDailyPlanQty() : 0;
        if (dayShifts == null || dayShifts.isEmpty()) {
            return new ShiftPlanResult(defaultQty, "未知");
        }

        // 构建 日期→WorkCalendar 映射
        Map<LocalDate, MdmWorkCalendar> calendarMap = new HashMap<>();
        if (workCalendarList != null) {
            for (MdmWorkCalendar cal : workCalendarList) {
                if (cal.getCalendarTime() != null) {
                    LocalDate calDate = cal.getCalendarTime().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate();
                    calendarMap.put(calDate, cal);
                }
            }
        }

        // 按天遍历，找到第一个非停产班次
        int scheduleDays = dayShifts.stream()
                .mapToInt(s -> s.getScheduleDay() != null ? s.getScheduleDay() : 1)
                .max().orElse(1);

        for (int day = 1; day <= scheduleDays; day++) {
            // 计算该天的日期
            LocalDate currentDate = scheduleDate.plusDays(day - 1);

            // 获取该天的班次配置
            final int currentDay = day;
            List<CxShiftConfig> dayConfigs = dayShifts.stream()
                    .filter(s -> s.getScheduleDay() != null && s.getScheduleDay() == currentDay)
                    .collect(Collectors.toList());

            if (dayConfigs.isEmpty()) continue;

            // 获取该天的日历信息
            MdmWorkCalendar calendar = calendarMap.get(currentDate);

            // 如果整天停产（dayFlag=0 或 三个班次全部停产），跳到下一天
            if (calendar != null && scheduleDayTypeHelper.isFullDayStopped(calendar)) {
                log.debug("日期 {} 整天停产，跳过", currentDate);
                continue;
            }

            for (CxShiftConfig shiftConfig : dayConfigs) {
                String classField = shiftConfig.getClassField();
                if (classField == null || !classField.startsWith("CLASS")) continue;

                // 检查该班次是否停产
                if (calendar != null && scheduleDayTypeHelper.isShiftStopped(calendar, shiftConfig)) {
                    log.debug("日期 {} 班次 {} 停产，跳过", currentDate, classField);
                    continue;
                }

                try {
                    int classIndex = Integer.parseInt(classField.substring(5));
                    Integer planQty = productionCalculator.getClassPlanQtyByIndex(lhResult, classIndex);
                    if (planQty != null && planQty > 0) {
                        String shiftName = shiftConfig.getShiftCode() != null
                                ? shiftConfig.getShiftCode() : classField;
                        return new ShiftPlanResult(planQty, shiftName);
                    }
                } catch (NumberFormatException e) {
                    log.warn("无法解析班次字段: {}", classField);
                }
            }
        }

        return new ShiftPlanResult(defaultQty, "日计划");
    }

    /**
     * 按日硫化量比例分配胎胚库存到硫化任务
     *
     * <p>当一个胎胚被多个物料共用时，按各物料的日硫化量比例分配库存
     * <p>最后一条硫化任务用倒扣形式（总库存 - 已分配）
     *
     * @param stocks                 胎胚库存列表
     * @param lhScheduleResults      硫化排程结果列表
     * @param dayShifts              当前天班次配置
     * @param scheduleDate           排程日期
     * @param materialLhCapacityMap  物料日硫化产能映射（用于获取日硫化量）
     * @param monthSurplusMap        月度硫化余量映射（硫化余量<=0的任务跳过分配）
     * @return 硫化任务ID → 分配的库存数量
     */
    private Map<String, Integer> allocateStockByMaterialRatio(
            List<CxStock> stocks,
            List<LhScheduleResult> lhScheduleResults,
            List<CxShiftConfig> dayShifts,
            LocalDate scheduleDate,
            Map<String, MonthPlanProductLhCapacityVo> materialLhCapacityMap,
            Map<String, MdmMonthSurplus> monthSurplusMap) {

        Map<String, Integer> materialStockMap = new HashMap<>();

        for (CxStock stock : stocks) {
            String embryoCode = stock.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            int totalStock = stock.getEffectiveStock();
            if (totalStock <= 0) {
                continue;
            }

            // 找到该胎胚对应的所有硫化任务
            List<LhScheduleResult> relatedTasks = new ArrayList<>();
            for (LhScheduleResult lh : lhScheduleResults) {
                if (embryoCode.equals(lh.getEmbryoCode())) {
                    relatedTasks.add(lh);
                }
            }

            if (relatedTasks.isEmpty()) {
                // 胎胚没有对应的硫化任务，跳过
                log.debug("胎胚 {} 没有对应的硫化任务，跳过", embryoCode);
                continue;
            }

            if (relatedTasks.size() == 1) {
                // 胎胚只对应一个硫化任务，直接分配全部库存
                LhScheduleResult task = relatedTasks.get(0);
                String taskKey = String.valueOf(task.getId());

                // 检查硫化余量：如果已超产（<=0），跳过分配
                if (productionCalculator.isVulcanizeSurplusExhausted(
                        task.getMaterialCode(), task.getProductStatus(), monthSurplusMap)) {
                    log.debug("胎胚 {} 硫化任务 {} 硫化余量<=0，跳过库存分配", embryoCode, taskKey);
                    continue;
                }

                int surplus = productionCalculator.getVulcanizingSurplus(
                        task.getMaterialCode(), task.getProductStatus(), monthSurplusMap);
                int allocatedStock = Math.min(totalStock, surplus);
                materialStockMap.merge(taskKey, allocatedStock, Integer::sum);
                log.debug("胎胚 {} 只对应硫化任务 {}，按状态余量上限分配库存 {}",
                        embryoCode, taskKey, allocatedStock);
            } else {
                // 胎胚对应多个硫化任务，按物料的日硫化量比例分配
                int totalDemand = 0;
                List<TaskDemand> taskDemands = new ArrayList<>();

                for (LhScheduleResult lh : relatedTasks) {
                    String materialCode = lh.getMaterialCode();
                    int dayVulcanizationQty = 0;

                    // 检查硫化余量：如果已超产（<=0），跳过分配
                    if (productionCalculator.isVulcanizeSurplusExhausted(
                            materialCode, lh.getProductStatus(), monthSurplusMap)) {
                        log.debug("胎胚 {} 硫化任务 {} 物料 {} 硫化余量<=0，跳过库存分配",
                                embryoCode, lh.getId(), materialCode);
                        continue;
                    }

                    // 从 materialLhCapacityMap 获取日硫化量（用于比例计算，已按参数模式计算）
                    if (materialLhCapacityMap != null && materialCode != null) {
                        MonthPlanProductLhCapacityVo capacityVo = materialLhCapacityMap.get(materialCode);
                        if (capacityVo != null) {
                            dayVulcanizationQty = capacityVo.getDayVulcanizationQty() != null
                                    ? capacityVo.getDayVulcanizationQty() : 0;
                        }
                    }

                    if (dayVulcanizationQty <= 0) {
                        log.debug("胎胚 {} 硫化任务 {} 日硫化量=0，跳过分配", embryoCode, lh.getId());
                        continue;
                    }

                    taskDemands.add(new TaskDemand(String.valueOf(lh.getId()), dayVulcanizationQty,
                            materialCode, lh.getProductStatus(), "日硫化量"));
                    totalDemand += dayVulcanizationQty;
                }

                if (taskDemands.isEmpty()) {
                    log.debug("胎胚 {} 对应多个硫化任务但全部被过滤（无有效日硫化量），跳过分配", embryoCode);
                    continue;
                }

                // 构建分配追踪列表，每个任务记录硫化余量上限
                List<StockTaskAllocation> allocations = new ArrayList<>();
                for (TaskDemand td : taskDemands) {
                    int surplus = productionCalculator.getVulcanizingSurplus(
                            td.getMaterialCode(), td.getProductStatus(), monthSurplusMap);
                    allocations.add(new StockTaskAllocation(td, 0, surplus));
                }

                // 多轮分配：每轮按日硫化量比例分配给尚有容量的物料，最后一个倒扣
                boolean demandZero = (totalDemand == 0);
                int remaining = totalStock;
                for (int round = 1; remaining > 0; round++) {
                    int roundAllocated = distributeRound(allocations, remaining, demandZero);
                    if (roundAllocated == 0) {
                        // 所有状态账户均达到硫化余量上限，剩余物理库存保留为未分配库存
                        log.debug("胎胚 {} 所有物料状态账户已达硫化余量上限，剩余库存 {} 不再分配",
                                embryoCode, remaining);
                        break;
                    }
                    remaining -= roundAllocated;
                    log.debug("胎胚 {} 第{}轮分配完成，本轮分配 {}，剩余 {}", embryoCode, round, roundAllocated, remaining);
                }

                // 写回分配结果
                for (StockTaskAllocation a : allocations) {
                    materialStockMap.merge(a.getTaskKey(), a.getAllocated(), Integer::sum);
                    log.debug("物料编码 {}，胎胚 {} 共用分配：硫化任务 {} 日硫化量 {}，分配库存 {}（硫化余量上限={}）",
                            a.getMaterialCode(), embryoCode, a.getTaskKey(), a.getDemand(), a.getAllocated(), a.getSurplus());
                }
            }
        }

        return materialStockMap;
    }

    /**
     * 加载物料收尾信息并计算收尾日
     *
     * <p>流程：
     * <ol>
     *   <li>从 T_CX_MATERIAL_ENDING 表加载已存在的收尾信息</li>
     *   <li>对于没有收尾信息的物料，从月计划计算收尾日</li>
     *   <li>计算成型余量、预计收尾天数、紧急收尾标记等</li>
     * </ol>
     *
     * @param context      排程上下文
     * @param scheduleDate 排程日期
     */
    private void loadMaterialEndings(ScheduleContextVo context, LocalDate scheduleDate) {
        int year = scheduleDate.getYear();
        int month = scheduleDate.getMonthValue();
        int currentDay = scheduleDate.getDayOfMonth();
        int lastDayOfMonth = scheduleDate.withDayOfMonth(scheduleDate.lengthOfMonth()).getDayOfMonth();
        Integer yearMonth = year * 100 + month;

        log.info("物料收尾计算：使用排产起始日期={}(第{}天), 月末={}日, 年月={}", scheduleDate, currentDay, lastDayOfMonth, yearMonth);

        // 1. 尝试从数据库加载已存在的收尾信息
        List<CxMaterialEnding> existingEndings = materialEndingMapper.selectByStatDate(scheduleDate);

        // 2. 获取月计划数据（按工厂过滤）
        List<FactoryMonthPlanProductionFinalResult> monthPlans = monthPlanMapper.selectByFactoryAndYearMonth(context.getFactoryCode(), yearMonth);
        Map<String, List<FactoryMonthPlanProductionFinalResult>> materialPlanMap = monthPlans.stream()
                .filter(p -> p.getMaterialCode() != null)
                .collect(Collectors.groupingBy(FactoryMonthPlanProductionFinalResult::getMaterialCode));

        // 3. 获取物料信息和库存
        Map<String, MdmMaterialInfo> materialMap = context.getMaterials() != null
                ? context.getMaterials().stream()
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, m -> m, (a, b) -> a))
                : new HashMap<>();

        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap() != null
                ? context.getFormingRemainderMap()
                : new HashMap<>();

        Map<String, MdmMonthSurplus> monthSurplusMap = context.getMonthSurplusMap() != null
                ? context.getMonthSurplusMap()
                : new HashMap<>();
        Map<String, Integer> formingRemainderByMaterial = new HashMap<>();
        formingRemainderMap.forEach((statusKey, remainder) -> {
            MdmMonthSurplus surplus = monthSurplusMap.get(statusKey);
            if (surplus != null && surplus.getMaterialCode() != null && remainder != null) {
                formingRemainderByMaterial.merge(surplus.getMaterialCode(), remainder, Integer::sum);
            }
        });
        Map<String, BigDecimal> monthSurplusByMaterial = monthSurplusMap.values().stream()
                .filter(surplus -> surplus.getMaterialCode() != null && surplus.getPlanSurplusQty() != null)
                .collect(Collectors.toMap(MdmMonthSurplus::getMaterialCode, MdmMonthSurplus::getPlanSurplusQty,
                        BigDecimal::add));

        // 4. 如果已有收尾信息，直接使用
        if (!existingEndings.isEmpty()) {
            context.setMaterialEndings(existingEndings);
            log.info("从数据库加载物料收尾信息 {} 条", existingEndings.size());
            return;
        }

        // 5. 计算每个物料的收尾信息
        List<CxMaterialEnding> materialEndings = new ArrayList<>();

        // 获取所有需要处理的物料编码（硫化排程中的物料）
        Set<String> materialCodes = new HashSet<>();
        if (context.getLhScheduleResults() != null) {
            materialCodes.addAll(context.getLhScheduleResults().stream()
                    .map(LhScheduleResult::getMaterialCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
        }
        // 也包含月计划中的物料
        materialCodes.addAll(materialPlanMap.keySet());

        for (String materialCode : materialCodes) {
            CxMaterialEnding ending = new CxMaterialEnding();
            ending.setMaterialCode(materialCode);
            ending.setStatDate(scheduleDate);

            // 获取物料信息
            MdmMaterialInfo material = materialMap.get(materialCode);
            if (material != null) {
                ending.setMaterialDesc(material.getMaterialDesc());
                ending.setStructureName(material.getStructureName());
            }

            // 获取硫化余量
            BigDecimal materialMonthSurplus = monthSurplusByMaterial.get(materialCode);
            if (materialMonthSurplus != null) {
                ending.setVulcanizingRemainder(materialMonthSurplus.intValue());
            }

            // 获取成型余量
            Integer formingRemainder = formingRemainderByMaterial.get(materialCode);
            if (formingRemainder != null) {
                ending.setFormingRemainder(formingRemainder);
            } else if (ending.getVulcanizingRemainder() != null) {
                // 成型余量 = 硫化余量 - 胎胚库存
                ending.setFormingRemainder(ending.getVulcanizingRemainder());
            }

            // 计算收尾日
            List<FactoryMonthPlanProductionFinalResult> plans = materialPlanMap.get(materialCode);
            if (plans != null && !plans.isEmpty()) {
                int endingDay = findMaterialEndingDay(plans, currentDay, lastDayOfMonth);
                LocalDate plannedEndingDate = scheduleDate.withDayOfMonth(endingDay);
                ending.setPlannedEndingDate(plannedEndingDate);

                // 计算距收尾日的天数
                int daysToEnding = (int) java.time.temporal.ChronoUnit.DAYS.between(scheduleDate, plannedEndingDate);
                ending.setEstimatedEndingDays(BigDecimal.valueOf(daysToEnding));

                // 从参数表读取收尾天数阈值（与TaskGroupService保持一致）
                int urgentEndingDays = getEndingDaysFromParam(context, "SYS04050004", 3);
                int nearEndingDays = getEndingDaysFromParam(context, "SYS04050003", 10);

                // 设置收尾标记
                if (daysToEnding >= 0 && daysToEnding <= urgentEndingDays) {
                    ending.setIsUrgentEnding(1);
                }
                if (daysToEnding >= 0 && daysToEnding <= nearEndingDays) {
                    ending.setIsNearEnding(1);
                }
            } else {
                // 没有月计划，默认月末收尾
                ending.setPlannedEndingDate(scheduleDate.withDayOfMonth(lastDayOfMonth));
                ending.setEstimatedEndingDays(BigDecimal.valueOf(lastDayOfMonth - currentDay));
            }

            materialEndings.add(ending);
        }

        context.setMaterialEndings(materialEndings);
        log.info("计算物料收尾信息 {} 条", materialEndings.size());

        // 统计紧急收尾数量
        long urgentCount = materialEndings.stream()
                .filter(e -> e.getIsUrgentEnding() != null && e.getIsUrgentEnding() == 1)
                .count();
        if (urgentCount > 0) {
            log.warn("发现 {} 个紧急收尾物料", urgentCount);
        }
    }

    /**
     * 从参数配置中读取收尾天数阈值
     *
     * @param context      排程上下文
     * @param paramCode    参数编码（SYS04050003/SYS04050004）
     * @param defaultValue 默认值
     * @return 收尾天数阈值
     */
    private int getEndingDaysFromParam(ScheduleContextVo context, String paramCode, int defaultValue) {
        if (context.getParamConfigMap() != null) {
            CxParamConfig config = context.getParamConfigMap().get(paramCode);
            if (config != null && config.getParamValue() != null) {
                try {
                    return Integer.parseInt(config.getParamValue());
                } catch (NumberFormatException e) {
                    log.warn("参数 {} 值格式错误: {}，使用默认值 {}", paramCode, config.getParamValue(), defaultValue);
                }
            }
        }
        return defaultValue;
    }

    /**
     * 过滤已收尾物料
     *
     * <p>成型余量 <= 0 的物料表示已经收尾完成，不参与排程。
     * <p>需要过滤：
     * <ul>
     *   <li>硫化排程结果：移除已收尾物料的任务</li>
     *   <li>在机信息：移除已收尾物料的在机记录</li>
     *   <li>机台在机胎胚映射：移除已收尾物料的映射</li>
     * </ul>
     *
     * @param context 排程上下文
     */
    private void filterCompletedMaterials(ScheduleContextVo context) {
        // 获取成型余量映射
        Map<String, Integer> formingRemainderMap = context.getFormingRemainderMap();
        if (formingRemainderMap == null || formingRemainderMap.isEmpty()) {
            log.debug("成型余量映射为空，跳过过滤");
            return;
        }

        // 构建已收尾状态账户集合（成型余量 <= 0）
        Set<String> completedStatusKeys = new HashSet<>();
        for (Map.Entry<String, Integer> entry : formingRemainderMap.entrySet()) {
            if (entry.getValue() != null && entry.getValue() <= 0) {
                completedStatusKeys.add(entry.getKey());
            }
        }

        if (completedStatusKeys.isEmpty()) {
            log.debug("没有已收尾的物料，跳过过滤");
            return;
        }

        log.info("发现 {} 个已收尾物料状态账户，开始过滤", completedStatusKeys.size());

        // 1. 过滤硫化排程结果
        int originalLhCount = context.getLhScheduleResults() != null ? context.getLhScheduleResults().size() : 0;
        if (context.getLhScheduleResults() != null) {
            List<LhScheduleResult> filteredLhResults = context.getLhScheduleResults().stream()
                    .filter(r -> {
                        String materialCode = r.getMaterialCode();
                        String materialStatusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                materialCode, r.getProductStatus());
                        // 仅过滤已收尾的对应产品状态，保留同物料的其他计划类型
                        if (materialCode != null && completedStatusKeys.contains(materialStatusKey)) {
                            log.debug("过滤硫化排程结果：物料状态账户={}，成型余量={}",
                                    materialStatusKey, formingRemainderMap.get(materialStatusKey));
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            context.setLhScheduleResults(filteredLhResults);
            log.info("过滤硫化排程结果：{} -> {} 条（移除 {} 条已收尾物料任务）",
                    originalLhCount, filteredLhResults.size(), originalLhCount - filteredLhResults.size());
        }

        // 2. 过滤在机信息
        int originalOnlineCount = context.getOnlineInfos() != null ? context.getOnlineInfos().size() : 0;
        if (context.getOnlineInfos() != null) {
            // 在机信息没有产品状态，只有同一物料所有状态均已完成时才能过滤
            Collection<MdmMonthSurplus> monthSurplusValues = context.getMonthSurplusMap() != null
                    ? context.getMonthSurplusMap().values() : Collections.emptyList();
            Set<String> completedMaterialCodes = monthSurplusValues.stream()
                    .filter(surplus -> surplus.getMaterialCode() != null)
                    .collect(Collectors.groupingBy(MdmMonthSurplus::getMaterialCode))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().stream().allMatch(surplus -> {
                        String statusKey = MonthPlanSurplusCalculator.buildMaterialStatusKey(
                                surplus.getMaterialCode(), surplus.getProductStatus());
                        return completedStatusKeys.contains(statusKey);
                    }))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            // 在机信息的key格式是：物料编码|胎胚描述，需要保持一致
            Set<String> completedKeys = getCompletedKeys(context, completedMaterialCodes);

            List<CxMachineOnlineInfo> filteredOnlineInfos = context.getOnlineInfos().stream()
                    .filter(info -> {
                        // 使用物料编码 + 胎胚描述组合键（与completedKeys保持一致）
                        String materialCode = info.getMaterialCode();
                        String embryoSpec = info.getEmbryoSpec();
                        String combinedKey = materialCode + "|" + embryoSpec;
                        // 如果组合键在已收尾集合中，则过滤掉
                        if (!combinedKey.equals("|") && completedKeys.contains(combinedKey)) {
                            log.debug("过滤在机信息：机台={}，组合键={}，对应物料已收尾",
                                    info.getCxCode(), combinedKey);
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            context.setOnlineInfos(filteredOnlineInfos);
            log.info("过滤在机信息：{} -> {} 条（移除 {} 条已收尾物料在机记录）",
                    originalOnlineCount, filteredOnlineInfos.size(), originalOnlineCount - filteredOnlineInfos.size());
        }

        // 3. 重新构建机台在机胎胚映射（使用过滤后的在机信息）
        // machineOnlineEmbryoMap 存储格式：embryoCode → Set(cxCode)
        if (context.getOnlineInfos() != null) {
            Map<String, Set<String>> machineOnlineEmbryoMap = new HashMap<>();
            for (CxMachineOnlineInfo onlineInfo : context.getOnlineInfos()) {
                String cxCode = onlineInfo.getCxCode();
                //KF_NICK 这里onlineInfo.getMaterialCode() 是胎胚号
                String embryoCode = onlineInfo.getMaterialCode();
                if (cxCode != null && embryoCode != null && !embryoCode.isEmpty()) {
                    machineOnlineEmbryoMap.computeIfAbsent(embryoCode, k -> new HashSet<>()).add(cxCode);
                }
            }
            context.setMachineOnlineEmbryoMap(machineOnlineEmbryoMap);
            log.info("重新构建机台在机胎胚映射，共 {} 个胎胚有在机任务", machineOnlineEmbryoMap.size());
        }

        // 4. 记录被过滤的物料信息
        StringBuilder sb = new StringBuilder("已收尾物料列表：\n");
        for (String materialStatusKey : completedStatusKeys) {
            Integer remainder = formingRemainderMap.get(materialStatusKey);
            sb.append(String.format("  - 物料状态账户: %s, 成型余量: %d\n", materialStatusKey, remainder));
        }
        log.info(sb.toString());
    }

    private static Set<String> getCompletedKeys(ScheduleContextVo context, Set<String> completedMaterialCodes) {
        Map<String, String> materialToEmbryoDescMap = new HashMap<>();
        if (context.getMaterials() != null) {
            for (MdmMaterialInfo material : context.getMaterials()) {
                if (material.getMaterialCode() != null && material.getEmbryoDesc() != null) {
                    materialToEmbryoDescMap.put(material.getMaterialCode(), material.getEmbryoDesc());
                }
            }
        }

        Set<String> completedKeys = new HashSet<>();
        for (String materialCode : completedMaterialCodes) {
            String embryoDesc = materialToEmbryoDescMap.get(materialCode);
            if (materialCode != null && embryoDesc != null) {
                completedKeys.add(materialCode + "|" + embryoDesc);
            }
        }
        return completedKeys;
    }

    // ==================== 私有方法：收尾计算 ====================

    /**
     * 找到该物料的最近一个收尾日
     *
     * <p>使用月计划的 END_DAY 确定收尾日，避免中间断产日（如 D20=NULL）导致提前截断。
     * 多条计划时取最大的 END_DAY。
     *
     * @param plans         月计划列表
     * @param currentDay    当前日期（几号）
     * @param lastDayOfMonth 月末日期
     * @return 最近一个收尾日
     */
    private int findMaterialEndingDay(List<FactoryMonthPlanProductionFinalResult> plans, int currentDay, int lastDayOfMonth) {
        int endingDay = currentDay;
        for (FactoryMonthPlanProductionFinalResult plan : plans) {
            Integer planEndDay = plan.getEndDay();
            if (planEndDay != null && planEndDay > endingDay) {
                endingDay = planEndDay;
            }
        }
        // 扫描DAY_X字段，找到最后一天有实际排产数据的日期（兜底：END_DAY可能不准确）
        for (int day = lastDayOfMonth; day > endingDay; day--) {
            for (FactoryMonthPlanProductionFinalResult plan : plans) {
                Integer dayQty = plan.getDayQty(day);
                if (dayQty != null && dayQty > 0) {
                    endingDay = day;
                    break;
                }
            }
            if (endingDay == day) {
                break;
            }
        }
        return endingDay > currentDay ? endingDay : lastDayOfMonth;
    }

    /**
     * 从 T_LH_PARAMS 按工厂+参数编码读取硫化源头参数值。
     */
    private String loadLhParamValue(LhParamsMapper mapper, String factoryCode, String paramCode) {
        try {
            LhParams param = mapper.selectOne(
                    new LambdaQueryWrapper<LhParams>()
                            .eq(LhParams::getFactoryCode, factoryCode)
                            .eq(LhParams::getParamCode, paramCode)
                            .eq(LhParams::getIsDelete, "0")
                            .last("LIMIT 1"));
            if (param != null && param.getParamValue() != null && !param.getParamValue().trim().isEmpty()) {
                return param.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("从T_LH_PARAMS加载参数失败: factoryCode={}, paramCode={}, error={}", factoryCode, paramCode, e.getMessage());
        }
        return null;
    }

    private Map<String, Integer> loadMachineMaxEmbryoTypes(ScheduleContextVo context, Map<String, CxParamConfig> paramConfigMap) {
        CxParamConfig newConfig = paramConfigMap.get(PARAM_CODE_MACHINE_MAX_EMBRYO_TYPES);
        if (newConfig != null && newConfig.getParamValue() != null && newConfig.getParamValue().contains(",")) {
            Map<String, Integer> result = productionCalculator.parseMachineMaxTypes(newConfig.getParamValue());
            if (!result.isEmpty()) {
                log.info("机台最大胎胚种类数（新格式 SYS04040001）: {}", result);
                return result;
            }
        }

        CxParamConfig valueConfig = paramConfigMap.get("SYS04040002");
        CxParamConfig prefixConfig = paramConfigMap.get("SYS04040003");
        if (valueConfig != null && valueConfig.getParamValue() != null) {
            try {
                int value = Integer.parseInt(valueConfig.getParamValue());
                String prefix = prefixConfig != null && prefixConfig.getParamValue() != null
                        ? prefixConfig.getParamValue().trim() : DEFAULT_MAX_EMBRYO_PREFIX;
                Map<String, Integer> result = new LinkedHashMap<>();
                result.put(prefix, value);
                log.info("机台最大胎胚种类数（旧格式兼容 SYS04040002+40003）: {}={}", prefix, value);
                return result;
            } catch (NumberFormatException e) {
                log.warn("解析旧参数 SYS04040002 失败: {}", valueConfig.getParamValue());
            }
        }

        if (newConfig != null && newConfig.getParamValue() != null) {
            try {
                int value = Integer.parseInt(newConfig.getParamValue());
                Map<String, Integer> result = new LinkedHashMap<>();
                result.put(DEFAULT_MAX_EMBRYO_PREFIX, value);
                log.info("机台最大胎胚种类数（旧单值格式 SYS04040001）: {}={}", DEFAULT_MAX_EMBRYO_PREFIX, value);
                return result;
            } catch (NumberFormatException ignored) {}
        }

        Map<String, Integer> result = new LinkedHashMap<>();
        result.put(DEFAULT_MAX_EMBRYO_PREFIX, DEFAULT_MAX_EMBRYO_TYPES);
        log.info("机台最大胎胚种类数（默认）: {}={}", DEFAULT_MAX_EMBRYO_PREFIX, DEFAULT_MAX_EMBRYO_TYPES);
        return result;
    }

    private String loadFactoryParamValue(FactoryParamMapper mapper, String factoryCode, String paramCode) {
        try {
            LambdaQueryWrapper<FactoryParam> wrapper = new LambdaQueryWrapper<FactoryParam>()
                    .eq(FactoryParam::getFactoryCode, factoryCode)
                    .eq(FactoryParam::getParamCode, paramCode)
                    .eq(FactoryParam::getIsDelete, "0");
            wrapper.eq(FactoryParam::getProductTypeCode, "TBR");
            FactoryParam param = mapper.selectOne(wrapper);
            if (param != null && param.getParamValue() != null && !param.getParamValue().trim().isEmpty()) {
                return param.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("从T_MP_FACTORY_PARAM加载参数失败: factoryCode={}, paramCode={}, error={}", factoryCode, paramCode, e.getMessage());
        }
        return null;
    }

    // ==================== 属性注入 ====================
}
