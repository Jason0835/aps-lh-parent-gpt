package com.zlt.aps.lh.service.impl;

import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.util.ShiftCapacityResolverUtil;
import com.zlt.aps.lh.util.ShiftFieldUtil;
import com.zlt.aps.lh.api.domain.dto.MachineFaultWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.enums.MachineStopTypeEnum;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 临时故障窗口服务。仅登记原始禁产时间并适配公共产能算法，不调用其他业务调度。
 */
@Slf4j
@Service
public class LhTemporaryFaultService {

    /** 仅供公共时间运算识别的故障窗口类型，不表示精度保养。 */
    public static final String FAULT_CAPACITY_WINDOW_TYPE = "06_FAULT_CAPACITY";

    /**
     * 登记故障原始时间；未来故障不会修改机台就绪时间或维修标记。
     * @param context 排程上下文
     * @param plan 原始06计划，已通过公共有效性校验
     */
    public void registerFault(LhScheduleContext context, MdmDevicePlanShut plan) {
        MachineFaultWindowDTO window = new MachineFaultWindowDTO();
        window.setPlanId(plan.getId());
        window.setMachineCode(plan.getMachineCode());
        window.setStartTime(new Date(plan.getBeginDate().getTime()));
        window.setEndTime(new Date(plan.getEndDate().getTime()));
        context.getTemporaryFaultWindowMap().put(plan.getId(), window);
    }

    /**
     * 将故障只读适配为公共产能区间；不得把返回列表挂入机台精度窗口。
     * @param context 排程上下文
     * @param machineCode 当前运行态机台编码，L/R按物理机台命中
     * @return 原始故障开始至结束的禁产区间，无预热
     */
    public static List<MachineMaintenanceWindowDTO> resolveCapacityWindows(
            LhScheduleContext context, String machineCode) {
        List<MachineMaintenanceWindowDTO> windows = new ArrayList<>(4);
        if (Objects.isNull(context) || StringUtils.isEmpty(machineCode)) {
            return windows;
        }
        String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(machineCode);
        for (MachineFaultWindowDTO fault : context.getTemporaryFaultWindowMap().values()) {
            if (!StringUtils.equals(physicalCode,
                    LhSingleControlMachineUtil.resolvePhysicalMachineCode(fault.getMachineCode()))) {
                continue;
            }
            MachineMaintenanceWindowDTO window = new MachineMaintenanceWindowDTO();
            window.setMachineCode(machineCode);
            window.setMaintenanceType(FAULT_CAPACITY_WINDOW_TYPE);
            window.setSourcePlanDate(LhScheduleTimeUtil.clearTime(fault.getStartTime()));
            window.setMaintenanceStartTime(fault.getStartTime());
            window.setMaintenanceEndTime(fault.getEndTime());
            window.setProductionResumeTime(fault.getEndTime());
            window.setTriggerReason(MachineStopTypeEnum.TEMPORARY_FAULT.getCode() + "临时故障仅扣时间产能");
            windows.add(window);
        }
        return windows;
    }
    /**
     * 保存前只读核对故障全覆盖区间没有计划量，不在此处回裁数量或补偿排程。
     * @param context 最终排程上下文
     */
    public static void validateProductionWindows(LhScheduleContext context) {
        if (context.getTemporaryFaultWindowMap().isEmpty()) {
            return;
        }
        for (LhScheduleResult result : context.getScheduleResultList()) {
            if (Objects.isNull(result)) {
                continue;
            }
            for (LhShiftConfigVO shift : LhScheduleTimeUtil.getScheduleShifts(context, context.getScheduleDate())) {
                if (Objects.isNull(shift) || Objects.isNull(shift.getShiftIndex())) {
                    continue;
                }
                int index = shift.getShiftIndex();
                Integer quantity = ShiftFieldUtil.getShiftPlanQty(result, index);
                if (Objects.isNull(quantity) || quantity <= 0) {
                    continue;
                }
                Date start = ShiftFieldUtil.getShiftStartTime(result, index);
                Date end = ShiftFieldUtil.getShiftEndTime(result, index);
                if (Objects.nonNull(start) && Objects.nonNull(end) && start.before(end)
                        && ShiftCapacityResolverUtil.resolveFaultFreeSeconds(context, result.getLhMachineCode(), start, end) == 0L) {
                    log.warn("故障禁产校验发现区间内仍有计划量，仅记录不阻断排程, 批次: {}, 机台: {}, 班次: {}, 开始: {}, 结束: {}, 计划量: {}",
                            context.getBatchNo(), result.getLhMachineCode(), index, start, end, quantity);
                }
            }
        }
    }

}
