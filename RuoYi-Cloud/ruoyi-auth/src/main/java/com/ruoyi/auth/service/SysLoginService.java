package com.ruoyi.auth.service;

import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.enums.UserStatus;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.exception.BaseException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.zlt.mdm.auth.api.domain.vo.UserSystemVo;
import com.zlt.mdm.auth.service.IMdmSystemAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 登录校验方法
 * 
 * @author ruoyi
 */
@Component
public class SysLoginService
{
    @Autowired
    private RemoteLogService remoteLogService;

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    IMdmSystemAuthService iMdmSystemAuthService;

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @Resource
    private ISysConfigService configService;

    //输入密码错误次数
    private String passwordErrorNum = "passwordErrorNum:";
    //密码连续输错后，被锁标识key
    private String passwordErrorUser = "passwordErrorUser:";

    /**
     * 登录
     */
    public LoginUser login(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isAnyBlank(username, password))
        {
            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.username.password.require"));
            throw new BaseException(I18nUtil.getMessage("auth.error.login.username.password.require"));
        }
//        // 密码如果不在指定范围内 错误
//        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
//                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
//        {
//            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.input.outrange"));
//            throw new BaseException(I18nUtil.getMessage("auth.error.login.input.outrange"));
//        }
        // 用户名不在指定范围内 错误
//        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
//                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
//        {
//            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.input.outrange"));
//            throw new BaseException(I18nUtil.getMessage("auth.error.login.input.outrange"));
//        }
        // 查询用户信息
        R<LoginUser> userResult = remoteUserService.postUserInfo(username);

        if (R.FAIL == userResult.getCode())
        {
            throw new BaseException(userResult.getMsg());
        }

        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData()))
        {
            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.login.notexist.user.account"));
            String msg=StringUtils.format(I18nUtil.getMessage("auth.error.login.notexist.user.username"),username);
            throw new BaseException(msg);
        }
        LoginUser userInfo = userResult.getData();
        SysUser user = userResult.getData().getSysUser();
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.account.deleted"));
            String msg=StringUtils.format(I18nUtil.getMessage("auth.error.account.deleted.username"),username);
            throw new BaseException(msg);
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.user.disabled"));
            String msg=StringUtils.format(I18nUtil.getMessage("auth.error.user.disabled.username"),username);
            throw new BaseException(msg);
        }

        String maxLockNumberStr = this.configService.selectConfigByKey("sys.password.lock.number"); //账号密码连续输错N此后，账号被锁定
        int mintaxLockNumber = StringUtils.isBlank(maxLockNumberStr) ? 5 : Integer.parseInt(maxLockNumberStr);
        String maxLockTimeStr = this.configService.selectConfigByKey("sys.password.lock.time"); //账号密码连续输错后被锁的时间（单位：分钟）
        int maxLockTime = StringUtils.isBlank(maxLockTimeStr) ? 30 : Integer.parseInt(maxLockTimeStr);
        if(redisTemplate.opsForValue().get(passwordErrorUser + username) != null) {
            //密码连续输错，账号被锁，时间到后自动解锁
            String msg = StringUtils.format(I18nUtil.getMessage("auth.error.user.lock"), mintaxLockNumber, maxLockTime);
            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, msg);
            throw new BaseException(msg);
        }

        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            String pasErrorNumStr = redisTemplate.opsForValue().get(passwordErrorNum + username);  //用户密码输错次数
            int pasErrorNum = StringUtils.isBlank(pasErrorNumStr) ? 0 : Integer.parseInt(pasErrorNumStr);
            pasErrorNum++;  //密码错误次数+1
            redisTemplate.opsForValue().set(passwordErrorNum + username, String.valueOf(pasErrorNum));

            if(pasErrorNum >= mintaxLockNumber) {
                //密码连续输错，被锁
                redisTemplate.opsForValue().set(passwordErrorUser + username, "1", maxLockTime , TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(passwordErrorNum + username, "0"); //重置密码输错次数
            }

            remoteLogService.saveLogininfor(username, Constants.LOGIN_FAIL, I18nUtil.getMessage("auth.error.user.wrongpassword"));
            throw new BaseException(I18nUtil.getMessage("auth.error.user.noexistorpassword"));
        }
        redisTemplate.opsForValue().set(passwordErrorNum + username, "0"); //密码输入正确后，重置密码输错次数
        remoteLogService.saveLogininfor(username, Constants.LOGIN_SUCCESS, I18nUtil.getMessage("auth.msg.login.success"));

        //20201204 linbn
        //加入第三方系统权限列表，系统Code给到前端
        UserSystemVo userSystemVo = iMdmSystemAuthService.selectSystemDataByUserId(user.getUserId());
        if(StringUtils.isNotNull(userSystemVo)) {
            userInfo.setSystemIds(userSystemVo.getSystemSet());
        }
        return userInfo;
    }

    public void logout(String loginName)
    {
        remoteLogService.saveLogininfor(loginName, Constants.LOGOUT, I18nUtil.getMessage("auth.msg.logout.success"));
    }

}