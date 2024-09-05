package com.ruoyi.system.service.impl;

import com.ruoyi.api.gateway.system.UserUtils;
import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.system.service.ISysUserOnlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * 在线用户 服务层处理
 *
 * @author ruoyi
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService {

    @Autowired
    private RedisService redisService;

    /**
     * 通过登录地址查询信息
     *
     * @param ipaddr 登录地址
     * @param user   用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user) {
        if (StringUtils.equals(ipaddr, user.getIpaddr())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     *
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user) {
        if (StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     *
     * @param ipaddr   登录地址
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user) {
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     *
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user) {
        Long lastTime = Long.parseLong(Convert.toStr(redisService.getCacheObject(CacheConstants.TOKEN_LAST_OPER_TIME + user.getUserid().toString())));
        return UserUtils.loginUserToUserOnline(user, lastTime, SecurityUtils.getUserLang());
    }

    @Override
    public void logoutByIds(String ids) {
        if(StringUtils.isEmpty(ids)){
            return;
        }

        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        HashMap<String, List<LoginUser>> users = new HashMap();
        String[] idArray = ids.split(",");
        List<String> idList = Arrays.asList(idArray);

        if (keys != null) {
            for (String key : keys) {
                LoginUser user = redisService.getCacheObject(key);
                if(idList.contains(user.getSessionId())){
                    cleanUserCache(user.getToken());
                }
            }
        }
    }

    @Override
    public void cleanUserCache(String token) {
        if (StringUtils.isNotNull(token)) {
            redisService.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + token);
            redisService.deleteObject(Constants.LOCALE_SESSION_ATTRIBUTE_NAME + token);
            redisService.deleteObject(Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME + token);
        }
    }
}
