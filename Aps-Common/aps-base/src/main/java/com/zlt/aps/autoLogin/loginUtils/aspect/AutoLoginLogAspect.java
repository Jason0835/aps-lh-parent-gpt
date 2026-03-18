package com.zlt.aps.autoLogin.loginUtils.aspect;


import com.zlt.aps.autoLogin.feign.FeignRequestContext;
import com.zlt.aps.autoLogin.loginUtils.TokenUtils;
import com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * API日志切面 用于记录API接口的请求和响应日志
 *
 * @author zhangxh
 * @date 20240606
 */
@Aspect
@Component
public class AutoLoginLogAspect {


    public static final String REQUEST_ID = "requestId";



    private final TokenUtils tokenUtils;

    public AutoLoginLogAspect(TokenUtils tokenUtils) {
        this.tokenUtils = tokenUtils;
    }

    // 拦截所有使用@AutoLoginLog注解的方法
    @Pointcut("@annotation(com.zlt.aps.autoLogin.loginUtils.annotation.AutoLoginLog)")
    public void apiLogPointcut() {
    }

    /**
     * 响应前记录日志
     * @param joinPoint
     */
    @Before("apiLogPointcut()")
    public void before(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID, requestId);

        // 获取方法上注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoLoginLog apiLog = method.getAnnotation(AutoLoginLog.class);

        //如果需要登录验证，需要获取登录信息
        if ("Y".equals(apiLog.isLogin())) {
            // 校验token
            String token = tokenUtils.getToken();
            // 设置请求头信息
            FeignRequestContext.addHeader("Authorization", "Bearer " + token);
            FeignRequestContext.addHeader("X-Request-ID", requestId);
        }

//        try {
//            SyncServiceLog syncServiceLog = new SyncServiceLog();
//            syncServiceLog.setFromSystem(apiLog.fromSystem());
//            syncServiceLog.setServiceName(apiLog.serviceName());
//            syncServiceLog.setToSystem(apiLog.toSystem());
//            syncServiceLog.setRequestId(requestId);
//            syncServiceLog.setUrl(request.getRequestURL().toString());
//            syncServiceLog.setRequest(JSONUtil.toJsonStr(joinPoint.getArgs()));
//            syncServiceLog.setStratTime(DateUtil.date());
//            logService.save(syncServiceLog);
//            logger.info("API日志已记录: requestId={}, service={}", requestId, apiLog.serviceName());
//        } catch (Exception e) {
//            logger.error("API日志记录失败: requestId={}, error={}", requestId, e.getMessage());
//        }
    }

    @AfterReturning(pointcut = "apiLogPointcut()", returning = "result")
    public void afterReturning(Object result) {
        // 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        
        // 清理请求头和过程变量
        FeignRequestContext.clear();
//        String process = null;
//        Map<String, Object> variables = ProcessContext.getAllVariables();
//        if (!variables.isEmpty()) {
//            process = JSONUtil.toJsonStr(variables);
//            ProcessContext.clear();
//        }

//        HttpServletRequest request = attributes.getRequest();
//        String requestId = (String) request.getAttribute(REQUEST_ID);
//        if (requestId == null) {
//            return;
//        }
//
//        try {
//            SyncServiceLog syncServiceLog = new SyncServiceLog();
//            syncServiceLog.setRequestId(requestId);
//            syncServiceLog.setResponse(JSONUtil.toJsonStr(result));
//            syncServiceLog.setProcess(process);
//
//            JSONObject jsonObject = JSONUtil.parseObj(result);
//            if (jsonObject != null) {
//                String code = jsonObject.getStr("code");
//                syncServiceLog.setIsSuccess("200".equals(code) ? "1" : "0");
//            }
//
//            logService.updateByRequestId(syncServiceLog);
//        } catch (Exception e) {
//            logger.error("API回写日志失败: requestId={}, error={}", requestId, e.getMessage());
//        }
    }
}
