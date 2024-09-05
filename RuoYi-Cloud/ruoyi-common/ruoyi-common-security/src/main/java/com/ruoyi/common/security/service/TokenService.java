package com.ruoyi.common.security.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.utils.IdUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.api.gateway.system.model.LoginUser;

/**
 * token验证处理
 *
 * @author ruoyi
 */
@Component
public class TokenService {
    @Autowired
    private RedisService redisService;

    private final static long EXPIRE_TIME = Constants.TOKEN_EXPIRE * 60;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    protected static final long MILLIS_SECOND = 1000;

    /**
     * 创建令牌
     */
    public Map<String, Object> createToken(LoginUser loginUser) {
        // 生成token
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        loginUser.setUserid(loginUser.getSysUser().getUserId());
        loginUser.setUsername(loginUser.getSysUser().getUserName());
        loginUser.setIpaddr(IpUtils.getIpAddr(ServletUtils.getRequest()));
        refreshToken(loginUser);

        // 保存或更新用户token
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(GatewayConstants.UI_ACCESS_TOKEN, token);
        map.put("expires_in", EXPIRE_TIME);
        redisService.setCacheObject(ACCESS_TOKEN + token, loginUser, EXPIRE_TIME, TimeUnit.SECONDS);

        //20201021 linbn
        //token中写入语言包信息
        setRedisLocale(token);

        return map;
    }

    /**
     * token中写入语言包信息
     *
     * @param token
     * @author linbn 201021
     */
    private void setRedisLocale(String token) {

        String localeKey = Constants.LOCALE_SESSION_ATTRIBUTE_NAME + token;
        String timezoneKey = Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME + token;
        String locale = redisService.getCacheObject(localeKey);
        String timezone = redisService.getCacheObject(timezoneKey);

        if (StringUtils.isEmpty(locale)) {
            locale = Locale.getDefault().toString();
        }
        if (StringUtils.isEmpty(timezone)) {
            timezone = TimeZone.getDefault().toString();
        }

        redisService.setCacheObject(localeKey
                , locale, EXPIRE_TIME, TimeUnit.SECONDS);
        redisService.setCacheObject(timezoneKey
                , timezone, EXPIRE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser() {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = SecurityUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            LoginUser user = getLoginUserByToken(token);
            return user;
        }
        return null;
    }

    public LoginUser getLoginUserByToken(String token) {
        String userKey = getTokenKey(token);
        return redisService.getCacheObject(userKey);
    }


    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser) {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + EXPIRE_TIME * MILLIS_SECOND);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisService.setCacheObject(userKey, loginUser, EXPIRE_TIME, TimeUnit.SECONDS);
        //linbn 201021 刷新语言包
        setRedisLocale(loginUser.getToken());
    }

    private String getTokenKey(String token) {
        return ACCESS_TOKEN + token;
    }

    /**
     * 获取请求token
     */
    public static String getToken(HttpServletRequest request) {
        String token = TokenUtil.getToken(request);
        return token;
    }

}