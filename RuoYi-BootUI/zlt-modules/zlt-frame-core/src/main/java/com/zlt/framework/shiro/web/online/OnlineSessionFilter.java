package com.zlt.framework.shiro.web.online;

import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.common4ui.constant.ShiroConstants;
import com.ruoyi.common4ui.enums.OnlineStatus;
import com.zlt.framework.shiro.OnlineSession;
import com.zlt.framework.utils.AuthorizationUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * 自定义访问控制
 * 
 * @author ruoyi
 */
public class OnlineSessionFilter extends AccessControlFilter
{
    public OnlineSessionFilter(String loginUrl, EnterpriseCacheSessionDAO sessionDAO){
        this.loginUrl = loginUrl;
        this.sessionDAO = sessionDAO;
    }

    protected String loginUrl;
    protected EnterpriseCacheSessionDAO sessionDAO;

    /**
     * 表示是否允许访问；mappedValue就是[urls]配置中拦截器参数部分，如果允许访问返回true，否则false；
     */
    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        Subject subject = getSubject(request, response);
        if (subject == null || subject.getSession() == null)
        {
            return true;
        }
        Session session = sessionDAO.readSession(subject.getSession().getId());
        if (session instanceof OnlineSession)
        {
            OnlineSession onlineSession = (OnlineSession) session;
            request.setAttribute(ShiroConstants.ONLINE_SESSION, onlineSession);
            // 把user对象设置进去
            boolean isGuest = onlineSession.getUserId() == null || onlineSession.getUserId() == 0L;
            if (isGuest)
            {
                SysUser user = AuthorizationUtils.getSysUser();
                if (user != null)
                {
                    onlineSession.setUserId(user.getUserId());
                    onlineSession.setLoginName(user.getUserName());
					onlineSession.setAvatar(user.getAvatar());
                    onlineSession.setDeptName(user.getDept().getDeptName());
                    onlineSession.markAttributeChanged();
                }
            }

            if (onlineSession.getStatus() == OnlineStatus.off_line)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 表示当访问拒绝时是否已经处理了；如果返回true表示需要继续处理；如果返回false表示该拦截器实例已经处理了，将直接返回即可。
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception
    {
        Subject subject = getSubject(request, response);
        if (subject != null)
        {
            subject.logout();
        }
        saveRequestAndRedirectToLogin(request, response);
        return false;
    }

    // 跳转到登录页
    @Override
    protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException
    {
        WebUtils.issueRedirect(request, response, loginUrl);
    }
}
