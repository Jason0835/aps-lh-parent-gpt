package com.ruoyi.framework.interceptor;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.annotation.RepeatSubmit;
import com.ruoyi.common4ui.json.JSON;
import com.ruoyi.common4ui.utils.ServletUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Slf4j
@Getter
@Setter
@Component
public abstract class UserParamsInterceptor extends HandlerInterceptorAdapter {

    protected String paramName;
    protected String defaultValue = "";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {

            String parameter = request.getParameter(this.getParamName());

            if(StringUtils.isEmpty(parameter)){
                Object oldValue = request.getSession().getAttribute(this.getParamName());
                parameter = StringUtils.isNull(oldValue) ? defaultValue : oldValue.toString();
            }
            request.getSession().setAttribute(this.getParamName(), parameter);
            return true;
        } else {
            return super.preHandle(request, response, handler);
        }
    }

}
