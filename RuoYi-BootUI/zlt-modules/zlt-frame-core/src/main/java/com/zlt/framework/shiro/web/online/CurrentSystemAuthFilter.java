package com.zlt.framework.shiro.web.online;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.framework.GlobalSetting;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import io.buji.pac4j.subject.Pac4jPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.AntPathMatcher;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 当前系统的权限的验证
 */
@Slf4j
public class CurrentSystemAuthFilter extends AccessControlFilter {

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    GlobalSetting globalSetting;

    AntPathMatcher matcher = new AntPathMatcher();

    List<String> anonPath = new ArrayList<>();

    public void setAnonPath(List<String> anonPath) {
        this.anonPath = anonPath;
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest servletRequest, ServletResponse servletResponse, Object o) throws Exception {

        Subject subject = getSubject(servletRequest, servletResponse);

        //没有登录的时候都放过
        if (subject == null || subject.getSession() == null
                || subject.getPrincipal() == null
        ) {
            return true;
        }

        return checkUserSystemAuth(subject);
    }

    /**
     * 看看有没有在设定的目录要排除的，有就返回false，终端循环
     *
     * @param pattern
     * @param path
     * @return
     */
    @Override
    protected boolean pathsMatch(String pattern, String path) {

        boolean isMatch = false;
        for (String item : anonPath) {
            isMatch = this.matcher.matches(item, path);
            if (isMatch) {
                break;
            }
        }

        return !isMatch;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        WebUtils.issueRedirect(req, res, "/unauth");

        return false;
    }

    /**
     * 单例模式取系统代码，重启生效
     *
     * @return
     */
    private String getSystemCode() {
        return globalSetting.getSysCode();
    }

    private Boolean checkUserSystemAuth(Subject subject) {
        Set<String> userSysCode = null;
        userSysCode = Pac4jRealmUtils.getSystemCode((Pac4jPrincipal) subject.getPrincipal());
        String syscode = getSystemCode();

        if (!userSysCode.contains(syscode)) {
            log.error(I18nUtil.getMessage("ui.biz.alter.userAuth.noSysCode"));
            return false;
        }

        return true;
    }
}
