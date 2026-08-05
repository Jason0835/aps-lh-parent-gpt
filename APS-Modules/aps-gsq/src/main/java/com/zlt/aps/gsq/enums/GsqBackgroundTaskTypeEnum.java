package com.zlt.aps.gsq.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钢丝圈后台任务类型
 *
 * <p>对齐胎侧 {@code TcBackgroundTaskTypeEnum}，区分自动滚动与4类人工操作异步任务，
 * 并通过 {@link #manualOperation} 标记识别人工写操作。</p>
 *
 * @author APS
 */
@Getter
public enum GsqBackgroundTaskTypeEnum {

    /** 自动滚动更新 */
    AUTO_ROLLING("AUTO_ROLLING", false),

    /** 人工插单 */
    MANUAL_INSERT("MANUAL_INSERT", true),

    /** 人工调量 */
    MANUAL_CHANGE_QTY("MANUAL_CHANGE_QTY", true),

    /** 人工转机台 */
    MANUAL_CHANGE_MACHINE("MANUAL_CHANGE_MACHINE", true),

    /** 人工删除 */
    MANUAL_DELETE("MANUAL_DELETE", true);

    /** 类型编码 */
    private final String code;

    /** 是否属于人工写操作 */
    private final boolean manualOperation;

    GsqBackgroundTaskTypeEnum(String code, boolean manualOperation) {
        this.code = code;
        this.manualOperation = manualOperation;
    }

    /**
     * 获取全部人工操作任务类型编码。
     *
     * <p>对齐胎侧 {@code TcBackgroundTaskTypeEnum.manualOperationCodes()}，
     * 用于查询最近人工操作任务（taskType IN manualOperationCodes）。</p>
     *
     * @return 人工任务类型编码集合
     */
    public static List<String> manualOperationCodes() {
        return Arrays.stream(values()).filter(GsqBackgroundTaskTypeEnum::isManualOperation)
                .map(GsqBackgroundTaskTypeEnum::getCode).collect(Collectors.toList());
    }
}
