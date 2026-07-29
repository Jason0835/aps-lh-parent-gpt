package com.zlt.aps.tm.api.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎面后台任务类型。
 */
@Getter
public enum TmBackgroundTaskTypeEnum {

    /** 自动排程。 */
    AUTO_PLAN("AUTO_PLAN", false),

    /** 人工插单。 */
    MANUAL_INSERT("MANUAL_INSERT", true),

    /** 人工调量。 */
    MANUAL_CHANGE_QTY("MANUAL_CHANGE_QTY", true),

    /** 人工单条转机台。 */
    MANUAL_CHANGE_MACHINE("MANUAL_CHANGE_MACHINE", true),

    /** 人工批量转机台。 */
    MANUAL_BATCH_CHANGE_MACHINE("MANUAL_BATCH_CHANGE_MACHINE", true),

    /** 人工删除。 */
    MANUAL_DELETE("MANUAL_DELETE", true),

    /** 人工发布。 */
    MANUAL_PUBLISH("MANUAL_PUBLISH", true),

    /** 异步发布到MES。 */
    RELEASE("RELEASE", false);

    /** 类型编码。 */
    private final String code;

    /** 是否属于人工写操作。 */
    private final boolean manualOperation;

    TmBackgroundTaskTypeEnum(String code, boolean manualOperation) {
        this.code = code;
        this.manualOperation = manualOperation;
    }

    /**
     * 获取全部人工操作任务类型编码。
     *
     * @return 人工任务类型编码
     */
    public static List<String> manualOperationCodes() {
        return Arrays.stream(values()).filter(TmBackgroundTaskTypeEnum::isManualOperation)
                .map(TmBackgroundTaskTypeEnum::getCode).collect(Collectors.toList());
    }
}
