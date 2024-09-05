package com.ruoyi.common.constant;

/**
 * 缓存的key 常量
 *
 * @author ruoyi
 */
public class CacheConstants {
    /**
     * 令牌自定义标识
     */
    public static final String HEADER = "Authorization";

    /**
     * 令牌前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 权限缓存前缀
     */
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 用户ID字段
     */
    public static final String DETAILS_USER_ID = "user_id";

    /**
     * 用户名字段
     */
    public static final String DETAILS_USERNAME = "username";

    /**
     * 用户部门名字段
     */
    public static final String DETAILS_USERDEPTNAME = "userdept";

    /**
     * 用户最后操作时间
     */
    public static final String TOKEN_LAST_OPER_TIME = "lastAccessTime:";

    /**
     * 用户操作语言
     */
    public static final String TOKEN_LANG = "lang";
    /**
     * 用户选择的工厂
     */
    public static final String TOKEN_FACTORY = "dept";
    /**
     * 当前系统代号
     */
    public static final String TOKEN_SYS_CODE = "sysCode";
}
