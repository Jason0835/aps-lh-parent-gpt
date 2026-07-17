package com.zlt.aps.tc.api.enums;

import com.ruoyi.common.i18n.utils.I18nUtil;

/**
 * 胎侧排程业务错误码枚举。
 *
 * <p>用于集中维护胎侧自动排程、人工调整和落库过程中的用户可见业务错误。</p>
 */
public enum TcScheduleErrorCodeEnum {

    /** 自动排程并发执行 */
    TC_SCHEDULE_RUNNING("TC_SCHEDULE_RUNNING", "ui.tc.schedule.running", "当前工厂和日期正在自动排程，请稍后重试"),

    /** 插单位置非法 */
    TC_INSERT_POSITION_INVALID("TC_INSERT_POSITION_INVALID", "ui.tc.schedule.insert.positionInvalid", "当前机台班次只能插到第二个在产规格之后"),

    /** 排程上下文为空 */
    TC_CONTEXT_EMPTY("TC_CONTEXT_EMPTY", "ui.tc.schedule.contextEmpty", "胎侧排程上下文不能为空"),

    /** 排程日期为空 */
    TC_SCHEDULE_DATE_EMPTY("TC_SCHEDULE_DATE_EMPTY", "ui.tc.schedule.dateEmpty", "胎侧排程日期不能为空"),

    /** 策略未注册 */
    TC_STRATEGY_NOT_REGISTERED("TC_STRATEGY_NOT_REGISTERED", "ui.tc.schedule.strategyNotRegistered", "胎侧排程策略未注册"),

    /** 任务不存在 */
    TC_TASK_NOT_FOUND("TC_TASK_NOT_FOUND", "ui.tc.schedule.taskNotFound", "未找到胎侧排程任务"),

    /** 参数为空 */
    TC_PARAM_EMPTY("TC_PARAM_EMPTY", "ui.tc.schedule.paramEmpty", "胎侧排程参数不能为空"),

    /** 操作人为空 */
    TC_OPERATOR_EMPTY("TC_OPERATOR_EMPTY", "ui.tc.schedule.operatorEmpty", "胎侧排程操作人不能为空"),

    /** 班次非法 */
    TC_SHIFT_INVALID("TC_SHIFT_INVALID", "ui.tc.schedule.shiftInvalid", "胎侧排程班次不支持"),

    /** 机台候选为空 */
    TC_MACHINE_CANDIDATE_EMPTY("TC_MACHINE_CANDIDATE_EMPTY", "ui.tc.schedule.machineCandidateEmpty", "胎侧排程机台候选不能为空"),

    /** 库存预测参数非法 */
    TC_INVENTORY_PREDICT_INVALID("TC_INVENTORY_PREDICT_INVALID", "ui.tc.schedule.inventoryPredictInvalid", "胎侧库存预测参数非法");

    /** 错误码 */
    private final String code;

    /** 多语言消息键 */
    private final String messageKey;

    /** 默认提示 */
    private final String defaultMessage;

    /**
     * 构造胎侧排程错误码。
     *
     * @param code           错误码
     * @param messageKey     多语言消息键
     * @param defaultMessage 默认提示
     */
    TcScheduleErrorCodeEnum(String code, String messageKey, String defaultMessage) {
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
        return I18nUtil.getMessage(messageKey);
    }
}
