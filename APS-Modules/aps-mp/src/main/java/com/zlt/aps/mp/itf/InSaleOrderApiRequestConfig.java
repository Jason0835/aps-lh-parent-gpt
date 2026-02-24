package com.zlt.aps.mp.itf;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

/**
 * 请求配置实现
 *
 * @author Chen
 * @date 2025/4/8
 */
@Data
public class InSaleOrderApiRequestConfig implements RequestConfigStrategy {

    /**
     * 请求地址
     */
    private String requestUrl;

    /**
     * 请求头
     */
    private HttpHeaders httpHeaders;

    /**
     * 请求体
     */
    private String requestBody;

    public InSaleOrderApiRequestConfig(String requestUrl, Map<String, String> headers, String requestBody) {
        this.requestUrl = requestUrl;
        this.requestBody = requestBody;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        MediaType type = MediaType.parseMediaType("application/json; charset=UTF-8");
        httpHeaders.setContentType(type);
        httpHeaders.add("Accept", MediaType.ALL_VALUE);
        if (headers != null) {
            Set<Map.Entry<String, String>> entrySet = headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                httpHeaders.add(entry.getKey(), entry.getValue());
            }
        }
        String timeMillis = String.valueOf(System.currentTimeMillis());
        String nonceStr = "100";
        String appId = "202503010101_2";
        String secret = "279353A6A31F35D475140B713DA89B5F";
        httpHeaders.add("timestamp", timeMillis);
        httpHeaders.add("signType", "sha256");
        httpHeaders.add("nonceStr", nonceStr);
        httpHeaders.add("appId", appId);
        String str = "appId=" + appId + "&nonceStr=" + nonceStr + "&signType=sha256" + "&timestamp=" + timeMillis + secret;
        String sign = DigestUtils.sha256Hex(str);
        httpHeaders.add("sign", sign);
        try {
            httpHeaders.add("Host", InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        httpHeaders.add("Connection", "Keep-Alive");
//        httpHeaders.add("User-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        this.httpHeaders = httpHeaders;
    }

    @Override
    public String getRequestUrl() {
        return this.requestUrl;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.httpHeaders;
    }

    @Override
    public String getRequestBody() {
        return this.requestBody;
    }

    @Override
    public String getLogString() {
        return "请求地址:" + this.requestUrl + "，请求头:" + this.httpHeaders + "，请求参数：" + this.requestBody;
    }
}
