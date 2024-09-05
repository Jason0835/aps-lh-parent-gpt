package com.ruoyi.gateway.service;

import java.io.IOException;
import java.util.HashMap;

import com.ruoyi.common.exception.CaptchaException;
import org.springframework.http.HttpHeaders;

/**
 * 验证码处理
 * 
 * @author ruoyi
 */
public interface ValidateCodeService
{
    /**
     * 生成验证码
     */
    public HashMap createCapcha() throws IOException, CaptchaException;

    /**
     * 校验验证码
     */
    public void checkCapcha(String key, String value, HttpHeaders headers) throws CaptchaException;
}
