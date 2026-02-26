package com.zlt.aps.autoLogin.loginUtils;

import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.system.api.ISysLoginService;
import com.ruoyi.system.api.form.LoginBody;
import com.zlt.aps.autoLogin.feign.FeignRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 接口登录方法Utils,用于获取token
 *
 * @author zhangxh
 * @date 20250506
 */
@Service
public class TokenUtils {

    private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

    @Resource
    private ISysLoginService iSysLoginService;

    @Resource
    private ISysUserService iSysUserService;

    @Resource
    private RedisService redisService;

    @Value("${sync.user.username:syncUser}")
    private String syncUsername;

    @Value("${sync.user.password:STzF8C#p8kyrAQy@6XzQ}")
    private String syncPassword;

    private static final String SYNC_TOKEN_KEY = "syncToken";

    /**
     * 登录并返回token
     */
    public synchronized String login() {
        // 先检查是否有其他线程已经登录成功了
        String existingToken = redisService.getCacheObject(SYNC_TOKEN_KEY);
        if (existingToken != null) {
            return existingToken;
        }

        log.info("开始执行同步用户登录: {}", syncUsername);
        LoginBody loginBody = new LoginBody();
        loginBody.setUsername(syncUsername);
        loginBody.setPassword(syncPassword);
        try {
            R<?> jsonObject = iSysLoginService.login(loginBody);
            if (jsonObject != null && jsonObject.getCode() == 200) {
                Object data = jsonObject.getData();
                if (data instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) data;
                    String token = String.valueOf(map.get("access_token"));
                    // 缓存30分钟
                    redisService.setCacheObject(SYNC_TOKEN_KEY, token, 60L, TimeUnit.MINUTES);
                    log.info("同步用户登录成功");
                    return token;
                }
            } else {
                log.error("同步用户登录失败: {}", jsonObject != null ? jsonObject.getMsg() : "响应为空");
            }
        } catch (Exception e) {
            log.error("同步用户登录异常", e);
        }
        return null;
    }

    public String getToken() {
        String token = redisService.getCacheObject(SYNC_TOKEN_KEY);
        if (token == null) {
            token = login();
        } else {
            // 如果已有token，尝试验证一下其有效性（可选，根据性能要求调整）
            // 这里保留原有的验证逻辑，但移除了冗余的redis获取
            try {
                FeignRequestContext.addHeader("Authorization", "Bearer " + token);
                FeignRequestContext.addHeader("X-Request-ID", UUID.randomUUID().toString());
                try {
                    AjaxResult ajaxResult = iSysUserService.getInfo(1L);
                    Object codeObj = ajaxResult.get("code");
                    if (codeObj != null && "401".equals(codeObj.toString())) {
                        token = login();
                    }
                } finally {
                    FeignRequestContext.clear();
                }
            } catch (Exception e) {
                log.warn("Token验证过程出现异常，尝试重新登录", e);
                token = login();
            }
        }
        return token;
    }
}
