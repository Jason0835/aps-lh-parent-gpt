package com.zlt.aps.redissonLock.aspect;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.client.RedissonLockClient;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.redissonLock.annotation.RedissonLockAnno;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 处理Redis锁的注解式实现
 *
 * @author Liam
 * @date 2023-08-28
 */
@Aspect
@Component
public class RedissonLockAspect {
    /**
     * 无锁名的默认锁名
     */
    private static final String LOCK_DEFAULT_NAME = "REDIS_LOCK:DEFAULT_LOCK";
    /**
     * 锁名分割符
     */
    private static final String LOCK_SEPARATOR = ":";
    /**
     * SPEL解析参数对象
     */
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    /**
     * spring方法参数名解析
     */
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Autowired
    private RedissonLockClient redissonLockClient;

    @Pointcut("@annotation(com.zlt.aps.redissonLock.annotation.RedissonLockAnno)")
    public void controllerAspect() {
    }

    @Around("controllerAspect()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法和对应注解
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        RedissonLockAnno annotation = method.getAnnotation(RedissonLockAnno.class);

        // 获取锁名
        String lockName = getLockName(method, annotation, joinPoint.getArgs());
        if (StringUtils.isBlank(lockName)) {
            lockName = LOCK_DEFAULT_NAME;
        }
        RLock lock = redissonLockClient.getLock(lockName);

        try {
            boolean isLock = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), annotation.unit());
            if (!isLock) {
                // 获取锁失败，表示目前有操作正在执行
                throw new RuntimeException(I18nUtil.getMessage(annotation.msgKey()));
            }

            // 执行具体业务逻辑
            return joinPoint.proceed(joinPoint.getArgs());
        } finally {
            // 释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取锁名
     *
     * @param method     方法
     * @param annotation 注解
     * @param args       参数
     * @return 锁名
     */
    private String getLockName(Method method, RedissonLockAnno annotation, Object[] args) {
        // 默认前缀
        StringBuilder builder = new StringBuilder();

        // 使用全局唯一标识
        if(StringUtils.isNotBlank(annotation.uniqueMark())){
            builder.append(annotation.uniqueMark());
        }

        // 需要根据指定参数解析
        String[] expressions = annotation.expressions();
        if (expressions != null && expressions.length > 0) {
            // 方法参数名
            String[] parameterNames = NAME_DISCOVERER.getParameterNames(method);
            if (args != null && parameterNames != null && args.length == parameterNames.length) {
                // 上下文对象
                StandardEvaluationContext context = new StandardEvaluationContext();
                for (int i = 0; i < args.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }

                // 参数值拼接结果
                StringBuilder params = new StringBuilder();

                // 逐个解析
                for (String expression : expressions) {
                    Expression parseExpression = PARSER.parseExpression(expression);
                    Object value = parseExpression.getValue(context);
                    params.append(value).append(LOCK_SEPARATOR);
                }

                // 指定参数添加到锁名
                builder.append(params);
            }
        }

        return builder.toString();
    }
}