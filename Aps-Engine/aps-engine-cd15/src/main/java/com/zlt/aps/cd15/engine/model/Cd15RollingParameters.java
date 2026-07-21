package com.zlt.aps.cd15.engine.model;

import lombok.Builder;
import lombok.Data;

/** 定时滚动窗口参数。 */
@Data
@Builder
public class Cd15RollingParameters {

    /** 交班前允许提前执行的分钟数。 */
    private int earlyMinutes;
    /** 交班后允许补偿执行的分钟数。 */
    private int lateMinutes;
    /** 输入版本保持不变的最少分钟数。 */
    private int stableMinutes;
}
