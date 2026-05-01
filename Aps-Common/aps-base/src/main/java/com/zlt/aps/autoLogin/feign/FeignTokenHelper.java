package com.zlt.aps.autoLogin.feign;

import com.zlt.aps.autoLogin.loginUtils.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Feign调用Token辅助工具
 * 在没有HTTP请求上下文的场景（如定时任务、Service内部Feign调用）中，
 * 显式地为Feign请求设置Authorization头，确保请求能通过网关认证。
 *
 * @author Chen
 * @since 2026/04/30
 */
@Slf4j
@Component
public class FeignTokenHelper {

    private static TokenUtils tokenUtils;

    @Autowired
    public void setTokenUtils(TokenUtils tokenUtils) {
        FeignTokenHelper.tokenUtils = tokenUtils;
    }

    /**
     * 在带有Token的上下文中执行Feign调用（无返回值）
     *
     * @param action Feign调用逻辑
     */
    public static void runWithToken(Runnable action) {
        try {
            setTokenHeader();
            action.run();
        } finally {
            FeignRequestContext.clear();
        }
    }

    /**
     * 在带有Token的上下文中执行Feign调用（有返回值）
     *
     * @param action Feign调用逻辑
     * @param <T>    返回值类型
     * @return Feign调用结果
     */
    public static <T> T callWithToken(Supplier<T> action) {
        try {
            setTokenHeader();
            return action.get();
        } finally {
            FeignRequestContext.clear();
        }
    }

    /**
     * 设置Token到FeignRequestContext
     */
    private static void setTokenHeader() {
        String token = tokenUtils.getToken();
        if (token != null) {
            FeignRequestContext.addHeader("Authorization", "Bearer " + token);
        } else {
            log.warn("获取同步Token失败，Feign请求可能无法通过网关认证");
        }
    }
}
