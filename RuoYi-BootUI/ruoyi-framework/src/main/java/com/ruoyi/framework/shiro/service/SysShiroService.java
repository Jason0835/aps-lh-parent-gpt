package com.ruoyi.framework.shiro.service;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.ISysLoginService;
import com.ruoyi.system.api.domain.SessionBody;
import com.zlt.framework.monoclient.GatewayWebClient;
import com.zlt.framework.shiro.OnlineSession;
import com.zlt.framework.shiro.service.IShiroService;
import org.apache.shiro.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * 会话db操作处理
 *
 * @author ruoyi
 */
@Service("sysShiroService")
public class SysShiroService implements IShiroService {

    @Autowired
    private ISysLoginService iSysLoginService;

    @Autowired
    @Qualifier("gatewayWebClient")
    private GatewayWebClient gatewayWebClient;

    private String lastSessionID;

    /**
     * 删除会话
     *
     * @param onlineSession 会话信息
     */
    @Override
    public void deleteSession(OnlineSession onlineSession) {
        iSysLoginService.logout();
    }

    @Override
    public List<SessionBody> getAllSessions() {
        List<SessionBody> result = gatewayWebClient.getRequest().getSessionKeys();
        result = JSON.parseArray(JSON.toJSONString(result), SessionBody.class);
        return result;
    }

    /**
     * 获取会话信息
     *
     * @param sessionId
     * @return
     */
    @Override
    public Session getSession(Serializable sessionId) {
        //Joran 2021-12-06 同步Mps模块解决登录即失效问题start
        //HashMap loginUser = gatewayWebClient.getRequest().getSessionVaild();
        HashMap loginUser = null;
        if(!StringUtils.equals(lastSessionID, sessionId.toString())){
            loginUser = gatewayWebClient.getRequest().getSessionVaild();
            lastSessionID = sessionId.toString();
        }
        //Joran 2021-12-06 同步Mps模块解决登录即失效问题end

        OnlineSession session = this.createSession(loginUser);
        if (!StringUtils.isNotNull(session.getId())) {
            session.setId(sessionId);
        }
        return session;
    }

    @Override
    public OnlineSession createSession(HashMap loginUser) {
        OnlineSession onlineSession = new OnlineSession();
        if (StringUtils.isNotNull(loginUser)) {
            LoginUser user = (LoginUser) loginUser.get("loginUser");
            SysUserOnline sysUserOnline = (SysUserOnline) loginUser.get("userOnline");
            if (StringUtils.isNotNull(user)) {

                onlineSession.setId(sysUserOnline.getSessionId());
                onlineSession.setHost(sysUserOnline.getIpaddr());
                onlineSession.setBrowser(sysUserOnline.getBrowser());
                onlineSession.setOs(sysUserOnline.getOs());
                onlineSession.setDeptName(user.getSysUser().getDept().getDeptName());
                onlineSession.setLoginName(sysUserOnline.getUserName());
//            onlineSession.setStartTimestamp(user.getLoginTime());
//            onlineSession.setLastAccessTime(user.getLoginTime());
                onlineSession.setTimeout(user.getExpireTime());
            }
        }
        return onlineSession;
    }
}
