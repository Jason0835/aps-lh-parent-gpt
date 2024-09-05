package com.ruoyi.system.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.system.service.ISysUserOnlineService;

/**
 * 在线用户监控
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/online")
public class SysUserOnlineController extends BaseController
{
    @Autowired
    private ISysUserOnlineService userOnlineService;

    @Autowired
    private RedisService redisService;

    @PreAuthorize(hasPermi = "monitor:online:list")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName)
    {
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        for (String key : keys)
        {
            LoginUser user = redisService.getCacheObject(key);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName))
            {
                if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
                }
            }
            else if (StringUtils.isNotEmpty(ipaddr))
            {
                if (StringUtils.equals(ipaddr, user.getIpaddr()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
                }
            }
            else if (StringUtils.isNotEmpty(userName))
            {
                if (StringUtils.equals(userName, user.getUsername()))
                {
                    userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
                }
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户by token
     */
    @PreAuthorize(hasPermi = "monitor:online:forceLogout")
    @Log(title = "system.title.onlineuser", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        userOnlineService.cleanUserCache(tokenId);
        return AjaxResult.success();
    }

    /**
     * 强退用户by ids
     */
    @PreAuthorize(hasPermi = "monitor:online:forceLogout")
    @Log(title = "system.title.onlineuser", businessType = BusinessType.FORCE)
    @DeleteMapping("/logout/{ids}")
    public AjaxResult forceLogoutByIds(@PathVariable("ids") String ids)
    {
        userOnlineService.logoutByIds(ids);
        return AjaxResult.success();
    }

    /***
     * 内部使用 在remote接口
     * @param tokenId
     * @return
     */
    @DeleteMapping("/cleanToken/{tokenId}")
    public AjaxResult cleanToken(@PathVariable("tokenId") String tokenId){
        userOnlineService.cleanUserCache(tokenId);
        return AjaxResult.success();
    }

    /***
     * loginuser转为onlineUser 用来给前端Session
     * @param user
     * @return
     */
    @PostMapping("/loginUserToUserOnline")
    public SysUserOnline loginUserToUserOnline(LoginUser user){
        return userOnlineService.loginUserToUserOnline(user);
    }
}
