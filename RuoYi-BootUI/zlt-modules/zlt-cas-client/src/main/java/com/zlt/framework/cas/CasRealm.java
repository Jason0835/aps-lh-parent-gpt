package com.zlt.framework.cas;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.ISysLoginService;
import com.zlt.framework.GlobalSetting;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * 认证与授权
 * @author gongtao
 * @version 2018-03-30 13:55
 **/
@Slf4j
public class CasRealm extends Pac4jRealm {

    @Autowired
    ISysLoginService iSysLoginService;

    @Autowired
    GlobalSetting globalSetting;

    @Autowired
    ISysUserService iSysUserService;
//TODO:重写接收类对象
    /**
     * 认证
     * @param authenticationToken
     * @return
     * @throws AuthenticationException
     */
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
       /* final Pac4jToken pac4jToken = (Pac4jToken) authenticationToken;
        String accessToken=Pac4jRealmUtils.getParam(pac4jToken,"token");
        Pac4jRealmUtils.setToken(pac4jToken, accessToken);
        //放到session里面，用来发送feign
        AuthorizationUtils.getSession().setAttribute(GatewayConstants.UI_ACCESS_TOKEN, accessToken);
        //这里的user没有权限信息。
        String loginUserJson=Pac4jRealmUtils.getParam(pac4jToken,"loginUserJson");
        LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(loginUser, loginUser.getToken(), getName());

        return info;*/

        Pac4jToken token = (Pac4jToken)authenticationToken;
        String accessToken= Pac4jRealmUtils.getParam(token,"token");
        Pac4jRealmUtils.setToken(token, accessToken);
        //放到session里面，用来发送feign
        AuthorizationUtils.getSession().setAttribute(GatewayConstants.UI_ACCESS_TOKEN, accessToken);
        List<CommonProfile> profiles = token.getProfiles();
        Pac4jPrincipal principal = new Pac4jPrincipal(profiles, getPrincipalNameAttribute());
        String lang= Pac4jRealmUtils.getParam(token, CacheConstants.TOKEN_LANG);
        AuthorizationUtils.getSession().setAttribute(CacheConstants.TOKEN_LANG, lang);//将CAS会话传递过来的语言包进行缓存到业务session
        PrincipalCollection principalCollection = new SimplePrincipalCollection(principal, this.getName());
        return new SimpleAuthenticationInfo(principalCollection, profiles.hashCode());

    }

    /**
     * 授权/验权（todo 后续有权限在此增加）
     * @param principals
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        SysUser user = AuthorizationUtils.getSysUser();

        //Joran 2020-12-28获取后台权限start
        Long userId= AuthorizationUtils.getUserId();
        AjaxResult ajaxResult = iSysUserService.getUserAuth(userId);
        if (com.ruoyi.common.utils.StringUtils.equals(String.valueOf(ajaxResult.get(Constants.CODE)), String.valueOf(HttpStatus.SUCCESS))) {
            LoginUser remoteLoginUser= JSON.parseObject(JSON.toJSONString(ajaxResult.get(Constants.DATA)), LoginUser.class);
            AuthorizationUtils.setLoginUserUserDeptRoleL1(remoteLoginUser);
        }
        //Joran 2020-12-28获取后台权限end

//        LoginUser loginUser = JSON.parseObject(JSON.toJSONString(ajaxResult.get(Constants.DATA)), LoginUser.class);

        // 角色列表
        Set<String> roles = new HashSet<String>();
        // 功能列表
        Set<String> menus = new HashSet<String>();

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

//    @Override
//    protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
//        Pac4jPrincipal pac4jPrincipal = (Pac4jPrincipal) principals.getPrimaryPrincipal();
//        return pac4jPrincipal.getProfile().getId();
//    }
//
//    @Override
//    protected Object getAuthenticationCacheKey(AuthenticationToken token) {
//        if (token instanceof Pac4jToken) {
//            Pac4jToken pac4jToken = (Pac4jToken) token;
//            Object principal = pac4jToken.getPrincipal();
//            if (principal instanceof Optional) {
//                @SuppressWarnings("unchecked") Optional<CasProfile> casProfileOptional = (Optional<CasProfile>) principal;
//                return casProfileOptional.get().getId();
//            }
//        }
//        return super.getAuthenticationCacheKey(token);
//    }



}
