package com.zlt.aps.lh.engine.strategy.support;

import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;

import java.util.Date;
import java.util.Objects;

/**
 * 历史指定承接共用的准备及结构生产边界，只读取本轮已确认的精确历史事实。
 *
 * @author APS
 * @date 2026/10/21
 */
public final class PreviousAlternateHandoffPolicy {

    /**
     * 禁止实例化无状态规则。
     */
    private PreviousAlternateHandoffPolicy() {
    }

    /**
     * 确认当前作用域为本批有效历史机台及后料状态，普通候选不能借用准备资格。
     *
     * @param context 当前排程上下文
     * @param sku 候选后物料
     * @param machineCode 本次指定运行侧
     * @return 精确历史作用域有效时返回true
     */
    public static boolean isEligibleAction(LhScheduleContext context, SkuScheduleDTO sku, String machineCode) {
        return Objects.nonNull(context) && !context.isIsolatedNextShiftPlan()
                && context.isPreviousAlternateAction(sku, machineCode)
                && context.isEligiblePreviousAlternateCombination(sku,
                context.getActivePreviousAlternateEvent().getPlan(), machineCode);
    }

    /**
     * 解析结构后料待准备的真实释放边界，供预演、增机日期对齐及生产切片共同读取。
     * 供胚、结构名额、设备和数量准入仍由原链路取较晚值及复核。
     *
     * @param context 当前已准入候选的历史作用域
     * @param sku 指定后物料
     * @param machineCode 精确指定运行侧
     * @return 真实释放所在班次起点；普通候选、前料仍需生产或无结构来源时为空
     */
    public static Date resolveStructureProductionStart(LhScheduleContext context,
            SkuScheduleDTO sku, String machineCode) {
        if (!isEligibleAction(context, sku, machineCode)
                || context.getActivePreviousAlternateEvent().resolveProductionRole()
                != PreviousAlternatePlanReleaseEvent.ProductionRole.AFTER_MATERIAL_PREPARATION
                || Objects.isNull(StructureSwitchSchedulingPolicy.resolveSource(context, sku))) {
            return null;
        }
        Date release = context.getActivePreviousAlternateEvent().getOfflineTime();
        if (Objects.isNull(release)) {
            return null;
        }
        // 切片保留释放所在的整个班次；实际准备仍由机台就绪及释放时间约束，不能取班初提前释放。
        return context.getScheduleWindowShifts().stream()
                .filter(shift -> !release.before(shift.getShiftStartDateTime())
                        && release.before(shift.getShiftEndDateTime()))
                .map(shift -> shift.getShiftStartDateTime()).findFirst().orElse(null);
    }
}
