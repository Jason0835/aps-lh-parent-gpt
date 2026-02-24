package com.zlt.aps.mp.itf;

import org.springframework.http.HttpHeaders;

/**
 * 请求配置策略
 *
 * @author Chen
 * @date 2025/4/8
 */
public interface RequestConfigStrategy {

    /**
     * 请求参数
     *
     * @return 结果
     */
    String getRequestUrl();

    /**
     * 请求头
     *
     * @return 结果
     */
    HttpHeaders getHeaders();

    /**
     * 请求体
     *
     * @return 结果
     */
    String getRequestBody();

    /**
     * 打印日志
     *
     * @return 请求路径、请求头、请求体
     */
    String getLogString();
}
