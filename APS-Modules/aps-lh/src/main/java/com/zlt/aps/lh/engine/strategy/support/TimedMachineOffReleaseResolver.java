package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.util.ShiftFieldUtil;

import java.util.Date;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 以最终逐班数量和真实结束时间计算前料释放；预演和正式扣账后发布共用同一口径。
 *
 * @author APS-LH
 * @date 2026-10-13
 */
public final class TimedMachineOffReleaseResolver {
    /**
     * 根据实际生产结果生成新的释放事实，保留原规则节点和快照对象。
     * 数量被最终扣账裁减后只复核时间，不重新摊量或消费账本。
     *
     * @param request 已选物理机台及真实资源画像
     * @param ruleDecision 原规则决策或上一次已发布事实
     * @param shifts 排程窗口班次
     * @param zeroQuantityAvailableTime 零量机台在本需求日的已知可用起点
     * @return 独立决策对象，包含实际量、生产完成和物理释放时刻
     * @throws IllegalStateException 有量班次缺少完成时间或缺少排程窗口时抛出
     * @throws ArithmeticException 物理机台合计数量超过整数范围时抛出
     */
    public OffMachineDecision resolve(TimedMachineOffRequest request, OffMachineDecision ruleDecision,
            List<LhShiftConfigVO> shifts, Date zeroQuantityAvailableTime) {
        Date completed = null;
        int totalQuantity = 0;
        for (LhScheduleResult result : request.getProfile().getOriginals()) {
            for (LhShiftConfigVO shift : shifts) {
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                if (Objects.isNull(quantity) || quantity <= 0) {
                    continue;
                }
                Date end = ShiftFieldUtil.getShiftEndTime(result, shift.getShiftIndex());
                if (Objects.isNull(end)) {
                    throw new IllegalStateException("按时间下机有量班次缺少真实完成时间");
                }
                totalQuantity = Math.addExact(totalQuantity, quantity);
                if (Objects.isNull(completed) || end.after(completed)) {
                    completed = end;
                }
            }
        }
        Date available = Objects.nonNull(completed) ? completed : zeroQuantityAvailableTime;
        Date released = request.getProfile().physicalReadyTime(available);
        OffMachineDecision resolved = BeanUtil.copyProperties(ruleDecision, OffMachineDecision.class);
        resolved.setScheduledQty(totalQuantity);
        resolved.setProductionEndTime(completed);
        Date windowEnd = shifts.stream().map(LhShiftConfigVO::getShiftEndDateTime)
                .max(Date::compareTo).orElseThrow(() -> new IllegalStateException("按时间下机缺少窗口结束时间"));
        // 窗口末端仅表示本轮计算范围结束，不能虚构窗口内已经下机。
        resolved.setOffMachineTime(released.before(windowEnd) ? released : null);
        resolved.setOccupancyEndTime(released);
        LhShiftConfigVO releaseShift = shifts.stream()
                .filter(shift -> !released.before(shift.getShiftStartDateTime())
                        && released.before(shift.getShiftEndDateTime()))
                .findFirst().orElse(null);
        resolved.setPlanDate(Objects.nonNull(releaseShift)
                ? releaseShift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null);
        resolved.setShift(Objects.nonNull(releaseShift) ? releaseShift.getShiftType() : null);
        return resolved;
    }

    /**
     * 解析需求日最早配置的早班或中班，缺早班时允许从中班继续；此前班次属于保留前缀。
     *
     * @param shifts 本次真实排程窗口
     * @param startDate 前置确定的需求业务日
     * @return 需求日首个已配置的早班或中班
     * @throws IllegalStateException 需求日没有早班或中班配置时抛出
     */
    public static LhShiftConfigVO resolveProductionStartShift(List<LhShiftConfigVO> shifts, LocalDate startDate) {
        return shifts.stream()
                .filter(shift -> shift.isMorningShift() || shift.isAfternoonShift())
                .filter(shift -> startDate.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .orElseThrow(() -> new IllegalStateException("按时间下机需求日缺少早班或中班配置"));
    }

    /**
     * 按实际班次工作日解析释放日期，跨日夜班归其业务日；窗口外保留实际自然日期。
     *
     * @param shifts 本次真实排程窗口
     * @param released 实际释放时刻
     * @return 窗口班次业务日或窗口外实际日期
     */
    public static LocalDate resolveReleaseBusinessDate(List<LhShiftConfigVO> shifts, Date released) {
        return shifts.stream()
                .filter(shift -> !released.before(shift.getShiftStartDateTime())
                        && released.before(shift.getShiftEndDateTime()))
                .map(LhShiftConfigVO::getWorkDate).findFirst().orElse(released)
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

}
