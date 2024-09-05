package com.zlt.framework.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.CacheUtils;
import com.ruoyi.common4ui.utils.ServletUtils;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import io.buji.pac4j.subject.Pac4jPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.pac4j.core.profile.CommonProfile;
import org.springframework.context.i18n.LocaleContextHolder;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * shiro 工具类
 *
 * @author ruoyi
 */
@Slf4j
public class AuthorizationUtils {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static Subject getSubject() {
        return SecurityUtils.getSubject();
    }

    public static Session getSession() {
        return SecurityUtils.getSubject().getSession();
    }

    /***
     * 从session里面取出cloud的accessToken
     * @return
     */
    public static String getAccessToken() {
        String token = null;
        try {
            Object obj = AuthorizationUtils.getSession().getAttribute(GatewayConstants.UI_ACCESS_TOKEN);
            if (StringUtils.isNotNull(obj)) {
                token = obj.toString();
            }
        } catch (Throwable e) {
            log.error("session 不存在：" + e.getMessage());
        }
        return token;
    }

    public static void logout() {
        CacheUtils.remove(com.ruoyi.common4ui.constant.CacheConstants.SYSTEM_DATA_KEY_PREFIX + AuthorizationUtils.getSessionId());
        getSubject().logout();
    }

    public static Set<String> getSystemCode() {
        Set<String> result = null;
        result = Pac4jRealmUtils.getSystemCode((Pac4jPrincipal) getSubject().getPrincipal());
        return result;
    }

    public static List getUserDeptRoleL1(String sysCode) {

        // Object obj = getSubject().getPrincipal();
        Pac4jPrincipal principal = (Pac4jPrincipal) getSubject().getPrincipal();
        List result = null;
        if (StringUtils.isNotNull(principal)) {
            LoginUser lgu = new LoginUser();
            LoginUser loginUser = Pac4jRealmUtils.getLoginUserByPac4jPrincipal(principal);
            BeanUtils.copyBeanProp(lgu, loginUser);
            result = lgu.getSystemRoleDeptLevel1(sysCode);
        }
        if (StringUtils.isNull(result)) {
            result = new ArrayList<>();
        }
        return result;
    }

    public static SysUser getSysUser() {
        SysUser user = null;
        Subject subject = getSubject();
        Pac4jPrincipal principal = (Pac4jPrincipal) subject.getPrincipal();
        if (StringUtils.isNotNull(principal)) {
            LoginUser loginUser = Pac4jRealmUtils.getLoginUserByPac4jPrincipal(principal);
            if (StringUtils.isNotNull(loginUser) && loginUser.getSysUser() != null) {
                user = new SysUser();
                BeanUtils.copyBeanProp(user, loginUser.getSysUser());
            }
        }
        return user;
    }

    public static void setSysUser(SysUser user) {
        Subject subject = getSubject();
        PrincipalCollection principalCollection = subject.getPrincipals();
        String realmName = principalCollection.getRealmNames().iterator().next();
        Pac4jPrincipal principal = (Pac4jPrincipal) subject.getPrincipal();
        LoginUser loginUser = Pac4jRealmUtils.getLoginUserByPac4jPrincipal(principal);
        //LoginUser loginUser = (LoginUser) principalCollection.getPrimaryPrincipal();
        loginUser.setSysUser(user);
        CommonProfile profile = principal.getProfile();
        profile.addAttribute("loginUserJson", JSON.toJSONString(loginUser));
        PrincipalCollection newPrincipalCollection = new SimplePrincipalCollection(principal, realmName);
        // 重新加载Principal
        subject.runAs(newPrincipalCollection);
    }

    /**
     * callback返回时调用远程接口获取工厂权限信息
     *
     * @param remoteLoginUser
     */
    public static void setLoginUserUserDeptRoleL1(LoginUser remoteLoginUser) {
        Subject subject = getSubject();
        Pac4jRealmUtils.setLoginUserUserDeptRoleL1(remoteLoginUser, subject);
    }

//    public static void clearCachedAuthorizationInfo() {
//        RealmSecurityManager rsm = (RealmSecurityManager) SecurityUtils.getSecurityManager();
//        UserRealm realm = (UserRealm) rsm.getRealms().iterator().next();
//        realm.clearAllCachedAuthorizationInfo();
//    }

    public static Long getUserId() {
        return getSysUser().getUserId().longValue();
    }

    public static final String getLoginName() {
        try {
            return getSysUser().getUserName();
        } catch (Exception e) {
            return "";
        }
    }

    public static final String getDepartName() {
        try {
            return getSysUser().getDept().getDeptName();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIp() {
        return getSubject().getSession().getHost();
    }

    public static String getSessionId() {
        return String.valueOf(getSubject().getSession().getId());
    }

    /**
     * 生成随机盐
     */
    public static String randomSalt() {
        // 一个Byte占两个字节，此处生成的3字节，字符串长度为6
        SecureRandomNumberGenerator secureRandom = new SecureRandomNumberGenerator();
        String hex = secureRandom.nextBytes(3).toHex();
        return hex;
    }

    public static String getFactory() {
        String factoryCode = null;
        try {
            factoryCode = Convert.toStr(getSubject().getSession().getAttribute(com.ruoyi.common.constant.CacheConstants.TOKEN_FACTORY));
        } catch (Throwable t) {
            log.warn(I18nUtil.getMessage("ui.system.alter.sessionNoSelectFactory"), t);
        }
        return factoryCode;
    }

    /**
     * 拦截器加入lang以后，就会写入session
     *
     * @return
     */
    public static String getLang() {
        String lang = null;
        try {
            lang = Convert.toStr(getSubject().getSession().getAttribute(com.ruoyi.common.constant.CacheConstants.TOKEN_LANG));
        } catch (Throwable t) {
            log.error(I18nUtil.getMessage("ui.system.alter.sessionNoLang"), t);
        }
        if (StringUtils.isEmpty(lang)) {
            lang = LocaleContextHolder.getLocale().toString();
        }
        return lang;
    }

    public static boolean isAjaxResponse(ServletRequest request, ServletResponse response, String errorMsg,
                                         String redirectUrl
    ) throws IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        if (ServletUtils.isAjaxRequest(req)) {
            AjaxResult ajaxResult = AjaxResult.error(errorMsg);
            ServletUtils.renderString(res, objectMapper.writeValueAsString(ajaxResult));
            return true;
        } else {
            WebUtils.issueRedirect(request, response, redirectUrl);
        }
        return false;
    }


}
