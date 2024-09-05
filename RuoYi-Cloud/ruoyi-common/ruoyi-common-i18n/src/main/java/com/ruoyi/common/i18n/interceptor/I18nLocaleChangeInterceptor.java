package com.ruoyi.common.i18n.interceptor;

import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义拦截器
 * @author lbn
 */
public class I18nLocaleChangeInterceptor extends LocaleChangeInterceptor {

    private LocaleResolver localeResolver;

    /**
     * 默认构造方法
     */
    public I18nLocaleChangeInterceptor() {
    }

    /**
     * 注入redis自定义解析器
     *
     * @param localeResolver
     */
    public I18nLocaleChangeInterceptor(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws ServletException {
        //1.获取请求国际化参数
        String newLocale = request.getParameter(this.getParamName());
        if (newLocale != null && this.checkHttpMethod(request.getMethod())) {
            if (localeResolver == null) {
                throw new IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?");
            }

            try {
                //把语言写入到session
                request.getSession().setAttribute(this.getParamName(), newLocale);
                localeResolver.setLocale(request, response, this.parseLocaleValue(newLocale));
            } catch (IllegalArgumentException var7) {
                if (!this.isIgnoreInvalidLocale()) {
                    throw var7;
                }

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + var7.getMessage());
                }
            }
        }

        return true;
    }

    private boolean checkHttpMethod(String currentMethod) {
        String[] configuredMethods = this.getHttpMethods();
        if (ObjectUtils.isEmpty(configuredMethods)) {
            return true;
        } else {
            String[] var3 = configuredMethods;
            int var4 = configuredMethods.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                String configuredMethod = var3[var5];
                if (configuredMethod.equalsIgnoreCase(currentMethod)) {
                    return true;
                }
            }

            return false;
        }
    }
}
