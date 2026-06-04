package com.zlt.aps.interceptor;

import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class LocaleFallbackInterceptor implements HandlerInterceptor {

    public static final String LOCALE_ATTRIBUTE_NAME = "APS_RESOLVED_LOCALE";

    private static final String LANG_USER_KEY_PREFIX = "lang_i18n:user:";
    private static final String LANG_TOKEN_KEY_PREFIX = "lang_i18n:local:";
    private static final String UI_LANG_TOKEN_KEY_PREFIX = "UI:lang_i18n:local:";
    private static final String CHANGE_LANG_URI = "/vue/user/changeLang";
    private static final long TOKEN_KEY_EXPIRE_MINUTES = 30;

    @Autowired
    private RedisService redisService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String sessionId = resolveSessionId(request);
            String authToken = resolveAuthToken(request);

            String lang = resolveLang(request);

            if (StringUtils.isEmpty(lang)) {
                return true;
            }

            writeAllLocaleKeys(sessionId, authToken, lang);
            setShiroSessionLang(lang);
            setLocaleContextHolder(lang);
            request.setAttribute(LOCALE_ATTRIBUTE_NAME, lang);
            log.debug("语言缓存已写入：lang={}, sessionId={}", lang, sessionId);
        } catch (Exception e) {
            log.warn("LocaleFallbackInterceptor preHandle异常: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        String requestURI = request.getRequestURI();
        if (!requestURI.contains(CHANGE_LANG_URI)) {
            return;
        }

        try {
            String lang = request.getParameter("lang");
            if (StringUtils.isEmpty(lang)) {
                log.warn("changeLang请求缺少lang参数");
                return;
            }

            String sessionId = resolveSessionId(request);
            String authToken = resolveAuthToken(request);

            writeAllLocaleKeys(sessionId, authToken, lang);
            setShiroSessionLang(lang);
            setLocaleContextHolder(lang);
            request.setAttribute(LOCALE_ATTRIBUTE_NAME, lang);

            log.info("语言切换完成：lang={}, sessionId={}", lang, sessionId);
        } catch (Exception e) {
            log.warn("LocaleFallbackInterceptor postHandle异常: {}", e.getMessage());
        }
    }

    private String resolveLang(HttpServletRequest request) {
        String lang = request.getParameter("lang");
        if (StringUtils.isNotEmpty(lang)) {
            log.debug("从请求参数获取语言: lang={}", lang);
            return lang;
        }

        String shiroLang = resolveShiroSessionLang();
        if (StringUtils.isNotEmpty(shiroLang)) {
            log.debug("从Shiro Session获取语言: lang={}", shiroLang);
            return shiroLang;
        }

        String acceptLang = resolveAcceptLanguage(request);
        if (StringUtils.isNotEmpty(acceptLang)) {
            log.debug("从Accept-Language头获取语言: lang={}", acceptLang);
            return acceptLang;
        }

        String userLang = resolveUserLang();
        if (StringUtils.isNotEmpty(userLang)) {
            log.debug("从用户维度Redis获取语言: lang={}", userLang);
            return userLang;
        }

        return null;
    }

    private String resolveShiroSessionLang() {
        try {
            HttpSession session = getSession();
            if (session != null) {
                Object langObj = session.getAttribute("lang");
                if (langObj != null) {
                    String lang = Convert.toStr(langObj);
                    if (StringUtils.isNotEmpty(lang)) {
                        return lang;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("resolveShiroSessionLang异常: {}", e.getMessage());
        }
        return null;
    }

    private void setShiroSessionLang(String lang) {
        try {
            HttpSession session = getSession();
            if (session != null) {
                session.setAttribute("lang", lang);
                log.debug("已设置Shiro Session lang属性: {}", lang);
            }
        } catch (Exception e) {
            log.debug("setShiroSessionLang异常: {}", e.getMessage());
        }
    }

    /**
     * 获取当前HttpSession
     */
    private HttpSession getSession() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                return request.getSession(false);
            }
        } catch (Exception e) {
            log.debug("getSession异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                return ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
            }
        } catch (Exception e) {
            log.debug("getCurrentRequest异常: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAcceptLanguage(HttpServletRequest request) {
        try {
            String acceptLang = request.getHeader("Accept-Language");
            if (StringUtils.isNotEmpty(acceptLang)) {
                acceptLang = acceptLang.split(",")[0].trim();
                if (acceptLang.matches("[a-z]{2}_[A-Z]{2}")) {
                    return acceptLang;
                }
            }
        } catch (Exception e) {
            log.debug("resolveAcceptLanguage异常: {}", e.getMessage());
        }
        return null;
    }

    private void writeAllLocaleKeys(String sessionId, String authToken, String lang) {
        if (StringUtils.isNotEmpty(sessionId)) {
            redisService.setCacheObject(UI_LANG_TOKEN_KEY_PREFIX + sessionId, lang, TOKEN_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
            redisService.setCacheObject(LANG_TOKEN_KEY_PREFIX + sessionId, lang, TOKEN_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }

        if (StringUtils.isNotEmpty(authToken)) {
            redisService.setCacheObject(LANG_TOKEN_KEY_PREFIX + authToken, lang, TOKEN_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
            redisService.setCacheObject(UI_LANG_TOKEN_KEY_PREFIX + authToken, lang, TOKEN_KEY_EXPIRE_MINUTES, TimeUnit.MINUTES);
        }

        Long userId = getUserId();
        if (userId != null) {
            redisService.setCacheObject(LANG_USER_KEY_PREFIX + userId, lang);
        }
    }

    private String resolveUserLang() {
        try {
            Long userId = getUserId();
            if (userId != null) {
                String userLang = Convert.toStr(redisService.getCacheObject(LANG_USER_KEY_PREFIX + userId));
                if (StringUtils.isNotEmpty(userLang)) {
                    return userLang;
                }
            }
        } catch (Exception e) {
            log.debug("resolveUserLang异常: {}", e.getMessage());
        }
        return null;
    }

    private String resolveSessionId(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                return session.getId();
            }
        } catch (Exception e) {
            log.debug("获取SessionId异常: {}", e.getMessage());
        }
        return null;
    }

    private String resolveAuthToken(HttpServletRequest request) {
        try {
            String token = TokenUtil.getToken(request);
            if (StringUtils.isNotEmpty(token)) {
                return token;
            }
        } catch (Exception e) {
            log.debug("TokenUtil.getToken异常: {}", e.getMessage());
        }
        return null;
    }

    private void setLocaleContextHolder(String lang) {
        try {
            if (StringUtils.isNotEmpty(lang) && lang.contains("_")) {
                String[] parts = lang.split("_");
                Locale locale = new Locale(parts[0], parts[1]);
                LocaleContextHolder.setLocale(locale);
                log.debug("LocaleContextHolder已设置: lang={}, thread={}", locale, Thread.currentThread().getName());
            }
        } catch (Exception e) {
            log.debug("setLocaleContextHolder异常: {}", e.getMessage());
        }
    }

    private Long getUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
