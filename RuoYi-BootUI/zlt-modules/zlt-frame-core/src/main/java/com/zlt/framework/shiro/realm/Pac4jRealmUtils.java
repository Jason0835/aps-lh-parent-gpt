package com.zlt.framework.shiro.realm;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.utils.StringUtils;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.pac4j.core.profile.CommonProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class Pac4jRealmUtils {

    public static Pac4jToken build(String username, String password, boolean rememberMe, String uuid, String code) {
        return Pac4jRealmUtils.build(username, password, rememberMe, uuid, code,"false");
    }

    public static Pac4jToken build(String username, String password, boolean rememberMe, String uuid, String code, String captchaEnabled) {
        List<CommonProfile> profiles = new ArrayList<>();
        CommonProfile oneProfile = new CommonProfile();
        Map attributes = new HashMap<String, Object>();
        oneProfile.build(username, attributes);
        profiles.add(oneProfile);

        oneProfile.addAttribute("username", username);
        oneProfile.addAttribute("password", password);
        oneProfile.setRemembered(rememberMe);
        oneProfile.addAttribute("uuid", uuid);
        oneProfile.addAttribute("captchaCode", code);
        oneProfile.addAttribute("captchaEnabled", captchaEnabled);  //是否开启登录验证码

        return new Pac4jToken(profiles, rememberMe);
    }

    public static void setToken(Pac4jToken token, String tokenStr) {
        token.getProfiles().get(0).addAttribute(CacheConstants.HEADER, tokenStr);
    }

    public static String getToken(Pac4jToken token) {
        return getParam(token, CacheConstants.HEADER);
    }

    public static String getPassword(Pac4jToken token) {
        return getParam(token, "password");
    }

    public static String getUuid(Pac4jToken token) {
        return getParam(token, "uuid");
    }

    public static String getCode(Pac4jToken token) {
        return getParam(token, "captchaCode");
    }

    public static String getUsername(Pac4jToken token) {
        return getParam(token, "username");
    }

    public static String getaptchaEnabled(Pac4jToken token) {
        return getParam(token, "captchaEnabled");
    }

    /**
     * 获取登录用户信息
     *
     * @param token
     * @return
     */
    public static LoginUser getLoginUser(Pac4jToken token) {
        String loginUserJson = getParam(token, "loginUserJson");
        LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
        return loginUser;
    }

    public static LoginUser getLoginUserByPac4jPrincipal(Pac4jPrincipal principal) {
        CommonProfile profile = principal.getProfile();
        String loginUserJson = (String) profile.getAttribute("loginUserJson");
        LoginUser loginUser = JSON.parseObject(loginUserJson, LoginUser.class);
        return loginUser;
    }

    public static String getParam(Pac4jToken token, String paramName) {
        String attribute = (String) token.getProfiles().get(0).getAttribute(paramName);
        return StringUtils.isEmpty(attribute) ? null : attribute;
    }

    /**
     * 创建Principal
     *
     * @param upToken
     * @param loginUserJson
     * @return
     */
    public static Pac4jPrincipal buildPac4jPrincipal(Pac4jToken upToken, List<CommonProfile> profiles, String loginUserJson) {
        CommonProfile profile = new CommonProfile();
        Map attributes = new HashMap<String, Object>();
        profile.build(Pac4jRealmUtils.getUsername(upToken), attributes);
        profile.addAttribute("username", Pac4jRealmUtils.getUsername(upToken));
        profile.addAttribute("password", Pac4jRealmUtils.getPassword(upToken));
        profile.addAttribute("uuid", Pac4jRealmUtils.getUuid(upToken));
        profile.addAttribute("captchaCode", Pac4jRealmUtils.getCode(upToken));
        profile.addAttribute("loginUserJson", loginUserJson);
        profiles.add(profile);
        Pac4jPrincipal principal = new Pac4jPrincipal(profiles);
        return principal;
    }

    public static Set<String> getSystemCode(Pac4jPrincipal principal) {
        Set<String> result = null;
        if (StringUtils.isNotNull(principal)) {
            LoginUser loginUser = getLoginUserByPac4jPrincipal(principal);
            result = loginUser.getSystemIds();
        }

        if (StringUtils.isNull(result)) {
            result = new HashSet<>();
        }
        return result;
    }

    /**
     * callback返回时调用远程接口获取工厂权限信息
     *
     * @param remoteLoginUser
     */
    public static void setLoginUserUserDeptRoleL1(LoginUser remoteLoginUser, Subject subject) {
        PrincipalCollection principalCollection = subject.getPrincipals();
        String realmName = principalCollection.getRealmNames().iterator().next();
        Pac4jPrincipal principal = (Pac4jPrincipal) subject.getPrincipal();
        LoginUser loginUser = Pac4jRealmUtils.getLoginUserByPac4jPrincipal(principal);
        loginUser.setRoleDeptLevel1(remoteLoginUser.getRoleDeptLevel1());
        CommonProfile profile = principal.getProfile();
        profile.addAttribute("loginUserJson", JSON.toJSONString(loginUser));
        PrincipalCollection newPrincipalCollection = new SimplePrincipalCollection(principal, realmName);
        // 重新加载Principal
        subject.runAs(newPrincipalCollection);
    }
}
