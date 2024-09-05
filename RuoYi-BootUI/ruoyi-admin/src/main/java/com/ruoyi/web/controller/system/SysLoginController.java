package com.ruoyi.web.controller.system;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.constant.ShiroConstants;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common4ui.utils.ServletUtils;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.framework.shiro.realm.Pac4jRealmUtils;
import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 登录验证
 *
 * @author ruoyi
 */
@Controller
public class SysLoginController extends BaseController {

    @Value("${shiro.user.captchaEnabled:false}")
    private String captchaEnabled;
    @Value("${shiro.sso.enable:false}")
    private Boolean isCasLogin;

    @Value("${shiro.sso.indexUrl:/toIndex}")
    private String casIndexUrl;

    @GetMapping("/login")
    public String login(ModelMap mmap, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //Joran 2022-04-25 如果是单点登录请求登录后直接进行拦截处理start
        if(isCasLogin){
            response.sendRedirect(casIndexUrl);
        }
        //Joran 2022-04-25 如果是单点登录请求登录后直接进行拦截处理end

        // 如果是Ajax请求，返回Json字符串。
        if (ServletUtils.isAjaxRequest(request)) {
            return ServletUtils.renderString(response, "{\"code\":\"1\",\"msg\":\""+ I18nUtil.getMessage("ui.login.nologin.alert") +"\"}");
        }
        mmap.put("captchaEnabled", captchaEnabled);
        return "login";
    }

    @PostMapping("/login")
    @ResponseBody
    public AjaxResult ajaxLogin(HttpServletRequest request, String username, String password, Boolean rememberMe) {
        Object uuid = request.getSession().getAttribute(Constants.CAPTCHA_CODE_KEY);
        String strUuid = uuid == null ? "" : uuid.toString();
        Pac4jToken token = Pac4jRealmUtils.build(username, password, rememberMe
                , strUuid
                , request.getParameter(ShiroConstants.CURRENT_VALIDATECODE),captchaEnabled);
        Subject subject = SecurityUtils.getSubject();
        try {
            subject.login(token);
            return success();
        } catch (AuthenticationException e) {
            String msg = e.getMessage();;
            if (StringUtils.isEmpty(e.getMessage())) {
                msg = I18nUtil.getMessage("user.not.exists");
            }
            return error(msg);
        }
    }

    @GetMapping("/unauth")
    public String unauth() {
        return "error/unauth";
    }
}
