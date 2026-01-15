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
        /**
         * 日期格式
         */
        String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
        /**
         * 天字段前缀
         */
        String FIELD_PREFIX_DAY = "day";
        /**
         * 调整量字段前缀
         */
        String FIELD_PREFIX_ADJUST_QTY = "adjustQty";
        /**
         * 月最大天数
         */
        int MAX_DAY_OF_MONTH = 31;
        /**
         * 分隔符：逗号
         */
        String SPLIT_COMMA = ",";
    }



}
