package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        if (request.hasDailyQuantityTarget()) {
            validateDailyQuantity(request, shifts);
        }
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
        // 零目标日仍保留此前生产前缀，最早释放不能倒退到目标业务日之前。
        if (request.hasDailyQuantityTarget() && available.before(zeroQuantityAvailableTime)) {
            available = zeroQuantityAvailableTime;
        }
        Date released = request.getProfile().physicalReadyTime(available, ruleDecision.getRuleOffMachineTime());
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
     * 校验冻结日目标和后续零量，扣账或硬约束改变数量时阻止发布错误的完成事实。
     *
     * @param request 已冻结日目标请求
     * @param shifts 完整窗口班次
     * @throws IllegalStateException 当日目标未完成或目标日后仍有前料产量时抛出
     */
    private static void validateDailyQuantity(TimedMachineOffRequest request, List<LhShiftConfigVO> shifts) {
        long dayQty = 0;
        long laterQty = 0;
        for (LhScheduleResult result : request.getProfile().getOriginals()) {
            for (LhShiftConfigVO shift : shifts) {
                LocalDate workDate = shift.getWorkDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, shift.getShiftIndex());
                int plannedQty = quantity == null ? 0 : quantity;
                if (workDate.equals(request.getStartDate())) {
                    dayQty += plannedQty;
                } else if (workDate.isAfter(request.getStartDate())) {
                    laterQty += plannedQty;
                }
            }
        }
        if (dayQty != request.requiredDayPlanQty() || laterQty != 0) {
            log.error("续作日目标下机数量冲突, machine={}, date={}, target={}, actual={}, later={}",
                    request.getProfile().getOriginals().get(0).getLhMachineCode(), request.getStartDate(),
                    request.requiredDayPlanQty(), dayQty, laterQty);
            throw new IllegalStateException(
                    I18nUtil.getMessage("ui.data.column.lhScheduleResult.scheduleExecuteError"));
        }
    }

    /**
     * 按请求类型解析生产区间起点，日目标请求包含该业务日所有可排班次。
     *
     * @param shifts 本次真实排程窗口
     * @param request 已冻结的下机请求
     * @return 目标日的生产起始班次，缺失时输出日志并返回空，由调用方跳过本次调整
     */
    public static LhShiftConfigVO resolveProductionStartShift(List<LhShiftConfigVO> shifts,
            TimedMachineOffRequest request) {
        if (!request.hasDailyQuantityTarget()) {
            return resolveProductionStartShift(shifts, request.getStartDate());
        }
        LhShiftConfigVO start = shifts.stream()
                .filter(shift -> request.getStartDate().equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .orElse(null);
        if (start == null) {
            log.warn("日目标下机需求日缺少班次配置，跳过本次下机调整, productionDate={}, materialCode={}, machineCode={}",
                    request.getStartDate(), request.getSourceSku().getMaterialCode(),
                    request.getProfile().getOriginals().get(0).getLhMachineCode());
        }
        return start;
    }

    /**
     * 解析需求日最早配置的早班或中班，缺早班时允许从中班继续；此前班次属于保留前缀。
     *
     * @param shifts 本次真实排程窗口
     * @param startDate 前置确定的需求业务日
     * @return 需求日首个已配置的早班或中班，缺失时输出日志并返回空
     */
    public static LhShiftConfigVO resolveProductionStartShift(List<LhShiftConfigVO> shifts, LocalDate startDate) {
        LhShiftConfigVO start = shifts.stream()
                .filter(shift -> shift.isMorningShift() || shift.isAfternoonShift())
                .filter(shift -> startDate.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .min(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime))
                .orElse(null);
        if (start == null) {
            log.warn("按时间下机需求日缺少早班或中班配置，跳过本次下机调整, productionDate={}", startDate);
        }
        return start;
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
