package com.zlt.aps.lh.component;

import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * 新增排产日期准入，仅解析业务日和准备下限，不修改候选或资源账本。
 *
 * @author APS
 * @since 2026/11/12
 */
public final class NewProductionDateAdmissionPolicy {

    /**
     * 禁止实例化无状态日期策略。
     */
    private NewProductionDateAdmissionPolicy() {
    }

    /**
     * 判断新增动作是否位于窗口T日之后，历史指定和续作由调用方保持原入口语义。
     *
     * @param context 已初始化窗口日期的上下文
     * @param businessDate 按班次workDate解析的业务日
     * @return T日之后允许新增，T日及更早日期不允许
     */
    public static boolean isAllowed(LhScheduleContext context, LocalDate businessDate) {
        LocalDate firstDate = context.getScheduleDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return businessDate.isAfter(firstDate);
    }

    /**
     * 读取T+1首班真实起点，窗口外沿用原喷砂恢复的次日日期供未排收口。
     *
     * @param context 已初始化的业务日及班次窗口
     * @return 新增准备不得早于的时间，夜班仍由原禁换模规则约束
     */
    public static Date resolveNotBeforeTime(LhScheduleContext context) {
        LocalDate nextDate = context.getScheduleDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1);
        return context.getScheduleWindowShifts().stream()
                .filter(shift -> Objects.nonNull(shift.getWorkDate())
                        && nextDate.equals(shift.getWorkDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()))
                .map(LhShiftConfigVO::getShiftStartDateTime)
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .orElse(Date.from(nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    /**
     * 抬高新增准备起点，保留原业务计算得到的更晚时点及无可用时间结果。
     *
     * @param context 已初始化的排程窗口
     * @param readyTime 原规则计算的就绪时间，允许为空
     * @return 与日期下限的较晚时刻；原时间为空时仍为空
     */
    public static Date alignStart(LhScheduleContext context, Date readyTime) {
        if (Objects.isNull(readyTime)) {
            return null;
        }
        Date notBefore = resolveNotBeforeTime(context);
        return readyTime.before(notBefore) ? notBefore : readyTime;
    }
}
