package com.ruoyi.api.gateway.system.model;

import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.api.gateway.system.domain.SysUser;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 用户信息
 *
 * @author ruoyi
 */
@Getter
@Setter
public class LoginUser implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 用户名id
     */
    private Long userid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 权限列表
     */
    private HashMap<String, Set<String>> permissions = new HashMap<>();

    /**
     * 角色列表
     */
    private HashMap<String, Set<String>> roles = new HashMap<>();

    /***
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户信息
     */
    private SysUser sysUser;

    /**
     * 有权限的系统ID,这个数据是单独的服务提供，不能放system
     * 属于系统外部的授权
     */
    private Set<String> systemIds;

    /**
     * 默认为空，登录的时候补充数据
     * 用户角色对应的部门信息
     * 20201210 linbn
     */
    private HashMap<String, List<SysDept>> roleDeptLevel1 = new HashMap<>();

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public Long getUserid()
    {
        return userid;
    }

    public void setUserid(Long userid)
    {
        this.userid = userid;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public Long getLoginTime()
    {
        return loginTime;
    }

    public void setLoginTime(Long loginTime)
    {
        this.loginTime = loginTime;
    }

    public Long getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Long expireTime)
    {
        this.expireTime = expireTime;
    }

    public String getIpaddr()
    {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    public Set<String> getSystemPermissions(String sysCode)
    {
        return this.permissions.get(sysCode);
    }

    public void setSystemPermissions(String sysCode, Set<String> permissions)
    {
        this.permissions.put(sysCode, permissions);
    }

    public Set<String> getSystemRoles(String sysCode)
    {
        return roles.get(sysCode);
    }

    public void setSystemRoles(String sysCode , Set<String> roles)
    {
        this.roles.put(sysCode, roles);
    }

    public List<SysDept> getSystemRoleDeptLevel1(String sysCode)
    {
        return roleDeptLevel1.get(sysCode);
    }

    public void setSystemRoleDeptLevel1(String sysCode , List<SysDept> roleDeptLevel1)
    {
        this.roleDeptLevel1.put(sysCode, roleDeptLevel1);
    }

    public SysUser getSysUser()
    {
        return sysUser;
    }

    public void setSysUser(SysUser sysUser)
    {
        this.sysUser = sysUser;
    }
}
