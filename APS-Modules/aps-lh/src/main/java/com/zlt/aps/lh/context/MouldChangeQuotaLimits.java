package com.zlt.aps.lh.context;

import lombok.Value;

/** 换模与换活字块共用的只读早班、中班、每日限额或统计次数。 */
@Value
public class MouldChangeQuotaLimits {
    /** 早班限额或次数。 */
    int morningLimit;
    /** 中班限额或次数。 */
    int afternoonLimit;
    /** 每日限额或次数，独立于早中班有效限额之和。 */
    int dailyLimit;
}
