package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhUnscheduledResult;
import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.component.MonthPlanDateResolver;
import com.zlt.aps.lh.component.NewSpecDelayDaysResolver;
import com.zlt.aps.lh.component.StructureEndingAlignmentService;
import com.zlt.aps.lh.component.TargetScheduleQtyResolver;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.engine.strategy.IFirstInspectionBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IHistoricalMouldChangeReverseSelectionStrategy;
import com.zlt.aps.lh.engine.strategy.IMachineMatchStrategy;
import com.zlt.aps.lh.engine.strategy.IMouldChangeBalanceStrategy;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.engine.strategy.support.PendingSkuUnscheduledRule;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * S4.5 新增规格排产处理器。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>处理 S4.4 未消费的新增 SKU，按排序结果逐个尝试上机；</li>
     *   <li>串联 SKU 排序、机台匹配、基础换模时间分配、可选换模均衡、首检均衡、开产时间计算和新增策略落地；</li>
 *   <li>新增排产会同时写入排程结果、机台运行态、未排原因、日计划账本和同胎胚换模占用。</li>
 * </ul>
 *
 * <p>注意：试制、量试、小批量、正规 SKU 的差异主要在排序 tie-break、单控机台约束、
 * 严格目标量和班次补满策略中体现，不应在本 Handler 中新增并行业务分支；
 * 唯一例外是硫化参数 SYS0311004=0 时在入口统一拦截试制、量试 SKU（续作排产不受该参数影响），
 * 该拦截只做准入排除和未排落库，不参与选机和数量分配。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class NewProductionHandler extends AbsScheduleStepHandler {

    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private IHistoricalMouldChangeReverseSelectionStrategy historicalReverseSelectionStrategy;
    @Resource
    private StructureEndingAlignmentService structureEndingAlignmentService;
    @Resource
    private NewSpecDelayDaysResolver newSpecDelayDaysResolver;
    @Resource
    private TargetScheduleQtyResolver targetScheduleQtyResolver;

    @Override
    protected void doHandle(LhScheduleContext context) {
        log.info("新增规格排产处理开始, 工厂: {}, 目标日: {}, 待排新增SKU: {}, 当前结果数: {}, 未排产数: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                context.getNewSpecSkuList().size(), context.getScheduleResultList().size(),
                context.getUnscheduledResultList().size());

        /*
         * SYS0311004=0 时试制、量试不参与新增排产（续作排产不受该参数影响）。
         * 必须在排序、前日交替反选和普通新增选机之前统一拦截：同物料多状态续作切换在S4.4
         * 已按续作口径消费可承接的X/T候选，此处只拦截仍残留在新增待排列表中的试制、量试SKU，
         * 写未排记录并清理运行态，避免其继续进入任何新增选机环节。
         */
        this.excludeTrialMassTrialNewSpecSkus(context);

        /*
         * S4.5 开始前冻结“排程开始时已在机且仍由原物料续作”的结果身份。
         * 共用模具联动置换和特殊材料置换都发生在 S4.5 完成后。若届时仅按机台或 scheduleType
         * 反推，会把本阶段刚生成的新增结果误当成续作候选；因此必须在新增链路写入任何结果之前
         * 先保留这份只读快照。
         */
        captureSubstitutionContinuationSnapshot(context);

        try {
            // 获取 S4.5 新增排产策略；特殊材料置换由本 Handler 完成后的独立 S4.5.1 步骤执行。
            IProductionStrategy strategy = strategyFactory.getProductionStrategy(
                    ScheduleTypeEnum.NEW_SPEC.getCode());

            /*
             * S4.5.2 排序字段准备：必须在 S4.4 全部完成后、最终新增 SKU 排序前统一重算延误天数。
             * 此时续作因 dayN 加机台进入新增排产的补偿 SKU 已生成，首次增机日期也已固化；普通新增
             * SKU 则只读取 S4.2 已加载月计划和完成量。解析器仅改写 delayDays，不改动待排量、日计划
             * 账本、SKU分组、比较器层级和排序方向，确保影响范围严格限制在新增排产排序字段。
             */
            newSpecDelayDaysResolver.refreshDelayDays(context);

            /*
             * S4.5.2 SKU优先级排序：S4.4已消费的SKU会同步移出structureSkuMap，本次排序继续
             * 对当前仍参与新增排产的结构按最大END_DAY计算一次距离，并统一标记同结构全部候选。
             * 排序标记只读取独立的structurePriorityMaxEndingDateMap，不读取也不修改后续结构收尾
             * 对齐使用的固定三天快照，确保本次变更只影响SKU排序标记。
             */
            ISkuPriorityStrategy priorityStrategy = strategyFactory.getSkuPriorityStrategy();
            priorityStrategy.sortByPriority(context);
            log.debug("新增规格SKU优先级排序完成, 待排新增SKU: {}", context.getNewSpecSkuList().size());

            /*
             * S4.5.3 前日交替计划机台反选：
             * 必须在新增SKU业务优先级排序完成后、普通新增选机前执行。反选策略会按历史班次4、5的
             * “机台+后物料”关系登记指定机台指令；机台驱动主链先在固定组合独立作用域执行
             * 完整可排校验，失败后再进入普通动态竞争。指令不得改写上一步形成的 SKU 业务排序字段。
             */
            historicalReverseSelectionStrategy.reverseSelect(context);
            log.info("前日交替计划机台反选完成, 排程结果数: {}, 待新增SKU: {}, 指令数: {}",
                    context.getScheduleResultList().size(), context.getNewSpecSkuList().size(),
                    context.getHistoricalReverseSelectionDirectiveList().size());

            /*
             * S4.5.3.1 结构收尾对齐在机统计缓存构建：
             * 结构转产表最大收尾日期已在S4.2月计划完成后的依赖任务中加载，本调用不再查询数据库。
             * 基于续作+换活字块排产完成后的实时排程结果构建【结构×班次】在机统计，
             * 后续每次新增选机先用S4.2日期快照校验[T,T+2]门禁；通过后才读取该动态缓存执行
             * 原结构对齐规则，并按结果提交增量更新，避免反复全表扫描且保证前后数据时序正确。
             */
            structureEndingAlignmentService.prepareStructureEndingAlignmentIndex(context);

            // S4.5.4 遍历新增SKU, 匹配机台
            IMachineMatchStrategy machineMatchStrategy = strategyFactory.getMachineMatchStrategy();

            // S4.5.5 换模时间分配（开关开启时附带换模均衡）
            IMouldChangeBalanceStrategy mouldChangeStrategy = strategyFactory.getMouldChangeBalanceStrategy();

            // S4.5.6 首检均衡分配
            IFirstInspectionBalanceStrategy inspectionStrategy = strategyFactory.getFirstInspectionBalanceStrategy();

            // S4.5.7 计算开产时间
            ICapacityCalculateStrategy capacityStrategy = strategyFactory.getCapacityCalculateStrategy();

            /*
             * S4.5.8 新增排产按业务日编排：
             * 1. 保持上方已生成的SKU业务顺序和历史反选指令，不在日循环内重算业务排序权重；
             * 2. 将class1～class8按workDate拆为T日2班、T+1/T+2各3班；
             * 3. 每日依次执行“在机延续、当天计划/锁定、加机台、提前生产、日终结转”；
             * 4. 每台机台按原业务顺序扫描候选，匹配等级优先、同等级以业务顺序兜底；
             * 5. 单个胜出候选仍复用现有选机、换模、首检、目标量、单控和排满内核；
             * 6. 临时资源失败只登记延期，T+2 的提前生产阶段完成后再统一写最终未排；
             * 7. 三天全部完成后，才执行下方窗口级胎胚调整和后续特殊材料置换。
             *
             * 该调用仍使用IProductionStrategy原接口，数据库字段、Mapper和保存链路均不改变。
             */
            strategy.scheduleNewSpecs(context, machineMatchStrategy,
                    mouldChangeStrategy, inspectionStrategy, capacityStrategy);
            log.info("新增规格选机排产完成, 排程结果数: {}, 剩余新增SKU: {}, 未排产数: {}",
                    context.getScheduleResultList().size(), context.getNewSpecSkuList().size(),
                    context.getUnscheduledResultList().size());
            strategy.allocateShiftPlanQty(context);
            strategy.adjustEmbryoStock(context);
            log.info("新增规格胎胚库存调整完成, 排程结果数: {}, 剩余新增SKU: {}, 未排产数: {}",
                    context.getScheduleResultList().size(), context.getNewSpecSkuList().size(),
                    context.getUnscheduledResultList().size());
            strategy.scheduleReduceMould(context);
            log.info("新增规格排产处理完成, 排程结果数: {}, 未排产数: {}",
                    context.getScheduleResultList().size(), context.getUnscheduledResultList().size());
        } finally {
            /*
             * 提前生产中心视图不仅服务 scheduleNewSpecs，还要支撑后续班次分配、胎胚回裁和
             * isEnd 最终复核。必须等整个 S4.5 完成后再清理；异常退出时同样清理，避免临时
             * 前移日计划或中心目标泄漏到后续特殊材料置换及其他排程步骤。
             */
            context.clearEarlyProductionRuntimePlans();
        }
    }

    /**
     * SYS0311004=0时拦截新增排产链路中的试制、量试SKU。
     *
     * <p>硫化参数SYS0311004只控制新增排产：参数=0时施工阶段为试制（01）、量试（02）的SKU
     * 不得通过新增排产上机；续作排产、同物料多状态续作切换和续作加机台补偿（续作衍生）
     * 均不受该参数影响。本方法在S4.5入口统一执行拦截：</p>
     * <ul>
     *   <li>命中SKU写未排记录（按物料+产品状态去重替换，未排数量为0）；</li>
     *   <li>目标量清零、移出结构待排池、全量SKU复合索引和活跃胎胚清单，语义与S4.3准入拦截清理一致；</li>
     *   <li>按对象身份从新增待排列表移除，避免DTO值相等误删同键其它副本。</li>
     * </ul>
     *
     * @param context 排程上下文
     */
    private void excludeTrialMassTrialNewSpecSkus(LhScheduleContext context) {
        if (PendingSkuUnscheduledRule.isTrialMassTrialSchedulingEnabled(context)
                || CollectionUtils.isEmpty(context.getNewSpecSkuList())) {
            return;
        }
        List<SkuScheduleDTO> blockedSkuList = new ArrayList<>(context.getNewSpecSkuList().size());
        List<LhUnscheduledResult> blockedResultList =
                new ArrayList<>(context.getNewSpecSkuList().size());
        for (SkuScheduleDTO sku : context.getNewSpecSkuList()) {
            LhUnscheduledResult unscheduledResult =
                    PendingSkuUnscheduledRule.evaluateNewSpecTrialExclusion(context, sku);
            if (Objects.isNull(unscheduledResult)) {
                continue;
            }
            blockedSkuList.add(sku);
            blockedResultList.add(unscheduledResult);
        }
        if (CollectionUtils.isEmpty(blockedSkuList)) {
            return;
        }
        // 按对象身份移除，与同物料多状态切换链的消费口径保持一致。
        Set<SkuScheduleDTO> blockedSkuIdentitySet = Collections.newSetFromMap(
                new IdentityHashMap<SkuScheduleDTO, Boolean>(blockedSkuList.size() * 2));
        blockedSkuIdentitySet.addAll(blockedSkuList);
        context.getNewSpecSkuList().removeIf(blockedSkuIdentitySet::contains);
        for (int index = 0; index < blockedSkuList.size(); index++) {
            this.appendOrReplaceUnscheduledResult(context, blockedResultList.get(index));
            this.cleanupExcludedTrialSku(context, blockedSkuList.get(index));
        }
        log.info("新增排产试制量试参数拦截完成, factoryCode: {}, batchNo: {}, blockedCount: {}, "
                        + "remainingNewSpecCount: {}, reason: {}",
                context.getFactoryCode(), context.getBatchNo(), blockedSkuList.size(),
                context.getNewSpecSkuList().size(),
                PendingSkuUnscheduledRule.NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON);
    }

    /**
     * 清理被参数拦截的试制、量试SKU运行态。
     *
     * <p>清理语义与S4.3新增准入拦截（cleanupBlockedSku）保持一致：目标量清零、
     * 移出结构待排池、全量SKU复合索引和活跃胎胚清单，保证后续换活字块、
     * 特殊材料置换等阶段不再找回该SKU。</p>
     *
     * @param context 排程上下文
     * @param sku 被拦截的试制、量试SKU
     */
    private void cleanupExcludedTrialSku(LhScheduleContext context, SkuScheduleDTO sku) {
        sku.setTargetScheduleQty(0);
        sku.setRemainingScheduleQty(0);
        context.removePendingSkuFromStructureMap(sku);
        context.getAllSkuScheduleDtoMap().remove(MonthPlanDateResolver.buildMaterialStatusKey(
                sku.getMaterialCode(), sku.getProductStatus()));
        targetScheduleQtyResolver.removeActiveEmbryoSku(context, sku,
                PendingSkuUnscheduledRule.NEW_SPEC_TRIAL_EXCLUSION_UNSCHEDULED_REASON);
    }

    /**
     * 按物料和产品状态写入或替换未排结果。
     *
     * <p>同一“物料+产品状态”可能因多副本SKU重复命中参数拦截，或S4.4同物料切换失败链
     * 已写入同键未排；本方法保持列表位置并替换为本次参数拦截结果，避免同一SKU重复落库。</p>
     *
     * @param context 排程上下文
     * @param unscheduledResult 参数拦截生成的未排结果
     */
    private void appendOrReplaceUnscheduledResult(LhScheduleContext context,
                                                  LhUnscheduledResult unscheduledResult) {
        if (Objects.isNull(context) || Objects.isNull(unscheduledResult)) {
            return;
        }
        for (int index = 0; index < context.getUnscheduledResultList().size(); index++) {
            LhUnscheduledResult existing = context.getUnscheduledResultList().get(index);
            if (Objects.nonNull(existing)
                    && StringUtils.equals(existing.getMaterialCode(), unscheduledResult.getMaterialCode())
                    && StringUtils.equals(StringUtils.trimToEmpty(existing.getProductStatus()),
                    StringUtils.trimToEmpty(unscheduledResult.getProductStatus()))) {
                context.getUnscheduledResultList().set(index, unscheduledResult);
                return;
            }
        }
        context.getUnscheduledResultList().add(unscheduledResult);
    }

    /**
     * 冻结全部后置置换允许使用的续作在机结果。
     *
     * <p>同时满足以下条件才进入快照：结果属于续作、机台存在初始在机快照、结果物料与初始在机物料一致。
     * 换活字块结果以及 S4.5 后续新增结果不会进入该集合，从数据来源上保证置换不影响新增排产。</p>
     *
     * @param context 排程上下文
     */
    private void captureSubstitutionContinuationSnapshot(LhScheduleContext context) {
        context.getSpecialMaterialContinuationResultSnapshot().clear();
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result)
                    || !ScheduleTypeEnum.CONTINUOUS.getCode().equals(result.getScheduleType())
                    || StringUtils.isEmpty(result.getLhMachineCode())
                    || StringUtils.isEmpty(result.getMaterialCode())) {
                continue;
            }
            MachineScheduleDTO initialMachine =
                    context.getInitialMachineScheduleMap().get(result.getLhMachineCode());
            if (Objects.isNull(initialMachine)
                    || !StringUtils.equals(initialMachine.getCurrentMaterialCode(), result.getMaterialCode())) {
                continue;
            }
            context.getSpecialMaterialContinuationResultSnapshot().add(result);
        }
        log.info("后置置换续作在机快照完成, 工厂: {}, 批次: {}, 续作结果数: {}",
                context.getFactoryCode(), context.getBatchNo(),
                context.getSpecialMaterialContinuationResultSnapshot().size());
    }

    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_5_NEW_PRODUCTION.getDescription();
    }
}
