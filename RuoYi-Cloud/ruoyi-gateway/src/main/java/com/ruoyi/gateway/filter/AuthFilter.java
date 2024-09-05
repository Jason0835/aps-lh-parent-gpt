package com.ruoyi.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import com.ruoyi.gateway.config.properties.IgnoreWhiteProperties;
import com.ruoyi.gateway.i18n.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Locale;

/**
 * 网关鉴权
 *
 * @author ruoyi
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {
    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final static long EXPIRE_TIME = Constants.TOKEN_EXPIRE * 60;

    // 排除过滤的 uri 地址，nacos自行添加
    @Autowired
    private IgnoreWhiteProperties ignoreWhite;

    @Resource(name = "stringRedisTemplate")
    protected ValueOperations<String, String> sops;

    @Autowired
    private RedisService redisService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        HttpHeaders headers = exchange.getRequest().getHeaders();
        // 跳过不需要验证的路径
        boolean isIgnoreWhite = StringUtils.matches(url, ignoreWhite.getWhites()); //是否是白名单路径
        String token = getToken(exchange.getRequest());
        if (StringUtils.isBlank(token) && !isIgnoreWhite) {
            return setUnauthorizedResponse(exchange, I18nUtil.getMessage("gateway.error.token.isnull",headers));
        }
        if(StringUtils.isBlank(token) && isIgnoreWhite) {
            //token为空，并且是白名单路径
            return chain.filter(exchange);
        }
        String userStr = sops.get(getTokenKey(token));
        if (StringUtils.isNull(userStr) && !isIgnoreWhite) {
            return setUnauthorizedResponse(exchange, I18nUtil.getMessage("gateway.error.token.expired",headers));
        }
        if (StringUtils.isNull(userStr) && isIgnoreWhite) {
            //token为空，并且是白名单路径
            return chain.filter(exchange);
        }
        LoginUser loginUser = JSONObject.parseObject(userStr, LoginUser.class);
        String userid = loginUser.getUserid().toString();
        String username = loginUser.getUsername();
        String deptName = loginUser.getSysUser().getDept().getDeptName();

        Locale locale = I18nUtil.getLocaleFromHeaders(exchange.getRequest().getHeaders());
        String lang = locale.toString();

        String factory = TokenUtil.getUserParam(headers,CacheConstants.TOKEN_FACTORY);

        deptName = getDeptName4Locale(headers, loginUser, deptName, locale);

        if ((StringUtils.isBlank(userid) || StringUtils.isBlank(username)) && !isIgnoreWhite) {
            return setUnauthorizedResponse(exchange, I18nUtil.getMessage("gateway.error.token.checkfail",headers));
        }
        if ((StringUtils.isBlank(userid) || StringUtils.isBlank(username)) && isIgnoreWhite) {
            //白名单路径
            return chain.filter(exchange);
        }

        frushToken(token, userid, headers);


        // 设置用户信息到请求
        ServerHttpRequest mutableReq = exchange.getRequest().mutate().header(CacheConstants.DETAILS_USER_ID, userid)
                .header(CacheConstants.DETAILS_USERDEPTNAME, deptName)
                .header(CacheConstants.TOKEN_LANG, lang)
                .header(CacheConstants.TOKEN_FACTORY, factory)
                .header(CacheConstants.DETAILS_USERNAME, ServletUtils.urlEncode(username)).build();

        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

        return chain.filter(mutableExchange);
    }

    private String getDeptName4Locale(HttpHeaders headers, LoginUser loginUser, String deptName, Locale locale) {
        try {
            deptName = StringUtils.getLocaleName(loginUser.getSysUser().getDept().getLangJson()
                    , locale
                    , deptName
                    );
            deptName = URLEncoder.encode(deptName, Constants.UTF8);
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.error.token.encodefail",headers), e);
        }
        return deptName;
    }

    protected Mono<Void> setUnauthorizedResponse(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = exchange.getRequest().getHeaders();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatus.OK);

        log.error(I18nUtil.getMessage("gateway.error.auth.checkfail.url",headers), exchange.getRequest().getPath(), msg);

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            return bufferFactory.wrap(JSON.toJSONBytes(R.fail(msg)));
        }));
    }

    protected String getTokenKey(String token) {
        return CacheConstants.LOGIN_TOKEN_KEY + token;
    }

    /**
     * 获取请求token
     */
    public static String getToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(CacheConstants.HEADER);
        if (StringUtils.isNotEmpty(token) && token.startsWith(CacheConstants.TOKEN_PREFIX)) {
            token = token.replace(CacheConstants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /***
     * 维持token有效性
     * @param token
     * @param userid
     */
    public void frushToken(String token, String userid, HttpHeaders headers) {

        // 设置过期时间
        redisService.expire(getTokenKey(token), EXPIRE_TIME);

        try {
            //有操作记录，刷新一下缓存
            String key = CacheConstants.TOKEN_LAST_OPER_TIME + userid;
            redisService.setCacheObject(key, (new Date()).getTime());
        } catch (Exception e) {
            log.error(I18nUtil.getMessage("gateway.error.gateway.redis.fail",headers), e);
        }

        redisService.expire(Constants.LOCALE_SESSION_ATTRIBUTE_NAME + token, EXPIRE_TIME);
        redisService.expire(Constants.TIME_ZONE_SESSION_ATTRIBUTE_NAME + token, EXPIRE_TIME);
    }

    @Override
    public int getOrder() {
        return -200;
    }
}