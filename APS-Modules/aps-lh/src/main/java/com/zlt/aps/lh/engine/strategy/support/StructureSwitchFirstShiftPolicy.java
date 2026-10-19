package com.zlt.aps.lh.engine.strategy.support;

import cn.hutool.core.bean.BeanUtil;
import com.zlt.aps.cx.entity.config.CxEmbryoLhTime;
import com.zlt.aps.lh.api.domain.dto.SkuScheduleDTO;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 按后结构续作标识及供胚所在班统一判定结构切换规则首班，试算不登记资源。
 *
 * @author APS
 * @date 2026/10/14
 */
@Slf4j
public final class StructureSwitchFirstShiftPolicy {
    /**
     * T日晚班只作判定坐标，不新增结果班次槽位。
     */
    private static final int HISTORICAL_SHIFT_INDEX = 0;
    /**
     * 既有夜班解析器要求正偏移表示前一自然日晚间，历史模板以T-1为基准并偏移一日。
     */
    private static final int HISTORICAL_NIGHT_DATE_OFFSET = 1;

    /**
     * 工具类不允许实例化。
     */
    private StructureSwitchFirstShiftPolicy() {
    }

    /**
     * 在原八班前补齐T日晚班；复用当前工厂晚班模板，独立后置副本不扩展窗口。
     *
     * @param context 已加载班次配置的上下文
     * @return 按真实时间排序的判定班次
     */
    public static List<LhShiftConfigVO> decisionShifts(LhScheduleContext context) {
        List<LhShiftConfigVO> shifts = new ArrayList<>(StructureSwitchSchedulingPolicy.orderedShifts(context));
        if (context.isIsolatedNextShiftPlan() || shifts.isEmpty() || Objects.isNull(context.getScheduleDate())) {
            return shifts;
        }
        LhShiftConfigVO template = shifts.stream().filter(LhShiftConfigVO::isNightShift).findFirst().orElse(null);
        if (Objects.nonNull(template)) {
            LhShiftConfigVO historical = new LhShiftConfigVO();
            BeanUtil.copyProperties(template, historical);
            historical.setScheduleBaseDate(LhScheduleTimeUtil.addDays(context.getScheduleDate(),
                    -HISTORICAL_NIGHT_DATE_OFFSET));
            historical.setDateOffset(HISTORICAL_NIGHT_DATE_OFFSET);
            historical.setShiftIndex(HISTORICAL_SHIFT_INDEX);
            historical.setShiftName("T日晚班");
            if (historical.getShiftEndDateTime().equals(shifts.get(0).getShiftStartDateTime())) {
                shifts.add(historical);
            }
        }
        shifts.sort(Comparator.comparing(LhShiftConfigVO::getShiftStartDateTime));
        return shifts;
    }

    /**
     * 按后结构续作标识及供胚所在班认定首班，已成功提交的同来源判定跨SKU共享。
     * 续作结构固定T日晚班；非续作结构取供胚所在班，窗口前T日晚班映射为窗口首班。
     * 首班定位不判断剩余产能，实际首检和生产继续执行原数量及资源约束。
     *
     * @param context 本批上下文和班次配置
     * @param source 有效供胚来源
     * @param sku 当前候选SKU
     * @param machineCode 参照运行态机台编码
     * @return 只读首班判定；非结构切换或非续作供胚超出既有判定范围时为空
     */
    public static StructureSwitchFirstShiftDecision resolve(LhScheduleContext context, CxEmbryoLhTime source,
            SkuScheduleDTO sku, String machineCode) {
        if (!StructureSwitchSchedulingPolicy.isEnabled(context) || Objects.isNull(source)
                || Objects.isNull(source.getEarliestLhTime()) || Objects.isNull(sku)) {
            return null;
        }
        StructureSwitchRuntimeState state = context.getStructureSwitchRuntimeMap().get(source.getNextStructureName());
        if (Objects.nonNull(state) && Objects.equals(source.getId(), state.getSourceId())
                && Objects.nonNull(state.getFirstShiftDecision())) {
            return state.getFirstShiftDecision();
        }
        if (context.isIsolatedNextShiftPlan()) {
            return null;
        }
        List<LhShiftConfigVO> shifts = decisionShifts(context);
        LhShiftConfigVO supplyShift = LhScheduleTimeUtil.resolveShiftByTime(shifts, source.getEarliestLhTime());
        boolean structureContinuation = context.getSwitchedNextStructureNameSet().contains(source.getNextStructureName());
        if (!structureContinuation && Objects.isNull(supplyShift)) {
            return null;
        }
        LhShiftConfigVO first;
        String reason;
        if (structureContinuation) {
            // 后结构已完成承接，固定从窗口前T日晚班计序，不能被供胚或实际首产后移。
            first = shifts.stream().filter(shift -> shift.getShiftIndex() == HISTORICAL_SHIFT_INDEX)
                    .findFirst().orElse(null);
            reason = Objects.nonNull(first) ? "后结构续作，首班固定T日晚班" : "首班超出本次判定范围";
        } else if (supplyShift.getShiftIndex() == HISTORICAL_SHIFT_INDEX) {
            // 非续作结构不得占用窗口前首班坐标，供胚仍保留其真实所属班用于额度计算。
            first = StructureSwitchSchedulingPolicy.orderedShifts(context).stream().findFirst().orElse(null);
            reason = Objects.nonNull(first) ? "后结构非续作，T日晚班供胚映射到窗口首班" : "首班超出本次判定范围";
        } else {
            first = supplyShift;
            reason = "后结构非续作，首班取供胚所在班";
        }
        log.debug("结构切换首班判定, factoryCode={}, scheduleDate={}, batchNo={}, sourceId={}, sku={}, "
                        + "status={}, machine={}, structure={}, structureContinuation={}, waitNotify={}, "
                        + "supplyTime={}, supplyShift={}, firstShift={}, reason={}",
                context.getFactoryCode(), context.getScheduleTargetDate(), context.getBatchNo(), source.getId(),
                sku.getMaterialCode(), sku.getProductStatus(), machineCode, source.getNextStructureName(),
                structureContinuation, source.getIsWaitNotify(), source.getEarliestLhTime(),
                Objects.isNull(supplyShift) ? null : supplyShift.getShiftName(),
                Objects.isNull(first) ? null : first.getShiftName(), reason);
        return new StructureSwitchFirstShiftDecision(source.getId(), source.getEarliestLhTime(), supplyShift,
                first, machineCode, structureContinuation, reason,
                Collections.unmodifiableList(new ArrayList<>(shifts)));
    }
}
