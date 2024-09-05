package com.ruoyi.system.api;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.model.LoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
/**
 * 用户权限信息服务
 *
 * @author linbn 20201214
 */
@FeignClient(contextId = "remoteAuthService", name = ServiceNameConstants.AUTH_SERVICE)
public interface RemoteAuthService {

    @PostMapping("appendUserAuth")
    public R<LoginUser> appendUserAuths(@RequestBody AjaxResult auths);
}
