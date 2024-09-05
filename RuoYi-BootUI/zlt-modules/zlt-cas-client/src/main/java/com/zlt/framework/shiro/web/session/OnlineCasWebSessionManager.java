package com.zlt.framework.shiro.web.session;

import com.ruoyi.common.core.utils.SpringUtils;
import com.zlt.framework.shiro.service.IShiroService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * 主要是在此如果会话的属性修改了 就标识下其修改了 然后方便 OnlineSessionDao同步
 *
 * @author ruoyi
 */
@Slf4j
@Getter
public class OnlineCasWebSessionManager extends OnlineWebSessionManager {

    @Override
    public IShiroService getIShiroService(){
        return SpringUtils.getBean("casShiroService");
    }
}
