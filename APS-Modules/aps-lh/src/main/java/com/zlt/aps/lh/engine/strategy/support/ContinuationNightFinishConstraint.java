package com.zlt.aps.lh.engine.strategy.support;

import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

/**
 * 跨日两机大余量收尾的分配前约束，贯通角色匹配、提交及最终业务审计。
 * 公式角色截至十九点，另一台承接剩余余量，历史顺序不得重新均分数量。
 *
 * @author APS
 * @date 2026/10/13
 */
@Getter
public final class ContinuationNightFinishConstraint implements Serializable {
    /**
     * 贯通正式分配和缺失约束检测的业务规则标识。
     */
    public static final String RULE_NAME = "跨日双机3.2余量收尾";
    /**
     * 续作约束快照序列化版本。
     */
    private static final long serialVersionUID = 1L;
    /**
     * 一点五班产阈值的整数分子。
     */
    private static final int THRESHOLD_NUMERATOR = 3;
    /**
     * 一点五班产阈值的整数分母。
     */
    private static final int THRESHOLD_DENOMINATOR = 2;
    /**
     * 本组唯一合法目标量。
     */
    private final int targetQty;
    /**
     * 扣除前置生产量后的两机待分配余量。
     */
    private final int dayRemainingQty;
    /**
     * 用于一点五班产阈值及预留夜班的标准班产。
     */
    private final int shiftQty;
    /**
     * 本次跨日收尾的真实生产日期。
     */
    private final Date workDate;
    /**
     * 分配前绑定的物理机台编码，单控两侧共享该身份。
     */
    private final String machineCode;
    /**
     * 是否为按最小值公式分配且不晚于十九点的机台。
     */
    private final boolean formulaRole;
    /**
     * 进入两机规则前已冻结的本机累计生产量。
     */
    private final int prefixQty;
    /**
     * 本机截至十九点的真实累计合法容量。
     */
    private final int nodeCapacityQty;
    /**
     * 按公式和本机数量单位冻结的累计应排量。
     */
    private final int requiredQty;
    /**
     * 收尾日夜班在完整窗口中的班次序号。
     */
    private final int nightShiftIndex;
    /**
     * 本机在真实夜班容量内应优先占用的数量。
     */
    private final int requiredNightQty;

    /**
     * 冻结尚未提交的规则输入、物理角色和真实容量内的应排数量。
     *
     * @param targetQty 本组唯一合法目标量
     * @param dayRemainingQty 扣除前置生产量后的两机待分配余量
     * @param shiftQty 用于一点五班产阈值及预留夜班的标准班产
     * @param workDate 本次跨日收尾的真实生产日期
     * @param machineCode 分配前绑定的物理机台编码，单控两侧共享该身份
     * @param formulaRole 是否为按最小值公式分配且不晚于十九点的机台
     * @param prefixQty 进入两机规则前已冻结的本机累计生产量
     * @param nodeCapacityQty 本机截至十九点的真实累计合法容量
     * @param requiredQty 按公式和本机数量单位冻结的累计应排量
     * @param nightShiftIndex 收尾日夜班在完整窗口中的班次序号
     * @param requiredNightQty 本机在真实夜班容量内应优先占用的数量
     */
    public ContinuationNightFinishConstraint(int targetQty, int dayRemainingQty, int shiftQty,
            Date workDate, String machineCode, boolean formulaRole, int prefixQty, int nodeCapacityQty,
            int requiredQty, int nightShiftIndex, int requiredNightQty) {
        this.targetQty = targetQty;
        this.dayRemainingQty = dayRemainingQty;
        this.shiftQty = shiftQty;
        this.workDate = new Date(workDate.getTime());
        this.machineCode = machineCode;
        this.formulaRole = formulaRole;
        this.prefixQty = prefixQty;
        this.nodeCapacityQty = nodeCapacityQty;
        this.requiredQty = requiredQty;
        this.nightShiftIndex = nightShiftIndex;
        this.requiredNightQty = requiredNightQty;
    }

    /**
     * 使用整数交叉比较判断是否命中跨日一点五班产及以上规则。
     *
     * @param remainingQty 收尾日两机尚待分配的余量
     * @param standardShiftQty 标准单台班产
     * @return 标准班产有效且余量达到阈值时返回真
     */
    public static boolean isLargeRemainder(int remainingQty, int standardShiftQty) {
        return standardShiftQty > 0
                && (long) remainingQty * THRESHOLD_DENOMINATOR
                >= (long) standardShiftQty * THRESHOLD_NUMERATOR;
    }
}
