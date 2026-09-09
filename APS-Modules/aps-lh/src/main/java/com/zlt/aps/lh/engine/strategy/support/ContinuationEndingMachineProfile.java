package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.CapsuleReplacementTimeWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

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
    /** 当前物理机台的设备停机，避免候选反复扫描全工厂停机列表。 */
    private final List<MdmDevicePlanShut> deviceStops;
    /** 当前物理机台已登记的胶囊时间窗口，候选期只读。 */
    private final List<CapsuleReplacementTimeWindowDTO> capsuleWindows;

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
        String physical = LhSingleControlMachineUtil.resolvePhysicalMachineCode(originals.get(0).getLhMachineCode());
        this.deviceStops = CollectionUtils.isEmpty(context.getDevicePlanShutList()) ? Collections.emptyList()
                : context.getDevicePlanShutList().stream().filter(Objects::nonNull)
                .filter(stop -> StringUtils.equals(physical, LhSingleControlMachineUtil.resolvePhysicalMachineCode(stop.getMachineCode())))
                .collect(Collectors.toList());
        this.capsuleWindows = context.getCapsuleReplacementTimeWindowMap().values().stream().filter(Objects::nonNull)
                .filter(window -> StringUtils.equals(physical, window.getPhysicalMachineCode())).collect(Collectors.toList());
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
     * 校验数量是否能由连续满班前缀与一个合法归整尾班组成。
     * @param quantity 物理机台合计量
     * @return 是否属于真实容量时间轴的合法连续数量
     */
    public boolean isLegalQuantity(int quantity) {
        int remaining = quantity;
        if (remaining < 0) {
            return false;
        }
        for (int capacity : capacities) {
            if (remaining < capacity) {
                return remaining % multiple == 0;
            }
            remaining -= capacity;
        }
        return remaining == 0;
    }

    /**
     * 初始化时把已有后续占用截止转换为合法连续数量上限，同时考虑生产结束后的物理占用。
     * <p>只收紧本时间轴的缓存容量，不修改原结果或上下文。候选搜索开始后不再调用。</p>
     * @param deadline 已提交后料的最早占用时间
     */
    public void restrictToReleaseDeadline(Date deadline) {
        if (Objects.isNull(deadline)) {
            return;
        }
        long total = 0;
        for (int capacity : capacities) {
            total += capacity;
        }
        int low = 0;
        int high = (int) Math.min(Integer.MAX_VALUE, total);
        while (low < high) {
            int middle = low + (int) (((long) high - low + 1) / 2);
            int quantity = this.floorQuantity(middle);
            Date ready = quantity > 0 ? this.switchReadyTime(quantity) : null;
            if (quantity == 0 || (Objects.nonNull(ready) && !ready.after(deadline))) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        int remaining = this.floorQuantity(low);
        for (int position = 0; position < capacities.length; position++) {
            capacities[position] = Math.min(capacities[position], remaining);
            remaining -= capacities[position];
        }
    }

    /** @param limit 连续数量上限 @return 原时间轴内不超过上限的合法量 */
    private int floorQuantity(int limit) {
        int quantity = 0;
        for (int capacity : capacities) {
            int remaining = limit - quantity;
            if (remaining < capacity) {
                return quantity + remaining / multiple * multiple;
            }
            quantity += capacity;
        }
        return quantity;
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
     * 按最后有量的槽位确定生产收尾班次，整班结束点不归入下一班。
     * @param quantity 物理机台连续总量
     * @return 最后有量班次；无生产或超出容量时返回窗口外序号
     */
    public int productionEndingShift(int quantity) {
        if (quantity <= 0) {
            return shifts.size() + 1;
        }
        int remaining = quantity;
        for (int position = 0; position < capacities.length; position++) {
            if (remaining > 0 && capacities[position] > 0 && remaining <= capacities[position]) {
                return shifts.get(position).getShiftIndex();
            }
            remaining -= capacities[position];
        }
        return shifts.size() + 1;
    }

    /**
     * 反算指定收尾班次的连续量上限，仅截取最后中班尾量，不裁减前序满产中班。
     * @param shiftIndex 收尾班次序号
     * @return 该班次截止前的最大合法连续量；本班无合法尾量时不超过前序总量
     */
    public int endingQuantityLimit(int shiftIndex) {
        long prefix = 0;
        for (int position = 0; position < capacities.length; position++) {
            LhShiftConfigVO shift = shifts.get(position);
            if (shift.getShiftIndex() != shiftIndex) {
                prefix += capacities[position];
                continue;
            }
            int maximum = (int) Math.min(Integer.MAX_VALUE, prefix + capacities[position]);
            if (!shift.isAfternoonShift() || capacities[position] <= 0) {
                return maximum;
            }
            int low = (int) Math.min(Integer.MAX_VALUE, prefix);
            int high = maximum;
            // 使用完整有效时间轴二分，清洗、维修及单控较晚侧均由completionTime处理。
            while (low < high) {
                int middle = low + (int) (((long) high - low + 1) / 2);
                int quantity = this.floorQuantity(middle);
                if (quantity <= prefix || isEndingBeforeAfternoonCutoff(context, shift, this.completionTime(quantity))) {
                    low = middle;
                } else {
                    high = middle - 1;
                }
            }
            return this.floorQuantity(low);
        }
        return 0;
    }

    /**
     * 候选、提交和最终审计共用中班生产截止口径，20:00整不满足“之前”。
     * @param context 排程参数快照
     * @param shift 最后有量班次
     * @param completed 真实生产结束时间，单控整机取较晚侧
     * @return 时间完整且中班收尾严格早于既有禁换模起点；其他生产班次不限制
     */
    public static boolean isEndingBeforeAfternoonCutoff(LhScheduleContext context,
            LhShiftConfigVO shift, Date completed) {
        if (Objects.isNull(completed)) {
            return false;
        }
        Date cutoff = LhScheduleTimeUtil.buildTime(LhScheduleTimeUtil.clearTime(shift.getShiftStartDateTime()),
                LhScheduleTimeUtil.getNoMouldChangeStartHour(context), 0, 0);
        return !shift.isAfternoonShift() || completed.before(cutoff);
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
        if (Objects.isNull(readyTime)) {
            return null;
        }
        for (CapsuleReplacementTimeWindowDTO window : capsuleWindows) {
            if (Objects.nonNull(window.getReplacementStartTime()) && Objects.nonNull(window.getReplacementEndTime())
                    && !window.getReplacementStartTime().after(readyTime) && window.getReplacementEndTime().after(readyTime)) {
                readyTime = window.getReplacementEndTime();
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
        return ShiftCapacityResolverUtil.resolveShiftPlanEndTime(deviceStops,
                cleaningWindows.get(side), maintenanceWindows.get(side), profile.getLhMachineCode(),
                ShiftFieldUtil.getShiftStartTime(profile, shiftIndex),
                ShiftFieldUtil.getShiftEndTime(profile, shiftIndex), quantity,
                ShiftFieldUtil.getShiftPlanQty(profile, shiftIndex), context, profile, shifts.get(position));
    }
}
