package com.zlt.mix.schedule.common.aspect;

import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 用户资源权限的鉴别和处理的切面处理
 *
 * @author Liam
 * @date 2022-07-12
 */
@Aspect
@Component
public class PermissionAspect {

    @Resource
    private TokenService tokenService;


    /**
     * 根据用户的资源权限对参数进行封装
     *
     * @param proceedingJoinPoint 连接点
     * @return 方法结果
     * @throws Throwable 异常
     */
    @Around("@annotation(com.zlt.mix.schedule.common.aspect.PermissionAnno)" +
            "||@annotation(com.zlt.mix.schedule.common.aspect.PermissionAnnos)")
    public Object mixAreaPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = proceedingJoinPoint.getArgs();

        PermissionAnno[] annotationsByType = method.getAnnotationsByType(PermissionAnno.class);
        for (PermissionAnno annotation : annotationsByType) {

            Object arg = args[annotation.index()];

            String getName = ReflectUtils.getFieldValue(arg, annotation.getName());

            //如果携带的参数不为空
            if (StringUtils.isNotEmpty(getName)) {
                ReflectUtils.setFieldValue(arg, annotation.setName(), Collections.singletonList(getName));
                continue;
            }

            //获取权限
            Set<String> mix = tokenService.getLoginUser().getPermissions().get(ZltConstant.MIX);

            //如果当前用户是Admin用户
            if (mix.contains(ZltConstant.ADMIN_PERMISSION)) {
                continue;
            }

            //判断对应权限
            String[] permissions = annotation.permissions();
            List<String> permissionList = new ArrayList<>();
            for (String permission : permissions) {
                if (mix.contains(permission)) {
                    permissionList.add(permission);
                }
            }
            //没有任何权限，直接返回默认结果值
            if (permissionList.size() == 0) {
                return annotation.returnType().newInstance();
            }

            //给SQL的入参赋值
            ReflectUtils.setFieldValue(arg, annotation.setName(), permissionList);
        }
        return proceedingJoinPoint.proceed();
    }
}
