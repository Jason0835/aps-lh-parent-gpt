package com.ruoyi.framework.web.exception;

import javax.servlet.http.HttpServletRequest;

import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.DemoModeException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.constant.CacheConstants;
import com.ruoyi.common4ui.utils.CacheUtils;
import com.ruoyi.common4ui.utils.spring.SpringUtils4BootUI;
import com.zlt.framework.config.SsoConfig;
import com.zlt.framework.utils.AuthorizationUtils;
import com.ruoyi.system.api.ISysLoginService;
import feign.FeignException;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;
import com.ruoyi.common4ui.exception.BusinessException;
import com.ruoyi.common4ui.utils.ServletUtils;
import com.ruoyi.common4ui.utils.security.PermissionUtils;

import java.io.IOException;

/**
 * 全局异常处理器
 * 
 * @author ruoyi
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler4UI
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler4UI.class);

    @Autowired
    private ISysLoginService sysLoginService;

    @Autowired
    SsoConfig ssoConfig;

    /**
     * 权限校验失败 如果请求为ajax返回json，普通请求跳转页面
     */
    @ExceptionHandler(AuthorizationException.class)
    public Object handleAuthorizationException(HttpServletRequest request, AuthorizationException e)
    {
        log.error(e.getMessage(), e);
        if (ServletUtils.isAjaxRequest(request))
        {
            return AjaxResult.error(PermissionUtils.getMsg(e.getMessage()));
        }
        else
        {
            ModelAndView modelAndView = getErrorPageMV(e.getMessage(), "error/unauth");
            return modelAndView;
        }
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler({ HttpRequestMethodNotSupportedException.class })
    public AjaxResult handleException(HttpRequestMethodNotSupportedException e)
    {
        log.error(e.getMessage(), e);
        return AjaxResult.error(StringUtils.format(I18nUtil.getMessage("ui.global.noSupport.method.fail"),e.getMethod()));
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult notFount(RuntimeException e)
    {
        log.error(I18nUtil.getMessage("ui.global.runtime.fail"), e);
        return AjaxResult.error(I18nUtil.getMessage("ui.global.runtime.fail") + e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e)
    {
        log.error(e.getMessage(), e);
        return AjaxResult.error(I18nUtil.getMessage("ui.global.server.fail"));
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Object businessException(HttpServletRequest request, BusinessException e)
    {
        log.error(e.getMessage(), e);
        if (ServletUtils.isAjaxRequest(request))
        {
            return AjaxResult.error(e.getMessage());
        }
        else
        {
            ModelAndView modelAndView = getErrorPageMV(e.getMessage(), "error/business");
            return modelAndView;
        }
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult validatedBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult demoModeException(DemoModeException e)
    {
        return AjaxResult.error(I18nUtil.getMessage("ui.global.demo.running"));
    }

    @ExceptionHandler(FeignException.class)
    public Object FeignException(FeignException e){
        log.error(e.getMessage(), e);

        //Joran 2020-12-25 出现feign请求异常，进行登录信息校验,若已失效则进行会话清除start
        if(ssoConfig.getEnable()){
            R<LoginUser> result = sysLoginService.geteUser();
            if (result.getCode() != Constants.SUCCESS) {
                // 清理远程缓存
                SpringUtils4BootUI.getBean(ISysLoginService.class).logout();
                // 清理本地缓存
                CacheUtils.remove(CacheConstants.SYSTEM_DATA_KEY_PREFIX +  AuthorizationUtils.getSessionId());
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                try {
                    WebUtils.issueRedirect(attributes.getRequest(),attributes.getResponse(),ssoConfig.getLogoutUrl());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        //Joran 2020-12-25 出现feign请求异常，进行登录信息校验,若已失效则进行会话清除end

        String msg = getExcptionInfo(e, I18nUtil.getMessage("ui.system.feign.invoke.fail"));

        ModelAndView modelAndView = getErrorPageMV(msg, "error/business");
        return modelAndView;
    }

    private ModelAndView getErrorPageMV(String msg, String errorPage) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("errorMessage", msg);
        modelAndView.setViewName(errorPage);
        return modelAndView;
    }

    private String getExcptionInfo(Exception e, String title){
        return StringUtils.format(title
                , e.getClass().getName()
                , e.getMessage()
                , e.getStackTrace()[0].toString()
        );
    }


}
