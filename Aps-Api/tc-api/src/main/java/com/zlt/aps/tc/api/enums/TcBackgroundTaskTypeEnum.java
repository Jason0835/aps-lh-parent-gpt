package com.zlt.aps.tc.api.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎侧后台任务类型。
 */
@Getter
public enum TcBackgroundTaskTypeEnum {

    /** 自动排程。 */
    AUTO_PLAN("AUTO_PLAN", false),

    /** 排程发布。 */
    RELEASE("RELEASE", false),

    /** 自动滚动更新。 */
    AUTO_ROLLING("AUTO_ROLLING", false),

    /** 人工插单。 */
    MANUAL_INSERT("MANUAL_INSERT", true),

    /** 人工调量。 */
    MANUAL_CHANGE_QTY("MANUAL_CHANGE_QTY", true),

    /** 人工单条或批量转机台。 */
    MANUAL_CHANGE_MACHINE("MANUAL_CHANGE_MACHINE", true),

    /** 人工删除。 */
    MANUAL_DELETE("MANUAL_DELETE", true);

    /** 类型编码。 */
    private final String code;

    /** 是否属于人工写操作。 */
    private final boolean manualOperation;

    TcBackgroundTaskTypeEnum(String code, boolean manualOperation) {
        this.code = code;
        this.manualOperation = manualOperation;
    }

    /**
     * 获取全部人工操作任务类型编码。
     *
     * @return 人工任务类型编码
     */
    public static List<String> manualOperationCodes() {
        return Arrays.stream(values()).filter(TcBackgroundTaskTypeEnum::isManualOperation)
                .map(TcBackgroundTaskTypeEnum::getCode).collect(Collectors.toList());
    }
}
