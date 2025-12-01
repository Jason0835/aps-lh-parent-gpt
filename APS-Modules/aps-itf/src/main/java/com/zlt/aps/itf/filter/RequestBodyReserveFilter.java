package com.zlt.aps.itf.filter;


import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.util.BodyReaderHttpServletRequestWrapper;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 针对post请求，将HttpServletRequest包一层 保留body里的参数
 * @Author tlt
 * @Date 2024/2/25 19:19
 **/
public class RequestBodyReserveFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;

        if(servletRequest instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            // POST请求类型，才获取POST请求体
            if(ApsConstant.HTTP_POST.equals(req.getMethod())){
                requestWrapper = new BodyReaderHttpServletRequestWrapper(req);
            }
        }

        if(requestWrapper == null) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(requestWrapper, servletResponse);
        }
    }
}
