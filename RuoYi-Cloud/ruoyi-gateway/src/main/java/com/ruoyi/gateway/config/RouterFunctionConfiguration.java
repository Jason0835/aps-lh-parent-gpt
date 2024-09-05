package com.ruoyi.gateway.config;

import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.gateway.handler.SessionHandle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import com.ruoyi.gateway.handler.ValidateCodeHandler;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * 路由配置信息
 *
 * @author ruoyi
 */
@Configuration
public class RouterFunctionConfiguration {



    @Bean
    public RouterFunction<ServerResponse> routers(ValidateCodeHandler validateCodeHandler, SessionHandle sessionHandle) {

        return RouterFunctions.route(RequestPredicates.GET("/" + GatewayConstants.CODE_URI), validateCodeHandler::get)
                .andRoute(RequestPredicates.GET("/" + GatewayConstants.SESSION_CHECK_URI).and(RequestPredicates.accept(MediaType.TEXT_PLAIN)), sessionHandle::check)
                .andRoute(RequestPredicates.GET("/" + GatewayConstants.ALIVE_SESSION_KEY), sessionHandle::getAliveKeys)
                ;
    }

}
