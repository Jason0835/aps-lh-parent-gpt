package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.api.gateway.system.model.LoginUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 在线用户监控对外暴露接口
 */
@FeignClient(contextId = "iSysUserOnlineService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysUserOnlineService {

    @GetMapping("/online/list")
    TableDataInfo list(@RequestParam("ipaddr") String ipaddr, @RequestParam("userName") String userName);

    /**
     * 强退用户
     * @param tokenId
     * @return
     */
    @DeleteMapping("/online/{tokenId}")
    AjaxResult forceLogout(@PathVariable("tokenId") String tokenId);

    @PostMapping("/online/loginUserToUserOnline")
    SysUserOnline loginUserToUserOnline(LoginUser user);

    @DeleteMapping("/online/logout/{ids}")
    AjaxResult forceLogoutByIds(@PathVariable("ids") String ids);
}
