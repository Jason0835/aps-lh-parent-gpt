package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.SysFile;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.TimeoutUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 个人信息 业务处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/user/profile")
public class SysProfileController extends BaseController
{
    @Autowired
    private ISysUserService userService;
    
    @Autowired
    private TokenService tokenService;

    @Autowired
    private RemoteFileService remoteFileService;

    @Resource
    private ISysConfigService configService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile()
    {
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(username));
        ajax.put("postGroup", userService.selectUserPostGroup(username));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "system.title.personalprofile", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        if (userService.updateUserProfile(user) > 0)
        {
            LoginUser loginUser = tokenService.getLoginUser();
            // 更新缓存用户信息
            loginUser.getSysUser().setNickName(user.getNickName());
            loginUser.getSysUser().setPhonenumber(user.getPhonenumber());
            loginUser.getSysUser().setEmail(user.getEmail());
            loginUser.getSysUser().setSex(user.getSex());
            tokenService.setLoginUser(loginUser);
            return AjaxResult.success();
        }
        return AjaxResult.error(I18nUtil.getMessage("system.error.user.update.fail"));
    }

    /**
     * 重置密码
     */
    @Log(title = "system.title.personalprofile", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(String oldPassword, String newPassword)
    {
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        String password = user.getPassword();

        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return AjaxResult.error(I18nUtil.getMessage("system.error.user.pwd.oldpwdwrong"));
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return AjaxResult.error(I18nUtil.getMessage("system.error.user.pwd.mustdiff"));
        }
        if (userService.resetUserPwd(username, SecurityUtils.encryptPassword(newPassword)) > 0)
        {
            // 更新缓存用户密码
            LoginUser loginUser = tokenService.getLoginUser();
            loginUser.getSysUser().setPassword(SecurityUtils.encryptPassword(newPassword));
            tokenService.setLoginUser(loginUser);

            //修改密码成功,删除用户首次使用系统标识
            redisTemplate.delete("newUserUser:" + username);
            //密码修改成功后，重新设置密码有效期
            String passwordTermStr = configService.selectConfigByKey("sys.password.term");
            int passwordTerm = StringUtils.isBlank(passwordTermStr) ? 90 : Integer.parseInt(passwordTermStr);
            redisTemplate.opsForValue().set("passwordTerm:" + username, "1", passwordTerm, TimeUnit.DAYS);

            return AjaxResult.success();
        }
        return AjaxResult.error(I18nUtil.getMessage("system.error.user.pwd.update.fail"));
    }


    /**
     * 头像上传
     */
    @Log(title = "system.title.personalprofile", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws IOException
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
            R<SysFile> fileResult = remoteFileService.upload(file);
            if (StringUtils.isNull(fileResult) || StringUtils.isNull(fileResult.getData()))
            {
                return AjaxResult.error(I18nUtil.getMessage("file.error.service.fail"));
            }
            String url = fileResult.getData().getUrl();
            if (userService.updateUserAvatar(loginUser.getUsername(), url))
            {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", url);
                // 更新缓存用户头像
                loginUser.getSysUser().setAvatar(url);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return AjaxResult.error(I18nUtil.getMessage("file.error.file.upload.image.fail"));
    }
}
