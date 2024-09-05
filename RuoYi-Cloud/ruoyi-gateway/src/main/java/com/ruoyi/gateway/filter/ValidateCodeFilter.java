package com.ruoyi.gateway.filter;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.sign.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.gateway.service.ValidateCodeService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 验证码过滤器
 * 
 * @author ruoyi
 */
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private final static String AUTH_URL = "/auth/login";

    @Autowired
    private ValidateCodeService validateCodeService;

    private static final String CODE = Constants.CODE;

    private static final String UUID = GatewayConstants.UUID;

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 非登录请求，不处理
            if (!StringUtils.containsIgnoreCase(request.getURI().getPath(), AUTH_URL))
            {
                return chain.filter(exchange);
            }

            try
            {
                String rspStr = resolveBodyFromRequest(request);
                JSONObject obj = JSONObject.parseObject(rspStr);
                boolean captchaEnabled = obj.get("captchaEnabled") == null ? false :obj.getBoolean("captchaEnabled");  //是否开启登录验证码
                if(captchaEnabled) {
                    validateCodeService.checkCapcha(obj.getString(CODE), obj.getString(UUID), request.getHeaders());
                }
            }
            catch (Exception e)
            {
                ServerHttpResponse response = exchange.getResponse();
                response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

                //201026,LINBN 不引用ajaxResult,直接返回数据。循环引用包问题。
                HashMap<String, Object> codeResult = new HashMap();
                codeResult.put(Constants.CODE, HttpStatus.ERROR);
                codeResult.put(GatewayConstants.MSG_TAG, e.getMessage());

                return exchange.getResponse().writeWith(
                        Mono.just(response.bufferFactory().wrap(JSON.toJSONBytes(codeResult))));
            }
            return chain.filter(exchange);
        };
    }

    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest)
    {
        // 获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();
        AtomicReference<String> bodyRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
            DataBufferUtils.release(buffer);
            bodyRef.set(charBuffer.toString());
        });
        return bodyRef.get();
    }
}
