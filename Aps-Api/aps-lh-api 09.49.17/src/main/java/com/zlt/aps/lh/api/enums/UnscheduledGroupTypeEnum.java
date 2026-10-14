package com.zlt.aps.lh.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 硫化未排结果分类。
 *
 * @author APS
 */
@Getter
@AllArgsConstructor
public enum UnscheduledGroupTypeEnum {

    /** 正常开产日期位于本次T～T+2窗口内，或早于T且需求仍有效。 */
    IN_SCHEDULE_WINDOW(1, "排产窗口内"),
    /** 正常开产日期位于T+2之后，或属于其他不排说明。 */
    OTHER(2, "其他");

    /** 分类值。 */
    private final int value;
    /** 分类说明。 */
    private final String description;
}
