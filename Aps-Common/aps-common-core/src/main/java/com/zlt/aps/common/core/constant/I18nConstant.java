package com.zlt.aps.common.core.constant;

/**
 * 经常引用的国际化
 * key常量定义
 *
 * @author ZLT
 * @date 20251203
 */
public class I18nConstant {
    /**
     * 条件不可为空的国际化提示
     */
    public static final String CONDITION_NO_EMPTY = "ui.data.query.param.condition.noEmpty";
    /**
     * 分厂、年份、月份、需求计划版本不能为空 国际化提示
     */
    public static final String REQUIRE_VERSION_NO_EMPTY = "ui.data.param.factoryAndMonthAndRequireVersionNoEmpty";
    /**
     * 分厂、年份、月份、需求计划版本、排产版本不能为空 国际化提示
     */
    public static final String PRODUCTION_VERSION_NO_EMPTY = "ui.data.param.factoryAndMonthAndProductionVersionNoEmpty";

    /**
     * 中文
     */
    public static final String ZH_CN = "zh_CN";

    /**
     * 英文
     */
    public static final String EN_US = "en_US";

    /**
     * 越文
     */
    public static final String VI_VN = "vi_VN";

    /**
     * 页面JSON的Redis前缀
     */
    public static final String REDIS_PAGE_JSON = "lang:REDIS_PAGE_JSON:";

    /**
     * 目前页面仅单个文件，默认加载该配置文件
     */
    public static final String PAGE_FILE_NAME = "i18n/web";

    /**
     * 中英越国际化的标准存储格式
     */
    public static final String I18N_JSON = "[{\"zh_CN\":\"{}\",\"en_US\":\"{}\",\"vi_VN\":\"{}\"}]";

    /**
     * I18n配置文件所属的默认模块：未定义模块
     */
    public static final Long I18N_DEFAULT_MODE = 1L;

    /**
     * 修改国际化的锁
     */
    public static final String REDIS_I18N_CHANGE_LOCK = "lock:REDIS_I18N_CHANGE_LOCK";

    /**
     * 初始化国际化的锁
     */
    public static final String REDIS_I18N_INIT_LOCK = "lock:REDIS_I18N_INIT_LOCK";
}
