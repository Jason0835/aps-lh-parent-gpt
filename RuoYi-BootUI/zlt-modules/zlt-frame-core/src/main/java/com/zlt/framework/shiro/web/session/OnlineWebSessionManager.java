package com.zlt.framework.shiro.web.session;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.CacheConstants;
import com.ruoyi.common4ui.constant.ShiroConstants;
import com.ruoyi.common4ui.utils.CacheUtils;
import com.ruoyi.system.api.domain.SessionBody;
import com.zlt.framework.shiro.OnlineSession;
import com.zlt.framework.shiro.service.IShiroService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionKey;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 主要是在此如果会话的属性修改了 就标识下其修改了 然后方便 OnlineSessionDao同步
 *
 * @author ruoyi
 */
@Slf4j
@Getter
public class OnlineWebSessionManager extends DefaultWebSessionManager {

    @Autowired
    RedisService redisService;

    public IShiroService getIShiroService(){
        return SpringUtils.getBean("sysShiroService");
    }

    @Override
    public void setAttribute(SessionKey sessionKey, Object attributeKey, Object value) throws InvalidSessionException {
        super.setAttribute(sessionKey, attributeKey, value);
        if (value != null && needMarkAttributeChanged(attributeKey)) {
            OnlineSession session = getOnlineSession(sessionKey);
            session.markAttributeChanged();
        }
    }

    private boolean needMarkAttributeChanged(Object attributeKey) {
        if (attributeKey == null) {
            return false;
        }
        String attributeKeyStr = attributeKey.toString();
        // 优化 flash属性没必要持久化
        if (attributeKeyStr.startsWith("org.springframework")) {
            return false;
        }
        if (attributeKeyStr.startsWith("javax.servlet")) {
            return false;
        }
        if (attributeKeyStr.equals(ShiroConstants.CURRENT_USERNAME)) {
            return false;
        }
        return true;
    }

    @Override
    public Object removeAttribute(SessionKey sessionKey, Object attributeKey) throws InvalidSessionException {
        Object removed = super.removeAttribute(sessionKey, attributeKey);
        if (removed != null) {
            OnlineSession s = getOnlineSession(sessionKey);
            s.markAttributeChanged();
        }

        return removed;
    }

    public OnlineSession getOnlineSession(SessionKey sessionKey) {
        OnlineSession session = null;
        Object obj = doGetSession(sessionKey);
        if (StringUtils.isNotNull(obj)) {
            session = new OnlineSession();
            BeanUtils.copyBeanProp(session, obj);
        }
        return session;
    }

    /**
     * 验证session是否有效 用于删除过期session
     */
    @Override
    public void validateSessions() {
        if (log.isInfoEnabled()) {
            log.info("invalidation sessions...");
        }

        int invalidCount = 0;

        List<SessionBody> result = getIShiroService().getAllSessions();
        Collection<Session> activeSessions = this.getActiveSessions();
        List<Session> deadSession = new ArrayList<>();

        //把需要删除的session取出来
        for (Session session : activeSessions) {
            if (result.stream().noneMatch(sessionBody ->
                    StringUtils.equals(sessionBody.getSessionId(), session.getId().toString())
            )) {
                deadSession.add(session);
            }
        }
        //这些session设置过期
        for (Session s : deadSession) {
            s.setTimeout(5 * 60 * 1000L);
        }

        // 激活Session,找不到的清理掉。
        for (SessionBody sessionId : result) {
            try {
                SessionKey key = new DefaultSessionKey(sessionId.getSessionId());
                Session session = retrieveSession(key);
                if (session == null) {
                    throw new InvalidSessionException();
                }
                //刷新有效期
                session.setTimeout(sessionId.getExpireTime());

                //redis的语言包设定刷新
                redisService.expire(Constants.UI_SESSION_PREFIX + Constants.LOCALE_SESSION_ATTRIBUTE_NAME + session.getId().toString(), Constants.TOKEN_EXPIRE,  TimeUnit.SECONDS);
                redisService.expire(Constants.UI_SESSION_PREFIX + Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME + session.getId().toString(), Constants.TOKEN_EXPIRE,  TimeUnit.SECONDS);

            } catch (InvalidSessionException e) {
                if (log.isDebugEnabled()) {
                    boolean expired = (e instanceof ExpiredSessionException);
                    String msg = "Invalidated session with id [" + sessionId.getSessionId() + "]"
                            + (expired ? " (expired)" : " (stopped)");
                    log.debug(msg);
                    CacheUtils.remove(CacheConstants.SYSTEM_DATA_KEY_PREFIX + sessionId.getSessionId());
                }
                invalidCount++;
            }
        }

        if (log.isInfoEnabled()) {
            String msg = "Finished invalidation session.";
            if (invalidCount > 0) {
                msg += " [" + invalidCount + "] sessions were stopped.";
            } else {
                msg += " No sessions were stopped.";
            }
            log.info(msg);
        }

    }

}
