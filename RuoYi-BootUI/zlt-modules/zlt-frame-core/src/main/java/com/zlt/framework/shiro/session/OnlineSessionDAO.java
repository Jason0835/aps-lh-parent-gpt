package com.zlt.framework.shiro.session;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common4ui.enums.OnlineStatus;
import com.zlt.framework.shiro.OnlineSession;
import com.zlt.framework.shiro.service.IShiroService;
import lombok.Getter;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 针对自定义的ShiroSession的db操作
 *
 * @author ruoyi
 */
@Getter
public class OnlineSessionDAO extends EnterpriseCacheSessionDAO {

    public OnlineSessionDAO() {
        super();
    }

    public OnlineSessionDAO(long expireTime) {
        super();
    }

    public IShiroService getIShiroService(){
        return SpringUtils.getBean("sysShiroService");
    }

    /**
     * 根据会话ID获取会话
     *
     * @param sessionId 会话ID
     * @return ShiroSession
     */
    @Override
    protected Session doReadSession(Serializable sessionId) {
        return getIShiroService().getSession(sessionId);
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        super.update(session);
    }

    /**
     * 当会话过期/停止（如用户退出时）属性等会调用
     */
    @Override
    protected void doDelete(Session session) {
        OnlineSession onlineSession = (OnlineSession) session;
        if (null == onlineSession) {
            return;
        }
        onlineSession.setStatus(OnlineStatus.off_line);
        getIShiroService().deleteSession(onlineSession);
    }
}
