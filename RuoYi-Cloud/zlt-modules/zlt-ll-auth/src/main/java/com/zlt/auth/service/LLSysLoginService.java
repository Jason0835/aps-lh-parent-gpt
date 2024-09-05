package com.zlt.auth.service;

import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.auth.service.SysLoginBaseService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.exception.BaseException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.utils.ADLoginUtils;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;
import java.util.List;

/**
 * 登录校验方法
 *
 * @author ruoyi
 */
@Component
@Slf4j
public class LLSysLoginService extends SysLoginBaseService {

    @Autowired
    ADLoginUtils adLoginUtils;
    @Value("${dc.switch:false}")
    private String dcSwitch;

    /**
     * 登录
     */
    @Override
    public LoginUser login(String username, String password) {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password)) {
            recordLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.username.password.require"));
            throw new BaseException(I18nUtil.getMessage("auth.error.login.username.password.require"));
        }
        // 密码如果不在指定范围内 错误
//        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
//                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
//        {
//            recordLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.input.outrange"));
//            throw new BaseException(I18nUtil.getMessage("auth.error.login.input.outrange"));
//        }
//        // 用户名不在指定范围内 错误
//        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
//                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
//        {
//            recordLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.input.outrange"));
//            throw new BaseException(I18nUtil.getMessage("auth.error.login.input.outrange"));
//        }

        //首先验证AD域的用户权限，如果能取回用户名账号，说明通过验证。
        SysUser adUser = new SysUser();
        if(Boolean.valueOf(dcSwitch)) {
            adUser = getUser(username, password);
        } else {
            //不进行ad域接口校验，默认都登录成功，fasel主要用来开发和测试环境
            adUser.setUserName(username);
        }

        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.postUserInfo(adUser.getUserName(), SecurityConstants.INNER);

        if (R.FAIL == userResult.getCode()) {
            throw new BaseException(userResult.getMsg());
        }

        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData())) {
            String msg = StringUtils.format(I18nUtil.getMessage("auth.error.login.notexist.user.username"), username);
            recordLogininfor(username, Constants.LOGIN_FAIL, msg);
            throw new BaseException(msg);
        }
        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            String msg = StringUtils.format(I18nUtil.getMessage("auth.error.account.deleted.username"), username);
            recordLogininfor(username, Constants.LOGIN_FAIL, msg);
            throw new BaseException(msg);
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            String msg = StringUtils.format(I18nUtil.getMessage("auth.error.user.disabled.username"), username);
            recordLogininfor(username, Constants.LOGIN_FAIL, msg);
            throw new BaseException(msg);
        }
//        if (!SecurityUtils.matchesPassword(password, user.getPassword())) {
//            recordLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.user.noexistorpassword"));
//            throw new BaseException(I18nUtil.getMessage("auth.error.user.noexistorpassword"));
//        }
//        recordLogininfor(username, Constants.LOGIN_SUCCESS, I18nUtil.getMessage("auth.msg.login.success"));

        //20201204 linbn
        //加入第三方系统权限列表，系统Code给到前端
        UserSystemVo userSystemVo = iMdmSystemAuthService.selectSystemDataByUserId(user.getUserId());
        if (StringUtils.isNotNull(userSystemVo)) {
            userInfo.setSystemIds(userSystemVo.getSystemSet());
        }
        return userInfo;
    }

    private SysUser getUser(String username, String password) {

        try {
            Hashtable env = adLoginUtils.getSearchTable(username, password);
            LdapContext ctx = adLoginUtils.getContext(env);
            List<SysUser> users = adLoginUtils.getUsers(ctx, username);
            if (users.size() > 0) {
                return users.get(0);
            }
        } catch (AuthenticationException ex) {
            String erroMsg = StringUtils.format("AD域用户名密码错误：{}",ex.toString());
            log.error(erroMsg);
            throw new BaseException(erroMsg);
        } catch (Throwable ex) {
            String erroMsg = StringUtils.format("AD域登录失败：{}",ex.toString());
            log.error(erroMsg);
            throw new BaseException(erroMsg);
        }

        String erroMsg = StringUtils.format("AD域用户名密码错误");
        throw new BaseException(erroMsg);
    }

}