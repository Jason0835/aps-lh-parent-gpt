package com.ruoyi.system.api;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.factory.RemoteUserFallbackFactory;
import com.ruoyi.api.gateway.system.model.LoginUser;
import sun.security.util.SecurityConstants;

/**
 * 用户服务
 * 
 * @author ruoyi
 */
@FeignClient(contextId = "remoteUserService", name = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService
{
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @return 结果
     */
    @GetMapping(value = "/user/info/{username}")
    public R<LoginUser> getUserInfo(@PathVariable("username") String username);

    @PostMapping("/user/userInfo")
    public R<LoginUser> postUserInfo(@RequestParam("username") String username);


    @DeleteMapping("/online/cleanToken/{tokenId}")
    public AjaxResult cleanToken(@PathVariable("tokenId") String tokenId);

}
