package com.zlt.framework.shiro.service;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.domain.SessionBody;
import com.zlt.framework.shiro.OnlineSession;
import org.apache.shiro.session.Session;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface IShiroService {
    /**
     * 获取会话信息
     *
     * @param sessionId
     * @return
     */
    Session getSession(Serializable sessionId);

    OnlineSession createSession(HashMap loginUser);

    /**
     * 删除会话
     *
     * @param onlineSession 会话信息
     */
    void deleteSession(OnlineSession onlineSession);

    List<SessionBody> getAllSessions();
}
