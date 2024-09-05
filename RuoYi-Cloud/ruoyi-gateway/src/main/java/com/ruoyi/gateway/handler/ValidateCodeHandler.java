package com.ruoyi.gateway.handler;

import com.ruoyi.common.exception.CaptchaException;
import com.ruoyi.gateway.service.ValidateCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;

/**
 * 验证码获取
 *
 * @author ruoyi
 */
@Component
public class ValidateCodeHandler {
    @Autowired
    private ValidateCodeService validateCodeService;


    public Mono<ServerResponse> get(ServerRequest serverRequest) {
        HashMap ajax;
        try {
            ajax = validateCodeService.createCapcha();
        } catch (CaptchaException | IOException e) {
            return Mono.error(e);
        }
        return ServerResponse.status(HttpStatus.OK).body(BodyInserters.fromValue(ajax));
    }


}