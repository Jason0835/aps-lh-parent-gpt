package com.ruoyi.gateway.service.impl;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.imageio.ImageIO;

import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.gateway.i18n.I18nUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import com.google.code.kaptcha.Producer;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.exception.CaptchaException;
import com.ruoyi.common.core.utils.IdUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.utils.sign.Base64;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.gateway.service.ValidateCodeService;

/**
 * 验证码实现处理
 *
 * @author ruoyi
 */
@Service
public class ValidateCodeServiceImpl implements ValidateCodeService {
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    @Autowired
    private RedisService redisService;

    // 验证码类型
    private String captchaType = "math";

    /**
     * 生成验证码
     */
    @Override
    public HashMap createCapcha() throws IOException, CaptchaException {
        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        // 生成验证码
        if ("math".equals(captchaType)) {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        } else if ("char".equals(captchaType)) {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {

            //201026,LINBN 不引用ajaxResult,直接返回数据。循环引用包问题。
            HashMap<String, Object> codeResult = new HashMap();
            codeResult.put(Constants.CODE, HttpStatus.ERROR);
            codeResult.put(GatewayConstants.MSG_TAG, e.getMessage());

            return codeResult;
        }

        //201026,LINBN 不引用ajaxResult,直接返回数据。循环引用包问题。
        HashMap<String, Object> codeResult = new HashMap();
        codeResult.put(Constants.CODE, HttpStatus.SUCCESS);
        codeResult.put(GatewayConstants.UUID, uuid);
        codeResult.put(GatewayConstants.IMG, Base64.encode(os.toByteArray()));

        return codeResult;
    }

    /**
     * 校验验证码
     */
    @Override
    public void checkCapcha(String code, String uuid, HttpHeaders headers) throws CaptchaException {

        if (StringUtils.isEmpty(code)) {
            //"验证码不能为空"
            throw new CaptchaException(I18nUtil.getMessage("gateway.error.code.isnull",headers));
        }
        if (StringUtils.isEmpty(uuid)) {
            //"验证码已失效"
            throw new CaptchaException(I18nUtil.getMessage("gateway.error.code.expired",headers));
        }
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisService.getCacheObject(verifyKey);
        redisService.deleteObject(verifyKey);

        if (!code.equalsIgnoreCase(captcha)) {
            //"验证码错误"
            throw new CaptchaException(I18nUtil.getMessage("gateway.error.code.wrong",headers));
        }
    }
}
