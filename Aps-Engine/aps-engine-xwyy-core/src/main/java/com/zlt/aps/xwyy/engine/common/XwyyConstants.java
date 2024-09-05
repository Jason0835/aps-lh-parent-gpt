package com.zlt.aps.xwyy.engine.common;

/**
 * 纤维压延常量
 * @Description
 * @Author zlt
 * @Date 2022-3-18 15:16:28
 */
public class XwyyConstants {
    /**
     * 额外计划量标识：无额外计划
     */
    public static final String EXTRA_PLAN_FLAG_NO = "0";

    /**
     * 额外计划量标识：中班额外计划
     */
    public static final String EXTRA_PLAN_FLAG_DAY = "1";

    /**
     * 额外计划量标识：夜班有额外计划
     */
    public static final String EXTRA_PLAN_FLAG_NIGHT = "2";

	/**
	 * 大卷提醒标识：不提醒
	 */
    public final static String REMAIND_FLAG_NO = "0";

	/**
	 * 大卷提醒标识：不提醒
	 */
    public final static String REMAIND_FLAG_YES = "1";
	
	/**
	 * 大卷提醒标识：不提醒
	 */
    public final static String ORIGINAL_REMIND_FLAG_NO = "0";
	
	/**
	 * 大卷提醒标识：需提醒
	 */
    public final static String ORIGINAL_REMIND_FLAG_YES = "1";
}
