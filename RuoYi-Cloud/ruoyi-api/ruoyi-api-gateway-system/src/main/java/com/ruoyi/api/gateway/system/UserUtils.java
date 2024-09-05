package com.ruoyi.api.gateway.system;

import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.api.gateway.system.model.LoginUser;

import java.util.Locale;

public class UserUtils {

    public static SysUserOnline loginUserToUserOnline(LoginUser user, Long lastTime, Locale locale)
    {
        if (StringUtils.isNull(user))
        {
            return null;
        }
        SysUserOnline sysUserOnline = new SysUserOnline();
        sysUserOnline.setTokenId(user.getToken());
        sysUserOnline.setUserName(user.getUsername());
        sysUserOnline.setIpaddr(user.getIpaddr());
        sysUserOnline.setLoginTime(user.getLoginTime());
        sysUserOnline.setSessionId(user.getSessionId());
        sysUserOnline.setLastAccessTime(lastTime);
        SysDept dept = user.getSysUser().getDept();
        if(StringUtils.isNotNull(dept)){
            sysUserOnline.setDeptName(StringUtils.getLocaleName(dept.getLangJson(),locale,dept.getDeptName()));
        }
        return sysUserOnline;
    }
}
