package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;

import java.util.List;
import java.util.Objects;

import java.util.Date;

/**
 * 单台物理机台的只读收尾产能时间轴。单控整机持有两侧，普通机台持有一条结果。
 * <p>缓存本组的班产、有效起止和停机窗口，候选只传总量，不重复复制结果或全量上下文。</p>
 */
public final class ContinuationEndingMachineProfile {
    /** 当前排程上下文，只读取设备停机和停产保机约束。 */
    private final LhScheduleContext context;
    /** 真实班次顺序。 */
    private final List<LhShiftConfigVO> shifts;
    /** 原结果，提交前不修改。 */
    private final List<LhScheduleResult> originals;
    /** 各侧原始有效容量与窗口，用于按既有时间内核反算尾量。 */
    private final List<LhScheduleResult> rawProfiles;
    /** 各侧已解析的清洗窗口，避免在候选循环内重复解析。 */
    private final List<List<MachineCleaningWindowDTO>> cleaningWindows;
    /** 各侧已解析的维修窗口。 */
    private final List<List<MachineMaintenanceWindowDTO>> maintenanceWindows;
    /** 应用已有释放和后续占用上限后的物理机台合计容量。 */
    private final int[] capacities;
    /** 物理机台残班归整单位。 */
    private final int multiple;

    /**
     * 构造只读时间轴，各侧容量必须已经过真实资源约束校验。
     * @param context 排程上下文
     * @param shifts 真实班次
     * @param originals 原结果
     * @param rawProfiles 各侧未截短时间比例基数
     * @param cleaningWindows 各侧清洗窗口
     * @param maintenanceWindows 各侧维修窗口
     * @param capacities 合计有效容量
     * @param multiple 残班归整单位
     */
    public ContinuationEndingMachineProfile(LhScheduleContext context, List<LhShiftConfigVO> shifts,
                                            List<LhScheduleResult> originals, List<LhScheduleResult> rawProfiles,
                                            List<List<MachineCleaningWindowDTO>> cleaningWindows,
                                            List<List<MachineMaintenanceWindowDTO>> maintenanceWindows,
                                            int[] capacities, int multiple) {
        this.context = context;
        this.shifts = shifts;
        this.originals = originals;
        this.rawProfiles = rawProfiles;
        this.cleaningWindows = cleaningWindows;
        this.maintenanceWindows = maintenanceWindows;
        this.capacities = capacities;
        this.multiple = multiple;
    }

    /** @return 原始结果列表，单控整机包含L/R两侧 */
    public List<LhScheduleResult> getOriginals() {
        return originals;
    }

    /** @return 物理机台有效班产，仅供当前试算读取 */
    public int[] getCapacities() {
        return capacities;
    }

    /** @return 残班归整单位 */
    public int getMultiple() {
        return multiple;
    }

    /**
     * 按连续生产总量计算真实生产完成时间，单控取较晚一侧。
     * @param quantity 物理机台合计排量
     * @return 完成时间，无生产返回null
     */
    public Date completionTime(int quantity) {
        int remaining = quantity;
        for (int position = 0; position < capacities.length; position++) {
            if (capacities[position] <= 0) {
                continue;
            }
            if (remaining > capacities[position]) {
                remaining -= capacities[position];
                continue;
            }
            if (remaining <= 0 || remaining % originals.size() != 0) {
                return null;
            }
            Date completion = null;
            for (int side = 0; side < originals.size(); side++) {
                Date sideEnd = this.sideEndTime(side, position, remaining / originals.size());
                if (Objects.isNull(sideEnd)) {
                    return null;
                }
                if (Objects.isNull(completion) || sideEnd.after(completion)) {
                    completion = sideEnd;
                }
            }
            return completion;
        }
        return null;
    }

    /**
     * 可交接时间必须保留停产保机占用，不能仅比较最后生产班次。
     * @param quantity 连续生产总量
     * @return 物理释放时间
     */
    public Date switchReadyTime(int quantity) {
        Date readyTime = this.completionTime(quantity);
        for (LhScheduleResult result : originals) {
            if (context.isContinuousStopHoldMachine(result.getLhMachineCode())) {
                return shifts.get(shifts.size() - 1).getShiftEndDateTime();
            }
        }
        return readyTime;
    }

    /**
     * 将已经通过整组校验的数量写入传入的结果副本；原结果由调用方统一提交。
     * @param proposed 各侧结果副本
     * @param quantity 物理机台合计排量
     */
    public void writePlan(List<LhScheduleResult> proposed, int quantity) {
        int remaining = quantity;
        for (int position = 0; position < capacities.length; position++) {
            int allocated = Math.min(remaining, capacities[position]);
            int sideQty = allocated / originals.size();
            int shiftIndex = shifts.get(position).getShiftIndex();
            for (int side = 0; side < originals.size(); side++) {
                Date start = sideQty > 0 ? ShiftFieldUtil.getShiftStartTime(rawProfiles.get(side), shiftIndex) : null;
                Date end = sideQty > 0 ? this.sideEndTime(side, position, sideQty) : null;
                ShiftFieldUtil.setShiftPlanQty(proposed.get(side), shiftIndex, sideQty, start, end);
            }
            remaining -= allocated;
        }
    }

    /**
     * 使用原始有效容量反算各侧完成时间，截断容量不得作为新的时间比例分母。
     * @param side 侧序号
     * @param position 班次位置
     * @param quantity 单侧数量
     * @return 真实完成时间
     */
    private Date sideEndTime(int side, int position, int quantity) {
        LhScheduleResult profile = rawProfiles.get(side);
        int shiftIndex = shifts.get(position).getShiftIndex();
        return ShiftCapacityResolverUtil.resolveShiftPlanEndTime(context.getDevicePlanShutList(),
                cleaningWindows.get(side), maintenanceWindows.get(side), profile.getLhMachineCode(),
                ShiftFieldUtil.getShiftStartTime(profile, shiftIndex),
                ShiftFieldUtil.getShiftEndTime(profile, shiftIndex), quantity,
                ShiftFieldUtil.getShiftPlanQty(profile, shiftIndex));
    }
}
