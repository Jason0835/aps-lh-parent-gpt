package com.ruoyi.gateway.handler;

import com.ruoyi.api.gateway.system.UserUtils;
import com.ruoyi.api.gateway.system.domain.SysUserOnline;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.gateway.filter.AuthFilter;
import com.ruoyi.gateway.i18n.I18nUtil;
import com.ruoyi.system.api.domain.SessionBody;
import com.ruoyi.api.gateway.system.model.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Component
public class SessionHandle {

    @Autowired
    private RedisService redisService;

    /***
     * 取会话token试一下，有没有，有则返回空
     * @param serverRequest
     * @return
     */
    public Mono<ServerResponse> check(ServerRequest serverRequest) {

        String token = AuthFilter.getToken(serverRequest.exchange().getRequest());
        HashMap<String, Object> datas = new HashMap<>();
        LoginUser user = redisService.getCacheObject(CacheConstants.LOGIN_TOKEN_KEY + token);

        if(StringUtils.isNull(user)){
            return ServerResponse.status(HttpStatus.NO_CONTENT).body(BodyInserters.fromValue(datas));
        }

        Long lastTime = Long.parseLong(redisService.getCacheObject(CacheConstants.TOKEN_LAST_OPER_TIME + user.getUserid().toString()));

        SysUserOnline sysUserOnline = UserUtils.loginUserToUserOnline(user, lastTime,
                I18nUtil.getLocaleFromHeaders(serverRequest.exchange().getRequest().getHeaders()));

        datas.put("loginUser", user);
        datas.put("userOnline", sysUserOnline);

        return ServerResponse.status(HttpStatus.OK).body(BodyInserters.fromValue(datas));
    }


    /***
     * 从Redis取出所有在线的客户端Session
     * @return
     */
    public Mono<ServerResponse> getAliveKeys(ServerRequest serverRequest) {
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SessionBody> sessionBodys = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                LoginUser user = redisService.getCacheObject(key);

                SessionBody body = new SessionBody();
                body.setAccessToken(user.getToken());
                body.setSessionId(user.getSessionId());
                body.setExpireTime(user.getExpireTime());
                sessionBodys.add(body);
            }
        }

        return ServerResponse.status(HttpStatus.OK).body(BodyInserters.fromValue(sessionBodys));
    }
}
