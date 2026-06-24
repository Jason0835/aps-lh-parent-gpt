package com.zlt.aps.tm.api.enums;

/**
 * 胎面排程业务错误码枚举。
 *
 * <p>用于集中维护胎面自动排程、人工调整和落库过程中的用户可见业务错误。</p>
 */
public enum TmScheduleErrorCodeEnum {

    /** 自动排程并发执行 */
    TM_SCHEDULE_RUNNING("TM_SCHEDULE_RUNNING", "ui.data.alert.tm.schedule.running", "当前工厂和日期正在自动排程，请稍后重试"),

    /** 插单位置非法 */
    TM_INSERT_POSITION_INVALID("TM_INSERT_POSITION_INVALID", "ui.data.alert.tm.insert.positionInvalid", "当前机台班次只能插到第二个在产规格之后"),

    /** 排程上下文为空 */
    TM_CONTEXT_EMPTY("TM_CONTEXT_EMPTY", "ui.data.alert.tm.schedule.contextEmpty", "胎面排程上下文不能为空"),

    /** 排程日期为空 */
    TM_SCHEDULE_DATE_EMPTY("TM_SCHEDULE_DATE_EMPTY", "ui.data.alert.tm.schedule.dateEmpty", "胎面排程日期不能为空"),

    /** 策略未注册 */
    TM_STRATEGY_NOT_REGISTERED("TM_STRATEGY_NOT_REGISTERED", "ui.data.alert.tm.schedule.strategyNotRegistered", "胎面排程策略未注册"),

    /** 任务不存在 */
    TM_TASK_NOT_FOUND("TM_TASK_NOT_FOUND", "ui.data.alert.tm.schedule.taskNotFound", "未找到胎面排程任务"),

    /** 参数为空 */
    TM_PARAM_EMPTY("TM_PARAM_EMPTY", "ui.data.alert.tm.schedule.paramEmpty", "胎面排程参数不能为空"),

    /** 操作人为空 */
    TM_OPERATOR_EMPTY("TM_OPERATOR_EMPTY", "ui.data.alert.tm.schedule.operatorEmpty", "胎面排程操作人不能为空"),

    /** 班次非法 */
    TM_SHIFT_INVALID("TM_SHIFT_INVALID", "ui.data.alert.tm.schedule.shiftInvalid", "胎面排程班次不支持"),

    /** 机台候选为空 */
    TM_MACHINE_CANDIDATE_EMPTY("TM_MACHINE_CANDIDATE_EMPTY", "ui.data.alert.tm.schedule.machineCandidateEmpty", "胎面排程机台候选不能为空"),

    /** 库存预测参数非法 */
    TM_INVENTORY_PREDICT_INVALID("TM_INVENTORY_PREDICT_INVALID", "ui.data.alert.tm.schedule.inventoryPredictInvalid", "胎面库存预测参数非法");

    /** 错误码 */
    private final String code;

    /** 多语言消息键 */
    private final String messageKey;

    /** 默认提示 */
    private final String defaultMessage;

    /**
     * 构造胎面排程错误码。
     *
     * @param code           错误码
     * @param messageKey     多语言消息键
     * @param defaultMessage 默认提示
     */
    TmScheduleErrorCodeEnum(String code, String messageKey, String defaultMessage) {
        this.code = code;
        this.messageKey = messageKey;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取多语言消息键。
     *
     * @return 多语言消息键
     */
    public String getMessageKey() {
        return messageKey;
    }

    /**
     * 获取默认提示。
     *
     * @return 默认提示
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
