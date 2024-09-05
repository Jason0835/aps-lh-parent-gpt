package com.zlt.framework.shiro.service;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.domain.SessionBody;
import com.zlt.framework.monoclient.CasWebClient;
import com.zlt.framework.shiro.OnlineSession;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * 单点CAS会话db操作处理
 */
@Service("casShiroService")
public class CasShiroService implements IShiroService {

    @Value("${shiro.sso.validateSessionUri}")
    private String validateSessionUri;

    @Autowired
    private CasWebClient casWebClient;

    @Value("${shiro.sso.deleteSessionUri}")
    private String deleteSessionUri;

    @Value("${shiro.sso.aliveTickesUri}")
    private String casAliveTickeUri;

    /**
     * 获取会话信息
     *
     * @param sessionId
     * @return
     */
    @Override
    public Session getSession(Serializable sessionId) {
        String uri = validateSessionUri + "?sessionId=" + sessionId;
        HashMap loginUser = casWebClient.getRequest().getCasSessionVaild(uri);
        OnlineSession session = this.createSession(loginUser);
        if (!StringUtils.isNotNull(session.getId())) {
            session.setId(sessionId);
        }
        return session;
    }

    @Override
    public OnlineSession createSession(HashMap loginUser) {
        OnlineSession onlineSession = new OnlineSession();
        if (StringUtils.isNotEmpty(loginUser)) {
            String loginUserJson = (String) loginUser.get("loginUser");
            LoginUser user = JSON.parseObject(loginUserJson, LoginUser.class);
            String userOnlineJson = (String) loginUser.get("userOnline");
            SysUserOnline sysUserOnline = JSON.parseObject(userOnlineJson, SysUserOnline.class);
            if (StringUtils.isNotNull(user)) {

                onlineSession.setId(sysUserOnline.getSessionId());
                onlineSession.setHost(sysUserOnline.getIpaddr());
                onlineSession.setBrowser(sysUserOnline.getBrowser());
                onlineSession.setOs(sysUserOnline.getOs());
                onlineSession.setDeptName(user.getSysUser().getDept().getDeptName());
                onlineSession.setLoginName(sysUserOnline.getUserName());
                onlineSession.setTimeout(user.getExpireTime());
            }
        }
        return onlineSession;
    }

    /**
     * 删除会话
     *
     * @param onlineSession 会话信息
     */
    @Override
    public void deleteSession(OnlineSession onlineSession) {
        String uri = deleteSessionUri + "?sessionId=" + onlineSession.getId();
        casWebClient.getRequest().deleteCasSession(uri);
    }

    @Override
    public List<SessionBody> getAllSessions(){
        List<SessionBody> result = casWebClient.getRequest().getCasSessionKeys(casAliveTickeUri);
        result = JSON.parseArray(JSON.toJSONString(result), SessionBody.class);
        return result;
    }

}
