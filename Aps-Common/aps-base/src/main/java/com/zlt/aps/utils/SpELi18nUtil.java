package com.zlt.aps.utils;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.lang.reflect.Method;

/**
 * SpEL国际化工具
 */
@Slf4j
public class SpELi18nUtil {

    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 解析带参数的国际化消息
     * @param messageKey 国际化key
     * @param argsSpEL 占位符参数的SpEL表达式数组
     * @return 替换占位符后的提示文本
     */
    public static String getI18nMessage(String messageKey, String[] argsSpEL, JoinPoint joinPoint, Method method) {
        try {
            // 解析参数SpEL，生成参数数组
            Object[] args = new Object[argsSpEL.length];
            for (int i = 0; i < argsSpEL.length; i++) {
                args[i] = parseSpEL(argsSpEL[i], joinPoint.getArgs(), method);
            }
            // 解析国际化消息（替换占位符）
            return StringUtils.format(I18nUtil.getMessage(messageKey),args);
        } catch (Exception e) {
            // 异常时返回key本身
            return messageKey;
        }
    }

    /**
     * 解析SpEL表达式
     * @param spel SpEL表达式字符串
     * @param args 方法参数值列表
     * @param method 方法
     * @return
     */
    public static String parseSpEL(String spel, Object[] args, Method method) {
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        StandardEvaluationContext context = new StandardEvaluationContext();
        // 绑定方法参数到SpEL上下文（支持对象参数）
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
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


}
