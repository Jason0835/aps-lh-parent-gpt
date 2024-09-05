package com.zlt.framework.shiro.web;

import com.ruoyi.api.gateway.system.model.LoginUser;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;

/***
 * 验证证书方式，使用登录证书和shiro证书验证，替代用户名密码验证方法。
 * @author lbn
 */
public class RemoteCredentialsMatcher extends SimpleCredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {

        Pac4jToken pac4jToken = (Pac4jToken)token;
        String sToken = Pac4jRealmUtils.getToken(pac4jToken);
        Pac4jPrincipal principal =(Pac4jPrincipal)info.getPrincipals().getPrimaryPrincipal();
        LoginUser loginUser= Pac4jRealmUtils.getLoginUserByPac4jPrincipal(principal);
        return super.equals(sToken, loginUser.getToken());
    }

}
