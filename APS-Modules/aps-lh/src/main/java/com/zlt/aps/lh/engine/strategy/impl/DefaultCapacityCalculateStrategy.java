/**
 * Copyright (c) 2008, 智立通（厦门）科技有限公司 All rights reserved。
 */
package com.zlt.aps.lh.engine.strategy.impl;

import com.zlt.aps.lh.api.domain.dto.MachineScheduleDTO;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.engine.strategy.ICapacityCalculateStrategy;
import com.zlt.aps.lh.service.impl.LhMaintenanceScheduleService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 默认产能计算策略实现
 * <p>基于硫化时间和模数计算班产、日产、首班产量和开产时间，
 * 并综合考虑保养、维修等计划停机情况</p>
 *
 * @author APS
 */
@Slf4j
@Component
public class DefaultCapacityCalculateStrategy implements ICapacityCalculateStrategy {

    private final LhMaintenanceScheduleService maintenanceScheduleService = new LhMaintenanceScheduleService();

    @Override
    public int calculateShiftCapacity(LhScheduleContext context, int lhTimeSeconds, int mouldQty) {
        if (lhTimeSeconds <= 0 || mouldQty <= 0) {
            return 0;
        }
        // 计算公式：(班次时间秒数 / 硫化时间秒数) 向下取整 * 模数
        int shiftSeconds = LhScheduleTimeUtil.getShiftDurationHours(context) * 3600;
        return (shiftSeconds / lhTimeSeconds) * mouldQty;
    }

    @Override
    public Date calculateStartTime(LhScheduleContext context, String machineCode, Date endingTime) {
        if (endingTime == null) {
            return new Date();
        }

        // 语义调整为“机台准备就绪时间”，仅表达机台从何时开始可以继续安排换模/首检/生产
        Date baseReadyTime = endingTime;

        // 判断机台是否有精度保养计划，若有则取其就绪时间。
        // 清洗与换模/换活字块的重叠判定已下沉到后续切换链路，不在这里提前固化 readyTime。
        Date maintenanceStartTime = calculateMaintenanceStartTime(context, machineCode, baseReadyTime);
        Date capsuleReplacementReadyTime = ShiftCapacityResolverUtil.resolveCapsuleReplacementReadyTime(
                context, machineCode, baseReadyTime);

        // 取基础可用、保养后可用及换胶囊后可用时间的最大值；05/06只通过各自产能时间轴生效。
        Date maxStartTime = baseReadyTime;
        if (maintenanceStartTime != null && maintenanceStartTime.after(maxStartTime)) {
            maxStartTime = maintenanceStartTime;
        }
        if (capsuleReplacementReadyTime != null && capsuleReplacementReadyTime.after(maxStartTime)) {
            maxStartTime = capsuleReplacementReadyTime;
        }

        if (context == null || !context.isNewSpecProposalPreview()) {
            log.debug("计算机台准备就绪时间, 机台: {}, 收尾时间: {}, 保养后就绪时间: {}, "
                            + "换胶囊后就绪时间: {}, 最终就绪时间: {}",
                    machineCode, LhScheduleTimeUtil.formatDateTime(endingTime),
                    LhScheduleTimeUtil.formatDateTime(maintenanceStartTime),
                    LhScheduleTimeUtil.formatDateTime(capsuleReplacementReadyTime),
                    LhScheduleTimeUtil.formatDateTime(maxStartTime));
        }
        return maxStartTime;
    }

    @Override
    public int calculateFirstShiftQty(Date startTime, Date shiftEndTime, int lhTimeSeconds, int mouldQty) {
        if (startTime == null || shiftEndTime == null || lhTimeSeconds <= 0 || mouldQty <= 0) {
            return 0;
        }
        // 首班可用时间（秒）= 首班次结束时间 - 上机时间
        long availableSeconds = (shiftEndTime.getTime() - startTime.getTime()) / 1000L;
        if (availableSeconds <= 0) {
            return 0;
        }
        // 首班计划量 = (可用时间 / 硫化时间) 向下取整 * 模数
        return (int) (availableSeconds / lhTimeSeconds) * mouldQty;
    }

    @Override
    public int calculateDailyCapacity(int lhTimeSeconds, int mouldQty) {
        if (lhTimeSeconds <= 0 || mouldQty <= 0) {
            return 0;
        }
        // 日硫化量 = (24 * 3600 / 硫化时间) * 模数
        return (24 * 3600 / lhTimeSeconds) * mouldQty;
    }

    /**
     * 计算设备保养后的开产时间
     * <p>
     * 已进入当前固定班次范围的保养必须先完成再安排后续 SKU；超出当前范围的未来保养只有在
     * 基础就绪时间真实落入“保养开始～胶囊预热完成”区间时才顺延，避免未来计划提前锁机。
     * </p>
     *
     * @param context     排程上下文
     * @param machineCode 机台编号
     * @param baseReadyTime 机台原始就绪时间
     * @return 保养后的开产时间，null表示无保养计划
     */
    private Date calculateMaintenanceStartTime(LhScheduleContext context, String machineCode, Date baseReadyTime) {
        MachineScheduleDTO machineDTO = context.getMachineScheduleMap().get(machineCode);
        if (machineDTO == null || !machineDTO.isHasMaintenancePlan()) {
            return null;
        }
        return maintenanceScheduleService.resolveMaintenanceResumeProductionTime(context, machineDTO, baseReadyTime);
    }

}
