package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;

/**
 * 前日交替来源及班次边界快照，实际释放随最终续作收口同步，完成状态由结果关联维护。
 *
 * @author APS
 * @date 2026/10/15
 */
@Data
public class PreviousAlternatePlanReleaseEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 历史动作前的生产段身份，由冻结的关系与实际在机信息共同决定。
     *
     * @author APS
     * @date 2026/10/16
     */
    public enum ProductionRole {
        /**
         * 本机承载实际前料，包括与历史前料不同的在机物料，沿用余量收尾或时间下机。
         */
        BEFORE_MATERIAL_PRODUCTION,
        /**
         * MES已显示历史后料，必须先完成历史动作及首检才能生产。
         */
        AFTER_MATERIAL_PREPARATION,
        /**
         * 历史本来就是同料动作，保留既有同料收尾和动作边界。
         */
        SAME_MATERIAL_ACTION,
        /**
         * 冻结关系与实际在机信息不能确定上述身份。
         */
        UNMATCHED
    }

    /** 历史L/R动作一次仅更换一副模具。 */
    public static final int SINGLE_REPLACEMENT_MOULD_QTY = 1;
    /** 原始交替计划，保留计划编号、前后物料及动作类型。 */
    private LhMouldChangePlan plan;
    /**
     * 本次实际动作只读快照，独立于历史计划类型及其排序，候选回滚不改变原装机事实。
     */
    private PreviousAlternateActionPlan actualActionPlan;
    /** 当前运行态机台编码，左右侧保持独立。 */
    private String machineCode;
    /** 下机前实际在机物料，不能用历史后料替代。 */
    private String materialCode;
    /** 下机前实际产品状态。 */
    private String productStatus;
    /**
     * 本机历史前料最终未排产，默认从T日历史班次尝试，较早早班仅在完整承接成功后生效。
     */
    private boolean beforeMaterialNotScheduled;
    /** 历史计划日期及班次解析出的默认下机起点；异料严格收尾可按合法班次内真实释放更新。 */
    private Date plannedShiftStartTime;
    /**
     * 原历史计划班次结束，用于历史排序及前料原下机边界，不再限制指定后料顺延。
     */
    private Date plannedShiftEndTime;
    /**
     * 已准入历史指定后料在本次固定排程窗口内的承接上限，不改变原历史班次和前料释放事实。
     */
    private Date handoffWindowEndTime;
    /** 本轮准备资源窗口截止；未来生产提案不能借此抢占尚未轮到的准备班次。 */
    private Date preparationWindowEndTime;

    /**
     * 无既有下机安排的普通续作等待补充历史强制释放，正式时间决策前不执行或预演该动作。
     */
    private boolean supplementalReleasePending;

    /**
     * 正式时间下机发布后指定后料的承接窗口上限，不用于时间下机预演或余量收尾计算。
     */
    private Date confirmedTimedHandoffWindowEndTime;

    /**
     * 余量组在原机分配的窗口上限，非空表示历史交替消费本组提交的真实释放。
     * 精确异料固定交替仍按原指定起点限制前料，其余历史关系等待原节点分配。
     */
    private Date remainderFinishWindowEndTime;

    /**
     * 普通余量组已确认精确异料历史交替，前料按原指定起点封口，后料不得因余量分配提前准备。
     * 由续作分组统一冻结，预演通过事件快照同步恢复；不适用于胎胚库存组或同料动作。
     */
    private boolean fixedHistoricalCutover;

    /**
     * 胎胚硬目标已完整完成后的承接窗口上限；仅延后准备截止，不改变整组余量释放流程。
     */
    private Date completedEmbryoEndingWindowEndTime;
    /**
     * 共用胎胚分摊额度尚未完成，本机及模具继续由前料占用，历史后料不得提前承接。
     */
    private boolean sharedEmbryoEndingPending;

    /**
     * 同一严格收尾账本已完整完成后，本机确认的物理释放时刻。
     * 精确历史后料可据此在首检后连续生产，需求名额仍由原需求业务日校验。
     */
    private Date completedStrictEndingReleaseTime;

    /**
     * 指定试制量试在本窗口内继续承接的上限，仅局部提案使用，不改变原续作释放事件。
     */
    private Date trialHandoffWindowEndTime;

    /**
     * 附加交接服务本次验证的准备班次截止，原历史事件默认不启用。
     */
    private Date extensionPreparationEndTime;

    /**
     * 完整后料提案已通过的强制交接时刻，防止旧余量快照恢复自然收尾时间。
     */
    private Date forcedHandoffReleaseTime;

    /** @return 是否由终末余量整组快照确定本次交替释放及准备边界 */
    public boolean isRemainderFinishDeferred() {
        return Objects.nonNull(remainderFinishWindowEndTime);
    }

    /**
     * 解析本次窗口承接上限或独立扩展确认的单班截止，并限制在当前准备业务日内。
     *
     * @return 准备截止时刻，采用半开区间
     */
    public Date getPreparationDeadline() {
        if (Objects.nonNull(extensionPreparationEndTime)) {
            return Objects.nonNull(preparationWindowEndTime)
                    && preparationWindowEndTime.before(extensionPreparationEndTime)
                    ? preparationWindowEndTime : extensionPreparationEndTime;
        }
        Date plannedEnd = this.isRemainderFinishDeferred() ? remainderFinishWindowEndTime
                : Objects.requireNonNull(plannedShiftEndTime, "历史计划班次截止未冻结");
        if (Objects.nonNull(confirmedTimedHandoffWindowEndTime)) {
            plannedEnd = confirmedTimedHandoffWindowEndTime;
        }
        if (Objects.nonNull(completedEmbryoEndingWindowEndTime)) {
            plannedEnd = completedEmbryoEndingWindowEndTime;
        }
        if (Objects.nonNull(trialHandoffWindowEndTime)) {
            plannedEnd = trialHandoffWindowEndTime;
        }
        if (Objects.nonNull(handoffWindowEndTime)) {
            plannedEnd = handoffWindowEndTime;
        }
        return Objects.nonNull(preparationWindowEndTime) && preparationWindowEndTime.before(plannedEnd)
                ? preparationWindowEndTime : plannedEnd;
    }

    /**
     * 前料最终未排时默认使用T日历史班次起点，完整提前承接成功后同步为已确认的真实释放。
     */
    private Date noProductionStartTime;
    /** 登记时为本次动作边界；异料严格收尾可更新为历史指定班次结束前的真实物理释放时刻。 */
    private Date offlineTime;
    /** 下机前整组实际模具，供单副置换及整组替换判定。 */
    private Set<String> originalMouldCodes = new LinkedHashSet<String>(2);
    /** 单副置换中仍留在原机台的模具；登记事件时冻结，预演和回滚不得释放。 */
    private Set<String> retainedMouldCodes = new LinkedHashSet<String>(2);

    /**
     * 从冻结字段解析动作前身份，避免将MES后料误认作需要先完成的历史前料。
     * 角色不另存可变标记，关联尝试与时间下机预演恢复原事件后同步恢复身份。
     *
     * @return 真实前料、待准备后料、历史同料动作或不匹配身份
     */
    public ProductionRole resolveProductionRole() {
        if (Objects.isNull(plan) || StringUtils.isBlank(materialCode)) {
            return ProductionRole.UNMATCHED;
        }
        if (StringUtils.equals(materialCode, plan.getBeforeMaterialCode())) {
            return StringUtils.equals(plan.getBeforeMaterialCode(), plan.getAfterMaterialCode())
                    ? ProductionRole.SAME_MATERIAL_ACTION : ProductionRole.BEFORE_MATERIAL_PRODUCTION;
        }
        if (beforeMaterialNotScheduled && StringUtils.isNotBlank(plan.getProductStatus())
                && StringUtils.equals(materialCode, plan.getAfterMaterialCode())
                && StringUtils.equals(productStatus, plan.getProductStatus())) {
            return ProductionRole.AFTER_MATERIAL_PREPARATION;
        }
        // 实际前料变化不撤销指定后料，前料数量和释放必须继续按真实在机身份处理。
        if (!StringUtils.equals(materialCode, plan.getAfterMaterialCode())) {
            return ProductionRole.BEFORE_MATERIAL_PRODUCTION;
        }
        return ProductionRole.UNMATCHED;
    }

    /**
     * 解析本轮交替的真实前料；MES已显示历史后料时仍保留原历史前料展示语义。
     *
     * @return 第三种在机物料或原历史前料编码
     */
    public String resolveBeforeMaterialCode() {
        return this.resolveProductionRole() == ProductionRole.BEFORE_MATERIAL_PRODUCTION
                ? materialCode : plan.getBeforeMaterialCode();
    }

    /**
     * 判断本事件是否按历史L/R执行同料单副换模。
     *
     * @return 同料正规换模且历史标识为L或R时返回true
     */
    public boolean isSingleMouldReplacement() {
        return Objects.nonNull(plan)
                && MouldChangeTypeEnum.REGULAR.getCode().equals(plan.getChangeMouldType())
                && StringUtils.isNotEmpty(materialCode)
                && StringUtils.equals(materialCode, plan.getBeforeMaterialCode())
                && StringUtils.equals(materialCode, plan.getAfterMaterialCode())
                && (LhScheduleConstant.LEFT_MOULD.equals(plan.getLeftRightMould())
                || LhScheduleConstant.RIGHT_MOULD.equals(plan.getLeftRightMould()));
    }
}
