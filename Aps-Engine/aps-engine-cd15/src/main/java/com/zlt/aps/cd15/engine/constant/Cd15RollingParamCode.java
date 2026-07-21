package com.zlt.aps.cd15.engine.constant;

import java.util.Arrays;
import java.util.List;

/** 斜裁定时滚动参数编码。 */
public final class Cd15RollingParamCode {

    public static final String EARLY_MINUTES = "SYS0601036";
    public static final String LATE_MINUTES = "SYS0601037";
    public static final String STABLE_MINUTES = "SYS0601038";
    public static final List<String> ALL_CODES = Arrays.asList(
            EARLY_MINUTES, LATE_MINUTES, STABLE_MINUTES);

    private Cd15RollingParamCode() {
    }
}
