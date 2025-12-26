package com.zlt.aps.common.core.constant;

/**
 * 业务常量类型接口
 * @author wengpc
 */
public interface BusiConstant {

    /**
     * 周程滚动调整
     */
    interface WeekRollAdjust {
        /**
         * 版本号前缀
         */
        String VERSION_PREFIX = "ADJ";
        /**
         * 锁定天数
         */
        int LOCK_DAYS = 3;
    }



}
