package com.zlt.aps.lh.util;

import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.dto.MachineCleaningWindowDTO;
import com.zlt.aps.lh.api.domain.dto.MachineMaintenanceWindowDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmDevicePlanShut;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 排程结果停机摘要工具。
 *
 * <p>将结果实际命中的保养、停机、清洗窗口回填到结果主表，便于排查定位。</p>
 *
 * @author APS
 */
public final class ResultDowntimeSummaryUtil {

    private ResultDowntimeSummaryUtil() {
    }

    /**
     * 按结果实际排产时间段回填停机摘要。
     *
     * @param result 排程结果
     * @param maintenanceWindowList 保养窗口
     * @param cleaningWindowList 清洗窗口
     * @param devicePlanShutList 设备停机窗口
     */
    public static void fillDowntimeSummary(LhScheduleResult result,
                                           List<MachineMaintenanceWindowDTO> maintenanceWindowList,
                                           List<MachineCleaningWindowDTO> cleaningWindowList,
                                           List<MdmDevicePlanShut> devicePlanShutList) {
        clearDowntimeSummary(result);
        if (Objects.isNull(result)) {
            return;
        }
        Date productionStartTime = resolveFirstPlannedShiftStartTime(result);
        Date productionEndTime = result.getSpecEndTime();
        if (Objects.isNull(productionStartTime) || Objects.isNull(productionEndTime)
                || !productionEndTime.after(productionStartTime)) {
            return;
        }
        fillMaintenanceSummary(result, maintenanceWindowList, productionStartTime, productionEndTime);
        fillCleaningSummary(result, cleaningWindowList, productionStartTime, productionEndTime);
        fillShutdownSummary(result, devicePlanShutList, productionStartTime, productionEndTime);
    }

    /**
     * 清空停机摘要字段。
     *
     * @param result 排程结果
     */
    public static void clearDowntimeSummary(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return;
        }
        result.setMaintenanceStartTime(null);
        result.setMaintenanceEndTime(null);
        result.setShutdownStartTime(null);
        result.setShutdownEndTime(null);
        result.setCleaningStartTime(null);
        result.setCleaningEndTime(null);
    }

    /** 精度保养原因分析标识 */
    private static final String MAINTENANCE_ANALYSIS = "精度保养";

    private static void fillMaintenanceSummary(LhScheduleResult result,
                                               List<MachineMaintenanceWindowDTO> maintenanceWindowList,
                                               Date productionStartTime,
                                               Date productionEndTime) {
        if (CollectionUtils.isEmpty(maintenanceWindowList)) {
            return;
        }
        Date earliestStartTime = null;
        Date latestEndTime = null;
        for (MachineMaintenanceWindowDTO maintenanceWindow : maintenanceWindowList) {
            if (Objects.isNull(maintenanceWindow)
                    || Objects.isNull(maintenanceWindow.getMaintenanceStartTime())
                    || Objects.isNull(maintenanceWindow.getMaintenanceEndTime())
                    || !isWindowOverlap(maintenanceWindow.getMaintenanceStartTime(),
                    maintenanceWindow.getMaintenanceEndTime(), productionStartTime, productionEndTime)) {
                continue;
            }
            earliestStartTime = earlier(earliestStartTime, maintenanceWindow.getMaintenanceStartTime());
            latestEndTime = later(latestEndTime, maintenanceWindow.getMaintenanceEndTime());
            // 对与保养窗口重叠的班次写入原因分析
            applyMaintenanceShiftAnalysis(result, maintenanceWindow);
        }
        result.setMaintenanceStartTime(earliestStartTime);
        result.setMaintenanceEndTime(latestEndTime);
    }

    /**
     * 对与保养窗口时间重叠的班次写入原因分析。
     *
     * @param result 排程结果
     * @param maintenanceWindow 保养窗口
     */
    private static void applyMaintenanceShiftAnalysis(LhScheduleResult result,
                                                      MachineMaintenanceWindowDTO maintenanceWindow) {
        if (Objects.isNull(result) || Objects.isNull(maintenanceWindow)
                || Objects.isNull(maintenanceWindow.getMaintenanceStartTime())
                || Objects.isNull(maintenanceWindow.getMaintenanceEndTime())) {
            return;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            if (Objects.isNull(shiftPlanQty) || shiftPlanQty <= 0) {
                continue;
            }
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            Date shiftEndTime = ShiftFieldUtil.getShiftEndTime(result, shiftIndex);
            if (Objects.isNull(shiftStartTime) || Objects.isNull(shiftEndTime)) {
                continue;
            }
            if (isWindowOverlap(maintenanceWindow.getMaintenanceStartTime(),
                    maintenanceWindow.getMaintenanceEndTime(), shiftStartTime, shiftEndTime)) {
                // 保留已有原因分析，追加保养标识
                String existingAnalysis = ShiftFieldUtil.getShiftAnalysis(result, shiftIndex);
                if (Objects.isNull(existingAnalysis) || existingAnalysis.isEmpty()) {
                    ShiftFieldUtil.setShiftAnalysis(result, shiftIndex, MAINTENANCE_ANALYSIS);
                } else if (!existingAnalysis.contains(MAINTENANCE_ANALYSIS)) {
                    ShiftFieldUtil.setShiftAnalysis(result, shiftIndex, existingAnalysis + "+" + MAINTENANCE_ANALYSIS);
                }
            }
        }
    }

    private static void fillCleaningSummary(LhScheduleResult result,
                                            List<MachineCleaningWindowDTO> cleaningWindowList,
                                            Date productionStartTime,
                                            Date productionEndTime) {
        if (CollectionUtils.isEmpty(cleaningWindowList)) {
            return;
        }
        Date earliestStartTime = null;
        Date latestEndTime = null;
        for (MachineCleaningWindowDTO cleaningWindow : cleaningWindowList) {
            if (Objects.isNull(cleaningWindow)
                    || Objects.isNull(cleaningWindow.getCleanStartTime())
                    || Objects.isNull(cleaningWindow.getCleanEndTime())
                    || !isWindowOverlap(cleaningWindow.getCleanStartTime(),
                    cleaningWindow.getCleanEndTime(), productionStartTime, productionEndTime)) {
                continue;
            }
            earliestStartTime = earlier(earliestStartTime, cleaningWindow.getCleanStartTime());
            latestEndTime = later(latestEndTime, cleaningWindow.getCleanEndTime());
        }
        result.setCleaningStartTime(earliestStartTime);
        result.setCleaningEndTime(latestEndTime);
    }

    private static void fillShutdownSummary(LhScheduleResult result,
                                            List<MdmDevicePlanShut> devicePlanShutList,
                                            Date productionStartTime,
                                            Date productionEndTime) {
        if (CollectionUtils.isEmpty(devicePlanShutList)) {
            return;
        }
        Date earliestStartTime = null;
        Date latestEndTime = null;
        for (MdmDevicePlanShut planShut : devicePlanShutList) {
            if (Objects.isNull(planShut)
                    || Objects.isNull(planShut.getBeginDate())
                    || Objects.isNull(planShut.getEndDate())
                    || !isWindowOverlap(planShut.getBeginDate(),
                    planShut.getEndDate(), productionStartTime, productionEndTime)) {
                continue;
            }
            earliestStartTime = earlier(earliestStartTime, planShut.getBeginDate());
            latestEndTime = later(latestEndTime, planShut.getEndDate());
        }
        result.setShutdownStartTime(earliestStartTime);
        result.setShutdownEndTime(latestEndTime);
    }

    private static Date resolveFirstPlannedShiftStartTime(LhScheduleResult result) {
        if (Objects.isNull(result)) {
            return null;
        }
        for (int shiftIndex = 1; shiftIndex <= LhScheduleConstant.MAX_SHIFT_SLOT_COUNT; shiftIndex++) {
            Integer shiftPlanQty = ShiftFieldUtil.getShiftPlanQty(result, shiftIndex);
            Date shiftStartTime = ShiftFieldUtil.getShiftStartTime(result, shiftIndex);
            if (Objects.nonNull(shiftPlanQty) && shiftPlanQty > 0 && Objects.nonNull(shiftStartTime)) {
                return shiftStartTime;
            }
        }
        return null;
    }

    private static boolean isWindowOverlap(Date windowStartTime,
                                           Date windowEndTime,
                                           Date productionStartTime,
                                           Date productionEndTime) {
        return Objects.nonNull(windowStartTime)
                && Objects.nonNull(windowEndTime)
                && Objects.nonNull(productionStartTime)
                && Objects.nonNull(productionEndTime)
                && windowEndTime.after(windowStartTime)
                && productionEndTime.after(productionStartTime)
                && windowStartTime.before(productionEndTime)
                && windowEndTime.after(productionStartTime);
    }

    private static Date earlier(Date left, Date right) {
        if (Objects.isNull(left)) {
            return right;
        }
        if (Objects.isNull(right)) {
            return left;
        }
        return left.before(right) ? left : right;
    }

    private static Date later(Date left, Date right) {
        if (Objects.isNull(left)) {
            return right;
        }
        if (Objects.isNull(right)) {
            return left;
        }
        return left.after(right) ? left : right;
    }
}
