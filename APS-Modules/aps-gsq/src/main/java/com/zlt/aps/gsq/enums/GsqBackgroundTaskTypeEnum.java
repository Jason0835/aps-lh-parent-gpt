package com.zlt.aps.gsq.enums;

import lombok.Getter;

/**
 * 钢丝圈后台任务类型
 *
 * @author APS
 */
@Getter
public enum GsqBackgroundTaskTypeEnum {

    /** 自动滚动更新 */
    AUTO_ROLLING("AUTO_ROLLING");

    /** 类型编码 */
    private final String code;

    GsqBackgroundTaskTypeEnum(String code) {
        this.code = code;
    }
}
