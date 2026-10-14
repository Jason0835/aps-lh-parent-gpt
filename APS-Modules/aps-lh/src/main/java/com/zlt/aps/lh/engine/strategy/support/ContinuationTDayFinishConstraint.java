package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

/**
 * T日节点分配前冻结的业务输入，最终审计不得从输出截止时间反推规则。
 *
 * @author APS
 * @date 2026/10/10
 */
@Getter
public final class ContinuationTDayFinishConstraint implements Serializable {
    /**
     * 续作快照序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 06点释放节点。
     */
    private static final int MORNING_RELEASE_HOUR = 6;
    /**
     * 中班19点释放节点。
     */
    private static final int AFTERNOON_RELEASE_HOUR = 19;
    /**
     * 本组唯一合法目标量。
     */
    private final int targetQty;
    /**
     * 用于大小余量阈值的标准单台班产。
     */
    private final int shiftQty;
    /**
     * 真实T日日期。
     */
    private final Date workDate;
    /**
     * 本机在规则节点前的真实合法容量。
     */
    private final int nodeCapacityQty;
    /**
     * 相同数量单位下本节点角色应尽量承接的数量；异质单位由守恒内核调整时为空。
     */
    private final Integer requiredQty;

    /**
     * 冻结节点角色的原始需求和容量，独立于计算完成后的计划与截止字段。
     *
     * @param targetQty 本组合法目标
     * @param shiftQty 标准单台班产
     * @param workDate 真实生产日期
     * @param nodeCapacityQty 已扣除硬边界的节点容量
     * @param requiredQty 同单位下必须承接的节点量，异质单位为空
     */
    public ContinuationTDayFinishConstraint(int targetQty, int shiftQty, Date workDate,
            int nodeCapacityQty, Integer requiredQty) {
        this.targetQty = targetQty;
        this.shiftQty = shiftQty;
        this.workDate = new Date(workDate.getTime());
        this.nodeCapacityQty = nodeCapacityQty;
        this.requiredQty = requiredQty;
    }

    /**
     * 仅根据业务日、合法余量和班产确定06点或19点节点。
     *
     * @param workDate 真实生产日期
     * @param targetQty 合法余量
     * @param shiftQty 标准单台班产
     * @return 该场景唯一节点
     */
    public static Date resolveNode(Date workDate, int targetQty, int shiftQty) {
        return LhScheduleTimeUtil.buildTime(LhScheduleTimeUtil.clearTime(workDate),
                targetQty <= shiftQty ? MORNING_RELEASE_HOUR : AFTERNOON_RELEASE_HOUR, 0, 0);
    }
}
