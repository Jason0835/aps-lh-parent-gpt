package com.zlt.framework.shiro.web.filter;

import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.CacheConstants;
import com.ruoyi.common4ui.utils.CacheUtils;
import com.ruoyi.common4ui.utils.spring.SpringUtils4BootUI;
import com.zlt.framework.GlobalSetting;
import com.zlt.framework.utils.AuthorizationUtils;
import com.ruoyi.system.api.ISysLoginService;
import lombok.Getter;
import lombok.Setter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;


/**
 * 退出过滤器
 *
 * @author ruoyi
 */
@Getter
@Setter
public class LogoutFilter extends io.buji.pac4j.filter.LogoutFilter {
    private static final Logger log = LoggerFactory.getLogger(LogoutFilter.class);

    /**
     * 退出后重定向的地址
     */
    private String loginUrl;

    /**
     * 是否启用CAS
     */
    private boolean casEnable;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        String logoutUrl = getLogoutUrlPattern();
        try {
            Subject subject = SecurityUtils.getSubject();
            try {
                SysUser user = AuthorizationUtils.getSysUser();
                //TODO:本地清理缓存问题。登出已经转到CAS的时候。本地单机版的时候，两种情况兼容
                if (StringUtils.isNotNull(user)) {
                    // 清理远程缓存
                    SpringUtils4BootUI.getBean(ISysLoginService.class).logout();
                    // 清理本地缓存
                    CacheUtils.remove(CacheConstants.SYSTEM_DATA_KEY_PREFIX +  AuthorizationUtils.getSessionId());
                    //清理用户权限缓存
                    SpringUtils4BootUI.getBean(GlobalSetting.class).removeKey(CacheConstants.RELOAD_LOGIN_USER_PREFIX+AuthorizationUtils.getUserId());
                }
                // 退出登录
                subject.logout();
            } catch (SessionException ise) {
                log.error(I18nUtil.getMessage("ui.login.logout.fail"), ise);
            }
//            issueRedirect(request, response, redirectUrl);
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("ui.login.logout.session.fail"), e);
        }
        if(casEnable){
            WebUtils.issueRedirect(servletRequest,servletResponse,logoutUrl);
        }else{
            super.doFilter(servletRequest, servletResponse, filterChain);
        }

       // super.doFilter(servletRequest, servletResponse, filterChain);

    }

//    @Override
//    protected boolean preHandle(ServletRequest request, ServletResponse response) throws Exception {
//        try {
//            Subject subject = getSubject(request, response);
//            String redirectUrl = getRedirectUrl(request, response, subject);
//            try {
//                SysUser user = AuthorizationUtils.getSysUser();
//                if (StringUtils.isNotNull(user)) {
//                    // 清理远程缓存
//                    SpringUtils4BootUI.getBean(ISysLoginService.class).logout();
//                    // 清理本地缓存
//                    CacheUtils.remove(CacheConstants.SYSTEM_DATA_KEY_PREFIX +  AuthorizationUtils.getSessionId());
//                }
//                // 退出登录
//                subject.logout();
//            } catch (SessionException ise) {
//                log.error(I18nUtil.getMessage("ui.login.logout.fail"), ise);
//            }
//            issueRedirect(request, response, redirectUrl);
//        } catch (Exception e) {
//            log.error(I18nUtil.getMessage("ui.login.logout.session.fail"), e);
//        }
//        return false;
//    }
//
//    /**
//     * 退出跳转URL
//     */
//    @Override
//    protected String getRedirectUrl(ServletRequest request, ServletResponse response, Subject subject) {
//        String url = getLoginUrl();
//        if (StringUtils.isNotEmpty(url)) {
//            return url;
//        }
//        return super.getRedirectUrl(request, response, subject);
//    }
}
