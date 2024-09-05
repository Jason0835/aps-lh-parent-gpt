package com.zlt.auth.controller;

import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.auth.controller.TokenBaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.form.LoginBody;
import com.ruoyi.system.api.form.RegisterBody;
import com.zlt.auth.service.LLSysLoginService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * token 控制
 *
 * @author ruoyi
 */
@Slf4j
@Api("系统登录接口")
@RestController
public class LLTokenController extends TokenBaseController {

    @Autowired
    protected LLSysLoginService sysLoginService;

    @PostMapping("login")
    @Override
    public R<?> login(@RequestBody LoginBody form) {
        // 用户登录
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
        userInfo.setSessionId(form.getSessionId());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
    @Override
    public R<?> logout(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            String username = loginUser.getUsername();
            // 删除用户缓存记录
            remoteUserService.cleanToken(loginUser.getToken());
            // 记录用户退出日志
            sysLoginService.logout(username);
        }
        return R.ok();
    }

    @PostMapping("register")
    @Override
    public R<?> register(@RequestBody RegisterBody registerBody) {
        // 用户注册
        //sysLoginService.register(registerBody.getUsername(), registerBody.getPassword());
        return R.ok();
    }
}
