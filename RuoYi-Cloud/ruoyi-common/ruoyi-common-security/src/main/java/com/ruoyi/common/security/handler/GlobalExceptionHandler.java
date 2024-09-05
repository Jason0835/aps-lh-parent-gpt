package com.ruoyi.common.security.handler;

import com.ruoyi.common.i18n.utils.I18nUtil;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ruoyi.common.exception.BaseException;
import com.ruoyi.common.exception.CustomException;
import com.ruoyi.common.exception.DemoModeException;
import com.ruoyi.common.exception.PreAuthorizeException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

/**
 * 全局异常处理器
 * 
 * @author ruoyi
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 基础异常
     */
    @ExceptionHandler(BaseException.class)
    public AjaxResult baseException(BaseException e)
    {
        return AjaxResult.error(e.getDefaultMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(CustomException.class)
    public AjaxResult businessException(CustomException e)
    {
        if (StringUtils.isNull(e.getCode()))
        {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e)
    {
        log.error(e.getMessage(), e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult validatedBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        //String message = e.getAllErrors().get(0).getDefaultMessage();
        String key=e.getAllErrors().get(0).getDefaultMessage();
        String message=I18nUtil.getMessage(key);
        return AjaxResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object validExceptionHandler(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
       // String message = e.getBindingResult().getFieldError().getDefaultMessage();
        String key=e.getBindingResult().getFieldError().getDefaultMessage();
        String message=I18nUtil.getMessage(key);
        return AjaxResult.error(message);
    }
    
    /**
     * 权限异常
     */
    @ExceptionHandler(PreAuthorizeException.class)
    public AjaxResult preAuthorizeException(PreAuthorizeException e)
    {
        return AjaxResult.error(I18nUtil.getMessage("common.error.global.no.auth"));
    }
    
    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult demoModeException(DemoModeException e)
    {
        return AjaxResult.error(I18nUtil.getMessage("common.error.global.demo.mode.undo"));
    }

    @ExceptionHandler(FeignException.class)
    public AjaxResult feignException(FeignException e){
        String msg = getExcptionInfo(e, I18nUtil.getMessage("common.error.feign.invoke.fail"));
        log.error(msg);
        return AjaxResult.error(msg);
    }

    @ExceptionHandler(SQLException.class)
    public AjaxResult sQLException(SQLException e){
        String msg = getExcptionInfo(e, I18nUtil.getMessage("common.error.sql.running.fail"));
        log.error(msg);
        return AjaxResult.error(msg);
    }

    private String getExcptionInfo(Exception e, String title){
        return StringUtils.format(title
                , e.getClass().getName()
                , e.getMessage()
                , e.getStackTrace()[0].toString()
        );
    }
}
