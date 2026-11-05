package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 计划性维修的固定08:00前置准入规则，只读取已确认的在机身份和收尾事实。
 * 不推算收尾、不发布设备窗口、不修改生产账本或设备来源。
 *
 * @author APS
 * @date 2026/10/29
 */
public final class LhPlannedRepairAdmissionPolicy {

    /**
     * 每个维修来源自然日的判断基准，独立于维修实际开始参数。
     */
    private static final LocalTime ADMISSION_TIME = LocalTime.of(8, 0);

    /**
     * 禁止创建无状态规则实例。
     */
    private LhPlannedRepairAdmissionPolicy() {
    }

    /**
     * 判断已确认在机SKU是否因严格晚于08:00收尾而跳过当日05。
     * 调用方必须先证明完整收尾，不能传入窗口截止、故障下机或初始化占位时间。
     *
     * @param plan 当前设备计划
     * @param inMachine 已确认的在机身份
     * @param finishTime 未扣本次维修约束的真实收尾时刻，无法确认时为空
     * @return 仅05且在机SKU严格晚于来源日08:00收尾时返回true
     */
    public static boolean shouldSkip(MdmDevicePlanShut plan, boolean inMachine, Date finishTime) {
        return plan != null && MachineStopTypeEnum.PLANNED_REPAIR.getCode().equals(plan.getMachineStopType())
                && plan.getBeginDate() != null && inMachine && finishTime != null
                && finishTime.after(resolveBoundary(plan));
    }

    /**
     * 从维修来源自然日构造固定判断时刻，不使用请求目标日或实际维修开始参数。
     *
     * @param plan 开始日期非空的05来源或同日实际计划
     * @return 来源日期08:00
     */
    public static Date resolveBoundary(MdmDevicePlanShut plan) {
        ZoneId zone = ZoneId.systemDefault();
        return Date.from(plan.getBeginDate().toInstant().atZone(zone).toLocalDate()
                .atTime(ADMISSION_TIME).atZone(zone).toInstant());
    }

    /**
     * 核验判断时点前已经生产的SKU，或等待供胚等约束但仍未切换的初始在机SKU。
     * 正量生产起点由调用方提供；没有真实生产段时，不以初始化结束时间猜测收尾。
     *
     * @param context 当前排程上下文及不可变初始机台快照
     * @param result 待核验的生产身份
     * @param productionStart 首个正量生产段起点
     * @param boundary 当前维修的08:00基准
     * @return 存在实际在机依据时返回true，空机和未知生产段返回false
     */
    public static boolean isInMachine(LhScheduleContext context, LhScheduleResult result,
            Date productionStart, Date boundary) {
        if (productionStart == null || StringUtils.isEmpty(result.getMaterialCode())) {
            return false;
        }
        if (!productionStart.after(boundary)) {
            return true;
        }
        MachineScheduleDTO initial = context.getInitialMachineScheduleMap().get(result.getLhMachineCode());
        return initial != null && StringUtils.equals(initial.getCurrentMaterialCode(), result.getMaterialCode())
                && !"1".equals(result.getIsChangeMould()) && !"1".equals(result.getIsTypeBlock());
    }
}
