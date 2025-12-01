package com.zlt.aps.monthplan.api.enums;

/**
 * 月计划调整通知单状态枚举类
 *
 * @author ZLT
 * @date 20250521
 */
public enum MonthPlanAdjustNoticeStatusEnum {
    /**
     * 1 创建
     */
    NEW(1, "创建"),
    /**
     * 2 提交
     */
    SUBMIT(2, "提交"),
    /**
     * 3 确认调整
     */
    CONFIRM(3, "确认调整"),
    /**
     * 4 作废
     */
    CANCEL(4, "作废");

    private Integer status;

    private String desc;

    MonthPlanAdjustNoticeStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}
