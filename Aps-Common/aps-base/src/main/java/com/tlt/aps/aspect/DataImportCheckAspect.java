package com.tlt.aps.aspect;

import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.annotation.DataImportCheck;
import com.tlt.aps.exception.BusinessException;
import com.tlt.aps.utils.SpELi18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * 数据导入数量检查切面
 * @author wengpc
 */
@Aspect
@Component
@Slf4j
public class DataImportCheckAspect {

    @Pointcut("@annotation(com.tlt.aps.annotation.DataImportCheck)")
    public void dataImportCheckPointcut() {}

    /**
     * 前置通知：方法执行前检查数据数量
     */
    @Before("dataImportCheckPointcut()")
    public void beforeImport(JoinPoint joinPoint) {
        // 1、获取当前方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DataImportCheck annotation = method.getAnnotation(DataImportCheck.class);
        if (annotation == null) {
            return;
        }
        // 2、获取注解配置的参数
        int maxCount = annotation.maxCount();
        String messageKey = annotation.messageKey();
        String defaultMessage = annotation.defaultMessage();
        String[] params = annotation.params();
        // 3、获取待导入的数据
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("参数为空");
            return;
        }
        // 4、计算数据数量（方法第一个参数为待导入的集合/列表）
        Object data = args[0];
        int dataCount = 0;
        if (data instanceof Collection<?>) {
            Collection<?> dataCollection = (Collection<?>) data;
            dataCount = dataCollection.size();
        } else {
            log.warn("待导入数据必须是集合类型（List/Set等）");
            return;
        }
        // 5、数量校验
        if (dataCount > maxCount) {
            // 6、解析国际化提示语
            String tipMessage;
            try {
                // 从国际化配置中获取提示语
                tipMessage = SpELi18nUtil.getI18nMessage(messageKey, params, joinPoint, method);
            } catch (Exception e) {
                // 国际化解析失败，使用默认提示语
                tipMessage = StringUtils.format("{}（当前数量：{}，最大允许：{}）", defaultMessage, dataCount, maxCount);
            }
            throw new BusinessException(tipMessage);
        }
    }

}