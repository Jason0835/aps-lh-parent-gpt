package com.zlt.aps.itf.util;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PostMethodUtils {

    public static String sendPost(String uri, String body, Map<String, String> header) {
        String result = "{}";
        CloseableHttpClient closeableHttpClient = null;
        try {
            closeableHttpClient = HttpClientBuilder.create().build();
            HttpPost httpPost = new HttpPost(uri);
    		httpPost.addHeader("Content-Type", "application/json");
            log.info("请求地址：" + uri + "，参数：" + body);
            if (header != null) {
            	for (Entry<String, String> entry: header.entrySet()) {
            		httpPost.addHeader(entry.getKey(), entry.getValue());
            	}
            }
            HttpEntity postEntitys = new StringEntity(body, "utf-8");
            httpPost.setEntity(postEntitys);
            HttpResponse httpResponse = null;
            HttpEntity entity = null;

            try {
                httpResponse = closeableHttpClient.execute(httpPost);
                entity = httpResponse.getEntity();

                if( entity != null ){
                    result = EntityUtils.toString(entity);
                }
            } catch (ClientProtocolException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
//            log.info("返回结果: " + result);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
	        	// 关闭连接
	        	IOUtils.close(closeableHttpClient);
            }catch (IOException e) {
                log.error(e.getMessage(), e);
			}
        }

        return result;
    }
}
