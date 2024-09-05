package com.ruoyi.auth.controller;

import com.alibaba.fastjson.JSON;
import com.ruoyi.auth.service.SysLoginService;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.LangVo;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.system.api.form.LoginBody;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.zlt.mdm.auth.api.domain.MdmSystemData;
import com.zlt.mdm.auth.service.IMdmSystemAuthService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * token 控制
 *
 * @author ruoyi
 */
@Slf4j
@Api("系统登录接口")
@RestController
public class TokenController {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysLoginService sysLoginService;

    @Autowired
    RemoteUserService remoteUserService;

    @Autowired
    IMdmSystemAuthService iMdmSystemAuthService;

    @PostMapping("login")
    public R<?> login(@RequestBody LoginBody form) {
        // 用户登录
        LoginUser userInfo = sysLoginService.login(form.getUsername(), form.getPassword());
        userInfo.setSessionId(form.getSessionId());
        // 获取登录token
        return R.ok(tokenService.createToken(userInfo));
    }

    @DeleteMapping("logout")
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

    @PostMapping("refresh")
    public R<?> refresh(HttpServletRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            // 刷新令牌有效期
            tokenService.refreshToken(loginUser);
            return R.ok();
        }
        return R.ok();
    }

    /***
     * 改变国际化用户语言的ACTION入口
     * @param request http请求
     * @param lang 语言输入，如zh-CN
     * @return ok or fail
     * @author linbn 201023
     */
    @GetMapping("changeLang/{lang}")
    @Log(title = "auth.msg.changelang.operator", businessType = BusinessType.OTHER)
    public R<?> changeLang(HttpServletRequest request, @NonNull @PathVariable String lang) {
        String token = TokenUtil.getToken(request);
        String msg = StringUtils.format(I18nUtil.getMessage("auth.msg.changelang.log"), token);
        log.debug(msg);
        String[] array = lang.split("_");
        Locale locale = new Locale(array[0], array[1]);

        LocaleResolver resolver = SpringUtils.getBean("langLocaleResolver");
        resolver.setLocale(request, null, locale);
        return R.ok();
    }

    /***
     * 获取当前用户的语言设定
     * @return
     */
    @GetMapping("getLang")
    public R<LangVo> getUserLang() {
        LangVo userLang = new LangVo(I18nUtil.getLocaleFromRedis(), I18nUtil.getTimezoneFromRedis());
        return R.ok(userLang);
    }

    /**
     * 取得登录的用户数据
     *
     * @return
     */
    @GetMapping("getLoginUser")
    public R<LoginUser> geteUser() {
        LoginUser user = tokenService.getLoginUser();

        if (user == null) {
            return R.fail(I18nUtil.getMessage("auth.error.login.notexist.token.info"));
        }

        return R.ok(user);
    }

    /***
     * 取得现有系统所有的连接配置
     * @return
     */
    @PostMapping("systemList")
    public R<List<MdmSystemData>> getSystemList(){
        return R.ok(iMdmSystemAuthService.selectSystemDataList());
    }

    /***
     * 把权限补到redis当中的loginuser缓存里面
     * @param auths
     * @return
     */
    @PostMapping("appendUserAuth")
    public R<LoginUser> appendUserAuths(@RequestBody AjaxResult auths){

        String token = (String)auths.get("token");
        String sysCode = (String)auths.get(CacheConstants.TOKEN_SYS_CODE);
        LoginUser user = tokenService.getLoginUserByToken(token);
        if (user == null) {
            return R.fail(I18nUtil.getMessage("auth.error.login.notexist.token.info"));
        }

        Object roles = auths.get(UserConstants.KEY_AUTH_ROLES);
        Object permissions = auths.get(UserConstants.KEY_AUTH_PERMISSIONS);
        Object factorys = auths.get(UserConstants.KEY_AUTH_FACTORYS);

        List<String> listRole = JSON.parseArray(JSON.toJSONString(roles), String.class);
        List<String> listPermissions = JSON.parseArray(JSON.toJSONString(permissions), String.class);

        user.setSystemRoles(sysCode,
                new HashSet(listRole));
        user.setSystemPermissions(sysCode,
                new HashSet(listPermissions));
        user.setSystemRoleDeptLevel1(sysCode,
                JSON.parseArray(JSON.toJSONString(factorys),SysDept.class));

        tokenService.setLoginUser(user);

        return R.ok(user);
    }
}
