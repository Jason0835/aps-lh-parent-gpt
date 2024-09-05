package com.ruoyi.web.controller.system;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.domain.vo.RouterVo;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.api.gateway.system.service.ISysMenuService;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.SpringUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TokenUtil;
import com.ruoyi.common4ui.config.Global;
import com.ruoyi.common4ui.constant.CacheConstants;
import com.ruoyi.common4ui.constant.ShiroConstants;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common4ui.utils.CacheUtils;
import com.ruoyi.common4ui.utils.CookieUtils;
import com.ruoyi.common4ui.utils.ServletUtils;
import com.ruoyi.file.api.service.IApsFileService;
import com.ruoyi.framework.shiro.service.SysPasswordService;
import com.ruoyi.system.api.ISysLoginService;
import com.zlt.framework.GlobalSetting;
import com.zlt.framework.utils.AuthorizationUtils;
import com.zlt.mdm.auth.api.domain.MdmSystemData;
import org.apache.shiro.authz.AuthorizationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.LocaleResolver;
import org.thymeleaf.util.ListUtils;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ruoyi.common4ui.constant.CacheConstants.SYSTEM_DATA_KEY_PREFIX;

/**
 * 首页 业务处理
 *
 * @author ruoyi
 */
//TODO:I18N
@Controller
public class SysIndexController extends BaseController {
    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private SysPasswordService passwordService;

    @Autowired
    private ISysLoginService iSysLoginService;

    @Autowired
    private GlobalSetting globalSetting;

    @Autowired
    private ISysUserService iSysUserService;

    @Autowired
    private IApsFileService iApsFileService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private static String localeAttributeName = Constants.LOCALE_SESSION_ATTRIBUTE_NAME;

    // 系统首页
    @GetMapping("/index")
    public String index(ModelMap mmap, @RequestParam(value = "lang", required = false) String lang, HttpServletRequest request, HttpServletResponse response) {

        SysUser user = AuthorizationUtils.getSysUser();

        if(StringUtils.isNotEmpty(user.getAvatar())){
            String imgEncode="data:image/jpg;base64,"+imageToBase64(user.getAvatar());
            user.setAvatar(imgEncode);
        }

        String indexStyle = null;
        try {
            //设置修改用户后端的语言,前端的语言由拦截控制。
            lang = StringUtils.isEmpty(lang) ? AuthorizationUtils.getLang() : lang;
            String token = AuthorizationUtils.getAccessToken();
            String cacheLang=redisTemplate.opsForValue().get(localeAttributeName + token);
            if (!lang.equals(cacheLang)) {
               iSysLoginService.changeLang(lang);
            }

            // 根据用户id取出菜单
            List<RouterVo> menus = menuService.getRoutersList();
            if (StringUtils.isNotNull(menus)) {
                mmap.put("menus", menus);
            }

            //一个系统都没有的情况下跳出不让访问。
            // 检查本系统有没有在清单里的，另外走拦截器
            List systems = getSystemData();
            if(ListUtils.isEmpty(systems)){
                throw new AuthorizationException(I18nUtil.getMessage("ui.biz.index.noSystemAuth.fail"));
            }
            //Joran 2020-12-28添加请求后台权限
            reloadRemoteLoginUser();
            List level1DeptList = AuthorizationUtils.getUserDeptRoleL1(globalSetting.getSysCode());

            mmap.put("user", user);
            mmap.put("sideTheme", configService.selectConfigByKey("sys.index.sideTheme"));
            mmap.put("skinName", configService.selectConfigByKey("sys.index.skinName"));
            mmap.put("ignoreFooter", configService.selectConfigByKey("sys.index.ignoreFooter"));
            mmap.put("copyrightYear", Global.getCopyrightYear());
            mmap.put("demoEnabled", Global.isDemoEnabled());
            mmap.put("systemCodes", systems);
            mmap.put("leve1Depts", level1DeptList);
            mmap.put("factory",AuthorizationUtils.getFactory());

            if(redisTemplate.hasKey("newUserUser:" + user.getUserName())) {
                mmap.put("newUserUser", I18nUtil.getMessage("ui.new.user.modify.passwrod.tip"));  //新用户首次使用系统未修改默认密码标识
                mmap.put("menus", null);
            }
            if(!redisTemplate.hasKey("passwordTerm:" + user.getUserName()) && !"admin".equals(user.getUserName())) {
                mmap.put("passwordTerm", I18nUtil.getMessage("ui.password.term.tip"));  //密码已经超过有效期标识
                mmap.put("menus", null);
            }
            // 菜单导航显示风格
            String menuStyle = configService.selectConfigByKey("sys.index.menuStyle");
            // 移动端，默认使左侧导航菜单，否则取默认配置
            indexStyle = ServletUtils.checkAgentIsMobile(ServletUtils.getRequest().getHeader("User-Agent")) ? "index" : menuStyle;
        } catch (Exception ex) {
            //跳到这里就是调后端异常
            String err = I18nUtil.getMessage("ui.index.getmenu.fail");
            logger.error(err, ex.getMessage(), ex);
            AuthorizationUtils.logout();
            throw ex;
        }

        // 优先Cookie配置导航菜单
        Cookie[] cookies = ServletUtils.getRequest().getCookies();
        for (Cookie cookie : cookies) {
            //Joran 2021-10-12 进行cookie重写解决，重启后首次登录成功后提示登录超时问题start
            String cookieName=cookie.getName();
            if(CacheConstants.JSESSIONID.equals(cookieName)){
                logger.info("登录成功进入首页，进行JSESSIONID重写，value="+cookie.getValue());
                response.addCookie(cookie);
            }
            //Joran 2021-10-12 进行cookie重写解决，重启后首次登录成功后提示登录超时问题end
            if (StringUtils.isNotEmpty(cookie.getName()) && "nav-style".equalsIgnoreCase(cookie.getName())) {
                indexStyle = cookie.getValue();
                break;
            }
        }
        iSysLoginService.refresh();

//        String webIndex = "topnav".equalsIgnoreCase(indexStyle) ? "index-topnav" : "index";
        //indexStyle值： default为index topnav为index-topnav null为默认
        String webIndex = "default".equalsIgnoreCase(indexStyle) ? "index" : "index-topnav";
        return webIndex;
    }

    // 锁定屏幕
    @GetMapping("/lockscreen")
    public String lockscreen(ModelMap mmap)
    {
        mmap.put("user", AuthorizationUtils.getSysUser());
        ServletUtils.getSession().setAttribute(ShiroConstants.LOCK_SCREEN, true);
        return "lock";
    }

    // 解锁屏幕
    @PostMapping("/unlockscreen")
    @ResponseBody
    public AjaxResult unlockscreen(String password)
    {
        SysUser user = AuthorizationUtils.getSysUser();
        if (StringUtils.isNull(user))
        {
            return AjaxResult.error("服务器超时，请重新登陆");
        }
        if (passwordService.matches(user, password))
        {
            ServletUtils.getSession().removeAttribute(ShiroConstants.LOCK_SCREEN);
            return AjaxResult.success();
        }
        return AjaxResult.error("密码不正确，请重新输入。");
    }

    // 切换主题
    @GetMapping("/system/switchSkin")
    public String switchSkin() {
        return "skin";
    }

    // 切换菜单
    @GetMapping("/system/menuStyle/{style}")
    public void menuStyle(@PathVariable String style, HttpServletResponse response) {
        CookieUtils.setCookie(response, "nav-style", style);
    }

    // 系统介绍
    @GetMapping("/system/main")
    public String main(ModelMap mmap) {
        mmap.put("version", Global.getVersion());
        return "main_v1";
    }


    /***
     * 加载系统清单，从缓存中取值，没有就访问接口，
     * 按用户session保存
     * @return
     */
    private List<MdmSystemData> getSystemData() {
        String key = SYSTEM_DATA_KEY_PREFIX + AuthorizationUtils.getSessionId();
        Object obj = CacheUtils.get(key);
        List<MdmSystemData> allSystem = null;
        if (StringUtils.isNotNull(obj)) {
            allSystem = (List<MdmSystemData>) obj;
        } else {
            R<List<MdmSystemData>> result = iSysLoginService.getSystemList();

            if (result.getCode() == HttpStatus.SUCCESS) {
                allSystem = result.getData();
                //TODO:LOGOUT的时候，要清理掉，包括超时的清理情况
                CacheUtils.put(key, allSystem);
            } else {
                logger.error(I18nUtil.getMessage("ui.biz.index.getsystems.fail"), result.getCode(), result.getMsg());
            }
        }

        //过滤出用户有的菜单数据
        if(StringUtils.isNotNull(allSystem)){
            Set<String> inUse = AuthorizationUtils.getSystemCode();
            List<MdmSystemData> newResult = allSystem.stream()
                    .filter(item -> inUse.contains(item.getSystemCode()))
                    .collect(Collectors.toList());

            newResult.stream()
                .forEach(one ->{
                    //根据语言包选择显示的系统名称
                    one.setShowName(StringUtils.getLocaleName(one.getLangJson(), I18nUtil.getLocaleFromRedis(), one.getSystemCode()));
                });
            allSystem = newResult;
        }
        return allSystem;
    }

    /**
     * 第一次进入时进行请求后端权限接口
     */
    public void reloadRemoteLoginUser() {
        Long userId = AuthorizationUtils.getUserId();

        LoginUser remoteLoginUser = (LoginUser) globalSetting.getKey(CacheConstants.RELOAD_LOGIN_USER_PREFIX + userId);
        if (StringUtils.isNull(remoteLoginUser)) {
            AjaxResult ajaxResult = iSysUserService.getUserAuth(userId);
            if (com.ruoyi.common.utils.StringUtils.equals(String.valueOf(ajaxResult.get(Constants.CODE)), String.valueOf(HttpStatus.SUCCESS))) {
                remoteLoginUser = JSON.parseObject(JSON.toJSONString(ajaxResult.get(Constants.DATA)), LoginUser.class);
                globalSetting.setKey(CacheConstants.RELOAD_LOGIN_USER_PREFIX + userId, remoteLoginUser);
            }
        }
        AuthorizationUtils.setLoginUserUserDeptRoleL1(remoteLoginUser);
    }


    @RequestMapping("/toIndex")
    public String toIndex(ModelMap modelMap){
        modelMap.put("lang",AuthorizationUtils.getLang());
        return "toIndex";
    }

    public String imageToBase64(String imgPath) {
        byte[] data = null;
        try {
            data = iApsFileService.downloadByteFile(imgPath, "image");
            BASE64Encoder encode = new BASE64Encoder();
            return encode.encode(data);
        } catch (Exception e) {
            return "";
        } finally {
        }

    }

}
