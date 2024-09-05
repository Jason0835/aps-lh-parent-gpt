package com.ruoyi.common.core.utils;

import javax.servlet.http.HttpServletRequest;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Locale;

/**
 * 权限获取工具类
 * 
 * @author ruoyi
 */
public class SecurityUtils
{
    /**
     * 获取用户
     */
    public static String getUsername()
    {
        String username = ServletUtils.getRequest().getHeader(CacheConstants.DETAILS_USERNAME);
        return StringUtils.isBlank(username) ? "" : ServletUtils.urlDecode(username);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId()
    {
        String userId = ServletUtils.getRequest().getHeader(CacheConstants.DETAILS_USER_ID);
        return StringUtils.isBlank(userId) ? null : Convert.toLong(userId);
    }

    /**
     * 获取请求token
     */
    public static String getToken()
    {
        return getToken(ServletUtils.getRequest());
    }

    /**
     * 根据request获取请求token
     */
    public static String getToken(HttpServletRequest request)
    {
        String token = ServletUtils.getRequest().getHeader(CacheConstants.HEADER);
        if (StringUtils.isNotEmpty(token) && token.startsWith(CacheConstants.TOKEN_PREFIX))
        {
            token = token.replace(CacheConstants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 是否为管理员
     * 
     * @param userId 用户ID
     * @return 结果
     */
    public static boolean isAdmin(Long userId)
    {
        return userId != null && 1L == userId;
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }

    /**
     * 判断密码是否相同
     *
     * @param rawPassword 真实密码
     * @param encodedPassword 加密后字符
     * @return 结果
     */
    public static boolean matchesPassword(String rawPassword, String encodedPassword)
    {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 获取用户语言
     */
    public static Locale getUserLang() {
        return ServletUtils.getUserLang();
    }


    /**
     * 获取用户当前选择的部门
     */
    public static String getUserCurrentFactory() {
        return ServletUtils.getRequest().getHeader(CacheConstants.TOKEN_FACTORY);
    }


}
