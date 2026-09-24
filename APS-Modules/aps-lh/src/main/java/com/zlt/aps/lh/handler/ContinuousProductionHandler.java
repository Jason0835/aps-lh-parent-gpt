package com.zlt.aps.lh.handler;

import com.zlt.aps.lh.api.enums.ScheduleStepEnum;
import com.zlt.aps.lh.api.enums.ScheduleTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.chain.validators.ContinuationOnlineMouldValidator;
import com.zlt.aps.lh.engine.factory.ScheduleStrategyFactory;
import com.zlt.aps.lh.engine.strategy.IProductionStrategy;
import com.zlt.aps.lh.engine.strategy.ISkuPriorityStrategy;
import com.zlt.aps.lh.engine.strategy.ITypeBlockProductionStrategy;
import com.zlt.aps.lh.service.impl.LhMaintenanceScheduleService;
import com.zlt.aps.lh.service.impl.PreviousAlternatePlanReuseService;
import com.zlt.aps.lh.service.impl.TimedMachineOffShiftService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * S4.4 续作规格排产处理器。
 *
 * <p>业务定位：</p>
 * <ul>
 *   <li>承接 S4.3 分类出的续作 SKU，优先处理 MES 在机规格；</li>
 *   <li>组织续作收尾、换活字块衔接、班次计划量分配、胎胚库存调整和降模排产；</li>
 *   <li>执行顺序早于 S4.5 新增排产，避免新增规格抢占本应续作收尾的机台窗口。</li>
 * </ul>
 *
 * <p>该 Handler 只负责步骤编排，具体续作目标量、换活字块匹配、同 SKU 多机台降模等规则
 * 分别下沉到 {@code ContinuousProductionStrategy} 和 {@code TypeBlockProductionStrategy}。</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class ContinuousProductionHandler extends AbsScheduleStepHandler {

    @Resource
    private ScheduleStrategyFactory strategyFactory;
    @Resource
    private ITypeBlockProductionStrategy typeBlockProductionStrategy;
    @Resource
    private LhMaintenanceScheduleService maintenanceScheduleService;
    /** 续作真实释放时间确定后，先消费可复用交替关系的一份机台需求。 */
    @Resource
    private PreviousAlternatePlanReuseService previousAlternatePlanReuseService;
    /** 余量收尾之后独立确定按时间下机边界，必须早于正式扣账和后料提交。 */
    @Resource
    private TimedMachineOffShiftService timedMachineOffShiftService;
    /** S4.4 续作排产前校验 MES 实际在机模具与续作 SKU 模具关系。 */
    @Resource
    private ContinuationOnlineMouldValidator continuationOnlineMouldValidator;

    @Override
    protected void doHandle(LhScheduleContext context) {
        log.info("续作排产处理开始, 工厂: {}, 目标日: {}, 续作SKU: {}, 新增SKU: {}, 当前结果数: {}",
                context.getFactoryCode(), LhScheduleTimeUtil.formatDate(context.getScheduleTargetDate()),
                context.getContinuousSkuList().size(), context.getNewSpecSkuList().size(),
                context.getScheduleResultList().size());
        // S4.4正式排产前校验真实在机模具，失败时不进入续作、换活字块及新增排产链路。
        continuationOnlineMouldValidator.validate(context);
        // 冻结历史交替时刻，续作识别保持原样，排量及所有后置调整共同遵守下机边界。
        previousAlternatePlanReuseService.prepareReleaseEvents(context);
        ISkuPriorityStrategy priorityStrategy = strategyFactory.getSkuPriorityStrategy();
        /*
         * S4.4排序调用：排序策略从S4.2独立的结构排序日期快照读取最大END_DAY，按结构只计算一次
         * 与T日的包含首尾距离；严格小于SYS0304002时统一标记当前同结构候选SKU。这里只消费
         * 排序结果，不查询结构转产表，也不调用或改变S4.5结构收尾对齐选机规则。
         */
        priorityStrategy.sortByPriority(context);
        log.debug("续作排产优先级排序完成, 续作SKU: {}, 新增SKU: {}",
                context.getContinuousSkuList().size(), context.getNewSpecSkuList().size());

        /*
         * S4.4开始前只登记中心决策，禁止逐SKU回调提前挂载精度窗口。
         * 精度统一等待续作最终收口，不能将初始化首班起点误当成物料已收尾。
         */
        maintenanceScheduleService.prepareMaintenancePlanWindows(context);

        IProductionStrategy strategy = strategyFactory.getProductionStrategy(
                ScheduleTypeEnum.CONTINUOUS.getCode());

        // S4.4.1 MES在机原物料延续生产与续作收尾：先处理原机台可持续生产的规格。
        strategy.scheduleContinuousEnding(context);
        log.info("续作收尾排产完成, 排程结果数: {}, 待新增SKU: {}",
                context.getScheduleResultList().size(), context.getNewSpecSkuList().size());

        // S4.4.2 班次计划量分配：续作策略中部分结果已携带班次量，此处保留统一策略入口。
        strategy.allocateShiftPlanQty(context);
        log.debug("续作班次计划量分配完成, 排程结果数: {}", context.getScheduleResultList().size());

        // S4.4.3 胎胚库存调整：按 SKU 维度库存裁剪，避免同胎胚多个 SKU 共享库存导致超排。
        strategy.adjustEmbryoStock(context);
        log.info("续作胎胚库存调整完成, 排程结果数: {}, 未排产数: {}",
                context.getScheduleResultList().size(), context.getUnscheduledResultList().size());

        // 前置续作降模只冻结余量收尾机台及硬边界，普通非收尾处理保持原有口径。
        strategy.scheduleReduceMould(context);
        // 历史交替正式排产前计算真实收尾，禁止后料提交后再搬动前料。
        strategy.calculateContinuationRemainderFinish(context);
        // 不依赖余量收尾组是否存在，独立处理已选机台的时间下机需求。
        timedMachineOffShiftService.resolveAndApply(context);
        // 数量稳定后只消费一次账本，同时发布机台、模具和待排余量。
        strategy.finalizeContinuousProduction(context);

        // 续作最终数量、真实收尾和物理交接时间已经稳定，按精度优先级统一分配每日一台额度。
        maintenanceScheduleService.finalizeMaintenancePlanWindows(context);

        // 独立复用阶段覆盖全部剩余产能机台，完成后换活字块只处理实时剩余资源与需求。
        previousAlternatePlanReuseService.reuse(context);

        // S4.4.5 收尾后换活字块衔接排产：只读取均衡后的最终机台可用时间。
        typeBlockProductionStrategy.scheduleTypeBlockChange(context);
        // 换活字块可能移出或回写待新增SKU，需重新构建结构视图供 S4.5 新增排序使用。
        context.rebuildStructureSkuMapFromPending(context.getNewSpecSkuList());
        log.info("换活字块衔接排产完成, 排程结果数: {}, 待新增SKU: {}",
                context.getScheduleResultList().size(), context.getNewSpecSkuList().size());

        // S4.4.6 续作后全量启用机台排序日志：排序逻辑不变，只展示均衡和换活字块后的真实机台状态。
        strategyFactory.getMachineMatchStrategy().traceEnabledMachineSort(context);
    }

    @Override
    protected String getStepName() {
        return ScheduleStepEnum.S4_4_CONTINUOUS_PRODUCTION.getDescription();
    }
}
