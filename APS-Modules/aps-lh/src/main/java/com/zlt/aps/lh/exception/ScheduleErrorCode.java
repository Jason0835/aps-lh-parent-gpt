package com.zlt.aps.lh.exception;

import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 排程错误码枚举
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum ScheduleErrorCode {

    // ==================== 通用错误 ====================
    UNKNOWN("S9999", "ui.data.column.lhScheduleResult.errorCode.unknown"),
    SYSTEM_ERROR("S0001", "ui.data.column.lhScheduleResult.errorCode.systemError"),
    /** 排程类型对应的排产策略未注册 */
    PRODUCTION_STRATEGY_NOT_REGISTERED("S0002", "ui.data.column.lhScheduleResult.errorCode.strategyNotRegistered"),
    /** 数据校验链配置不一致（如校验组内策略冲突） */
    VALIDATION_CHAIN_CONFIG_ERROR("S0003", "ui.data.column.lhScheduleResult.errorCode.validationChainConfigError"),

    // ==================== S4.1 前置校验错误 ====================
    MES_RELEASED("S4101", "ui.data.column.lhScheduleResult.errorCode.mesReleased"),
    SCHEDULE_IN_PROGRESS("S4102", "ui.data.column.lhScheduleResult.errorCode.scheduleInProgress"),
    BATCH_NO_GENERATE_FAILED("S4103", "ui.data.column.lhScheduleResult.errorCode.batchNoGenerateFailed"),
    CROSS_MONTH_SCHEDULE_UNSUPPORTED("S4104", "ui.data.column.lhScheduleResult.errorCode.crossMonthUnsupported"),

    // ==================== S4.2 数据初始化错误 ====================
    DATA_INCOMPLETE("S4201", "ui.data.column.lhScheduleResult.errorCode.dataIncomplete"),
    MACHINE_INFO_MISSING("S4202", "ui.data.column.lhScheduleResult.errorCode.machineInfoMissing"),
    MONTH_PLAN_MISSING("S4203", "ui.data.column.lhScheduleResult.errorCode.monthPlanMissing"),
    WORK_CALENDAR_MISSING("S4204", "ui.data.column.lhScheduleResult.errorCode.workCalendarMissing"),
    SKU_CAPACITY_MISSING("S4205", "ui.data.column.lhScheduleResult.errorCode.skuCapacityMissing"),
    MOULD_REL_MISSING("S4206", "ui.data.column.lhScheduleResult.errorCode.mouldRelMissing"),

    // ==================== S4.3 SKU归集错误 ====================
    NO_SKU_TO_SCHEDULE("S4301", "ui.data.column.lhScheduleResult.errorCode.noSkuToSchedule"),
    SURPLUS_CALCULATION_ERROR("S4302", "ui.data.column.lhScheduleResult.errorCode.surplusCalculationError"),

    // ==================== S4.4 续作排产错误 ====================
    CONTINUOUS_MACHINE_NOT_FOUND("S4401", "ui.data.column.lhScheduleResult.errorCode.continuousMachineNotFound"),
    SHIFT_ALLOCATION_FAILED("S4402", "ui.data.column.lhScheduleResult.errorCode.shiftAllocationFailed"),
    EMBRYO_STOCK_INSUFFICIENT("S4403", "ui.data.column.lhScheduleResult.errorCode.embryoStockInsufficient"),

    // ==================== S4.5 新增排产错误 ====================
    NO_MACHINE_AVAILABLE("S4501", "ui.data.column.lhScheduleResult.errorCode.noMachineAvailable"),
    MACHINE_SELECTION_FAILED("S4502", "ui.data.column.lhScheduleResult.errorCode.machineSelectionFailed"),
    MOULD_CHANGE_CAPACITY_EXCEEDED("S4503", "ui.data.column.lhScheduleResult.errorCode.mouldChangeCapacityExceeded"),
    MOULD_CHANGE_ALLOCATION_FAILED("S4504", "ui.data.column.lhScheduleResult.errorCode.mouldChangeAllocationFailed"),
    INSPECTION_ALLOCATION_FAILED("S4505", "ui.data.column.lhScheduleResult.errorCode.inspectionAllocationFailed"),
    NO_PRODUCTION_CAPACITY("S4506", "ui.data.column.lhScheduleResult.errorCode.noProductionCapacity"),

    // ==================== S4.6 结果校验错误 ====================
    RESULT_VALIDATION_FAILED("S4601", "ui.data.column.lhScheduleResult.errorCode.resultValidationFailed"),
    RESULT_SAVE_FAILED("S4602", "ui.data.column.lhScheduleResult.errorCode.resultSaveFailed"),
    MOULD_CHANGE_PLAN_FAILED("S4603", "ui.data.column.lhScheduleResult.errorCode.mouldChangePlanFailed");

    /** 错误码 */
    private final String code;

    /** 国际化key */
    private final String description;

    /**
     * 获取国际化后的错误描述
     *
     * @return 国际化错误描述
     */
    public String getLocalizedDescription() {
        return I18nUtil.getMessage(description);
    }

    /**
     * 根据错误码获取枚举
     *
     * @param code 错误码
     * @return 错误码枚举，未找到返回null
     */
    public static ScheduleErrorCode getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ScheduleErrorCode e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
