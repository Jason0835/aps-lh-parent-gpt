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

/** 前日交替来源及班次边界快照；实际释放随最终续作收口同步，完成状态由结果关联维护。 */
@Data
public class PreviousAlternatePlanReleaseEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 历史L/R动作一次仅更换一副模具。 */
    public static final int SINGLE_REPLACEMENT_MOULD_QTY = 1;
    /** 原始交替计划，保留计划编号、前后物料及动作类型。 */
    private LhMouldChangePlan plan;
    /** 当前运行态机台编码，左右侧保持独立。 */
    private String machineCode;
    /** 下机前实际在机物料，不能用历史后料替代。 */
    private String materialCode;
    /** 下机前实际产品状态。 */
    private String productStatus;
    /** 本机历史前料最终未排产，交替从T日历史班次起点尝试。 */
    private boolean beforeMaterialNotScheduled;
    /** 历史计划日期及班次解析出的起点，前料未完成时必须在此下机。 */
    private Date plannedShiftStartTime;
    /** 历史计划班次结束，不允许换模或换活字块开始于该时刻及之后。 */
    private Date plannedShiftEndTime;
    /** 本轮准备资源窗口截止；未来生产提案不能借此抢占尚未轮到的准备班次。 */
    private Date preparationWindowEndTime;

    /** @return 历史计划截止与当前准备窗口截止中的较早值，采用半开区间 */
    public Date getPreparationDeadline() {
        Date plannedEnd = Objects.requireNonNull(plannedShiftEndTime, "历史计划班次截止未冻结");
        return Objects.nonNull(preparationWindowEndTime) && preparationWindowEndTime.before(plannedEnd)
                ? preparationWindowEndTime : plannedEnd;
    }

    /** 前料最终未排时，T日对应历史班次的承接起点。 */
    private Date noProductionStartTime;
    /** 登记时为本次动作边界；有余量的严格收尾可收敛为更早的真实下机时刻。 */
    private Date offlineTime;
    /** 下机前整组实际模具，供单副置换及整组替换判定。 */
    private Set<String> originalMouldCodes = new LinkedHashSet<String>(2);
    /** 单副置换中仍留在原机台的模具；登记事件时冻结，预演和回滚不得释放。 */
    private Set<String> retainedMouldCodes = new LinkedHashSet<String>(2);

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
