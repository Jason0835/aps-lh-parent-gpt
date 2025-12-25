package com.tlt.aps.redissonLock.aspect;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.redissonLock.annotation.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;

/**
 * 分布式锁注解切面（支持国际化+参数化占位符）
 * @author wengpc
 */
@Aspect
@Component
@Slf4j
public class DistributedLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.tlt.aps.redissonLock.annotation.DistributedLock)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解和方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);

        // 解析SpEL生成锁key
        String lockKey = parseSpEL(distributedLock.key(), joinPoint, method);
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            // 尝试获取锁
            isLocked = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );
            if (!isLocked) {
                // 解析国际化提示信息（含参数替换）
                String failMessage = getI18nMessage(distributedLock.failMsg(), distributedLock.args(), joinPoint, method);
                throw new RuntimeException(failMessage);
            }
            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 释放锁
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 解析SpEL表达式生成锁key/参数值
     */
    private String parseSpEL(String spel, ProceedingJoinPoint joinPoint, Method method) {
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        StandardEvaluationContext context = new StandardEvaluationContext();
        // 绑定方法参数到SpEL上下文（支持对象参数）
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        try {
            // 解析SpEL表达式
            Object value = parser.parseExpression(spel).getValue(context);
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            // 解析失败时返回原始SpEL字符串
            log.error("SpEL解析失败：{} 异常：{}", spel, e.getMessage());
            return spel;
        }
    }

    /**
     * 解析带参数的国际化消息
     * @param msgKey 国际化key
     * @param argsSpEL 占位符参数的SpEL表达式数组
     * @return 替换占位符后的提示文本
     */
    private String getI18nMessage(String msgKey, String[] argsSpEL, ProceedingJoinPoint joinPoint, Method method) {
        try {
            // 解析参数SpEL，生成参数数组
            Object[] args = new Object[argsSpEL.length];
            for (int i = 0; i < argsSpEL.length; i++) {
                args[i] = parseSpEL(argsSpEL[i], joinPoint, method);
            }

            // 解析国际化消息（替换占位符）
            return StringUtils.format(I18nUtil.getMessage(msgKey),args);

        } catch (Exception e) {
            // 异常时返回key本身，避免程序报错
            return msgKey;
        }
    }
}