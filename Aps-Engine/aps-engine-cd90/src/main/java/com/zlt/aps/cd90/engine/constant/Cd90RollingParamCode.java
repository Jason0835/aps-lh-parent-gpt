package com.zlt.aps.cd90.engine.constant;

import java.util.Arrays;
import java.util.List;

/** 直裁定时滚动参数编码。 */
public final class Cd90RollingParamCode {

    public static final String EARLY_MINUTES = "SYS0701036";
    public static final String LATE_MINUTES = "SYS0701037";
    public static final String STABLE_MINUTES = "SYS0701038";
    public static final List<String> ALL_CODES = Arrays.asList(
            EARLY_MINUTES, LATE_MINUTES, STABLE_MINUTES);

    private Cd90RollingParamCode() {
    }
}
