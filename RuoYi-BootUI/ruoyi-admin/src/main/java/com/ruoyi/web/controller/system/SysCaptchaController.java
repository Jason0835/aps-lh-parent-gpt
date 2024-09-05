package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.core.utils.sign.Base64;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.framework.monoclient.GatewayWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * 图片验证码（支持算术形式）
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/captcha")
public class SysCaptchaController extends BaseController {

    @Autowired
    @Qualifier("gatewayWebClient")
    GatewayWebClient gatewayWebClient;

    /**
     * 验证码生成
     */
    @GetMapping(value = "/captchaImage")
    public ModelAndView getKaptchaImage(HttpServletRequest request, HttpServletResponse response) {
        ServletOutputStream out = null;
        try {
            HashMap codeMap = gatewayWebClient.getRequest().getCodeJson();
            if (!StringUtils.equals(codeMap.get(Constants.CODE).toString(), Constants.SUCCESS.toString())) {
                throw new RuntimeException(I18nUtil.getMessage("ui.captcha.get.fail"));
            }

            HttpSession session = request.getSession();
            response.setDateHeader("Expires", 0);
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.addHeader("Cache-Control", "post-check=0, pre-check=0");
            response.setHeader("Pragma", "no-cache");
            response.setContentType("image/jpeg");

            BufferedImage bi = null;
            session.setAttribute(Constants.CAPTCHA_CODE_KEY, codeMap.get(GatewayConstants.UUID));
            out = response.getOutputStream();
            byte[] image = Base64.decode(codeMap.get(GatewayConstants.IMG).toString());
            ByteArrayInputStream in = new ByteArrayInputStream(image);
            bi = ImageIO.read(in);
            ImageIO.write(bi, "jpg", out);
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}