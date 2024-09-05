package com.zlt.framework.cas;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author gongtao
 * @version 2018-07-05 15:30
 **/
@Slf4j
public class CallbackFilter extends io.buji.pac4j.filter.CallbackFilter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        super.doFilter(servletRequest, servletResponse, filterChain);
        log.debug("单点CallbackFilter,接收回调:{}" , servletRequest.getParameterMap());
    }
}
