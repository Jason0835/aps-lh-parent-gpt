package com.ruoyi.framework.shiro.realm;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import com.ruoyi.system.api.ISysLoginService;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.system.api.form.LoginBody;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.zlt.framework.GlobalSetting;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 自定义Realm 处理登录 权限
 *
 * @author ruoyi
 */
public class UserRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(UserRealm.class);

    @Autowired
    ISysLoginService iSysLoginService;

    @Autowired
    GlobalSetting globalSetting;

    @Autowired
    ISysUserService iSysUserService;

    /**
     * 授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection arg0) {

        SysUser user = AuthorizationUtils.getSysUser();
        // 角色列表
        Set<String> roles = new HashSet<String>();
        // 功能列表
        Set<String> menus = new HashSet<String>();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        // 管理员拥有所有权限
        if (user.isAdmin()) {
            info.addRole("admin");
            info.addStringPermission("*:*:*");
        } else {
            R<LoginUser> result = iSysLoginService.geteUser();
            if (result.getCode() != Constants.SUCCESS) {
                throw new AuthenticationException(result.getMsg());
            }
            LoginUser loginUser = result.getData();
            menus = loginUser.getSystemPermissions(globalSetting.getSysCode());
            roles = loginUser.getSystemRoles(globalSetting.getSysCode());
            // 角色加入AuthorizationInfo认证对象
            info.setRoles(roles);
            // 权限加入AuthorizationInfo认证对象
            info.setStringPermissions(menus);
        }
        return info;
    }

    /**
     * 登录认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {

        Pac4jToken upToken = (Pac4jToken) token;
        String sessionId = AuthorizationUtils.getSession().getId().toString();

        LoginBody body = new LoginBody();
        body.setUsername(Pac4jRealmUtils.getUsername(upToken));
        String password = null;
        if (Pac4jRealmUtils.getPassword(upToken) != null) {
            password = Pac4jRealmUtils.getPassword(upToken);
        }
        body.setPassword(password);
        body.setCode(Pac4jRealmUtils.getCode(upToken));
        body.setUuid(Pac4jRealmUtils.getUuid(upToken));
        body.setSessionId(sessionId);
        body.setCaptchaEnabled(Boolean.valueOf(Pac4jRealmUtils.getaptchaEnabled(upToken)));

        R<?> result = null;
        try {
            result = iSysLoginService.login(body);
            if (result.getCode() != R.SUCCESS) {
                throw new AuthenticationException(result.getMsg());
            }
        } catch (Exception e) {
            String str = StringUtils.format(I18nUtil.getMessage("ui.login.check.pwd.fail"), body.getUsername(), e.getMessage());
            log.error(str);
            throw new AuthenticationException(e.getMessage(), e);
        }

        String accessToken = ((Map) result.getData()).get(GatewayConstants.UI_ACCESS_TOKEN).toString();
        Pac4jRealmUtils.setToken(upToken, accessToken);
        //放到session里面，用来发送feign
        AuthorizationUtils.getSession().setAttribute(GatewayConstants.UI_ACCESS_TOKEN, accessToken);

        //这里的user没有权限信息。
        LoginUser user = iSysLoginService.geteUser().getData();

        AjaxResult ajaxResult = iSysUserService.getUserAuth(user.getSysUser().getUserId());
        if (!StringUtils.equals(String.valueOf(ajaxResult.get(Constants.CODE)), String.valueOf(HttpStatus.SUCCESS))) {
            throw new AuthenticationException(ajaxResult.get(GatewayConstants.MSG_TAG).toString());
        }

        List<CommonProfile> profiles=new ArrayList<>();
        Pac4jPrincipal principal = Pac4jRealmUtils.buildPac4jPrincipal(upToken,profiles,JSON.toJSONString(ajaxResult.get(Constants.DATA)));
        PrincipalCollection principalCollection = new SimplePrincipalCollection(principal, this.getName());
        return new SimpleAuthenticationInfo(principalCollection, profiles.hashCode());
       // return info;
    }

    /**
     * 清理指定用户授权信息缓存
     */
    public void clearCachedAuthorizationInfo(Object principal) {
        SimplePrincipalCollection principals = new SimplePrincipalCollection(principal, getName());
        this.clearCachedAuthorizationInfo(principals);
    }

    /**
     * 清理所有用户授权信息缓存
     */
    public void clearAllCachedAuthorizationInfo() {
        Cache<Object, AuthorizationInfo> cache = getAuthorizationCache();
        if (cache != null) {
            for (Object key : cache.keys()) {
                cache.remove(key);
            }
        }
    }

    @Override
    public Class getAuthenticationTokenClass() {
        return Pac4jToken.class;
    }
}
