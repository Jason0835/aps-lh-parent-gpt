package com.ruoyi.api.gateway.system.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.SysUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(contextId = "iSysProfileService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysProfileService {

    /**
     * 个人信息
     * @return
     */
    @GetMapping("/user/profile")
    AjaxResult profile();

    /**
     * 修改用户
     * @param user
     * @return
     */
    @PutMapping("/user/profile")
    AjaxResult updateProfile(@RequestBody SysUser user);

    /**
     * 重置密码
     * @param oldPassword
     * @param newPassword
     * @return
     */
    @PutMapping("/user/profile/updatePwd")
    AjaxResult updatePwd(@RequestParam("oldPassword") String oldPassword,@RequestParam("newPassword") String newPassword);

    /**
     * 头像上传
     */
    @PostMapping("/user/profile/avatar")
    AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file);
}
