package com.zlt.framework.shiro.session;

import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common4ui.enums.OnlineStatus;
import com.zlt.framework.shiro.OnlineSession;
import com.zlt.framework.shiro.service.CasShiroService;
import com.zlt.framework.shiro.service.IShiroService;
import lombok.Getter;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.EnterpriseCacheSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * 针对自定义的ShiroSession的db操作,对应调取SSO方法
 *
 * @author linbn
 */
@Getter
public class OnlineSessionSSODAO extends OnlineSessionDAO {

    @Override
    public IShiroService getIShiroService(){
        return SpringUtils.getBean("casShiroService");
    }
}
