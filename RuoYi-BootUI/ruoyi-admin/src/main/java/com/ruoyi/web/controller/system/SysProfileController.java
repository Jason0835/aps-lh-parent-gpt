package com.ruoyi.web.controller.system;

import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.api.gateway.system.service.ISysProfileService;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.CryptUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.config.Global;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common4ui.utils.file.FileUploadUtils;
import com.ruoyi.file.api.service.IApsFileService;
import com.zlt.framework.utils.AuthorizationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 个人信息 业务处理
 *
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {
    private static final Logger log = LoggerFactory.getLogger(SysProfileController.class);

    private String prefix = "system/user/profile";

    @Autowired
    private ISysProfileService sysProfileService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IApsFileService iApsFileService;

    @Resource
    private ISysConfigService configService;

    /**
     * 个人信息
     */
    @GetMapping()
    public String profile(ModelMap mmap) {
        SysUser user = AuthorizationUtils.getSysUser();
        if(StringUtils.isNotEmpty(user.getAvatar())){
            String imgEncode="data:image/jpg;base64,"+imageToBase64(user.getAvatar());
            user.setAvatar(imgEncode);
        }
        mmap.put("user", user);
       /* mmap.put("roleGroup", userService.selectUserRoleGroup(user.getUserId()));
        mmap.put("postGroup", userService.selectUserPostGroup(user.getUserId()));*/
       /* AjaxResult result=sysProfileService.profile();
        if(result!=null){
                if(result.containsKey("data")){
                    SysUser user= (SysUser) result.get("data");
                    mmap.put("user",user);
            }*/
        AjaxResult result = sysProfileService.profile();
        mmap.put("roleGroup", result.get("roleGroup"));
        mmap.put("postGroup", result.get("postGroup"));
        //}
        return prefix + "/profile";
    }

    @GetMapping("/checkPassword")
    @ResponseBody
    public boolean checkPassword(String password) {
        SysUser user = AuthorizationUtils.getSysUser();
        SysUser localUser = userService.selectUserById(user.getUserId());
        String userPassword = localUser.getPassword();
        if (CryptUtils.matchesPassword(password, userPassword)) {
            return true;
        }
        return false;
    }

    @GetMapping("/resetPwd")
    public String resetPwd(ModelMap mmap) {
        SysUser user = AuthorizationUtils.getSysUser();
        String minLength = configService.selectConfigByKey("sys.password.min.length");  //从系统参数中获取密码最小长度设置
        minLength = StringUtils.isBlank(minLength) ? "8" : minLength;
        mmap.put("passwordMinLength", minLength);
        mmap.put("user", user);
        mmap.put("passwordTip", StringUtils.format(I18nUtil.getMessage("system.error.login.input.verification"),minLength));
        return prefix + "/resetPwd";
    }

    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwd(String oldPassword, String newPassword) {
        String minLength = configService.selectConfigByKey("sys.password.min.length");  //从系统参数中获取密码最小长度设置
        minLength = StringUtils.isBlank(minLength) ? "8" : minLength;
        String pwPattern = "^(?![A-Za-z0-9]+$)(?![A-Za-z\\W]+$)(?![0-9\\W]+$)[a-zA-Z0-9\\W]{" + minLength + ",}$";
        if(!newPassword.matches(pwPattern)) {
            ////密码要求由数字、字母、特殊字符组成，并且密码长度不得少于minLength位
            String msg=StringUtils.format(I18nUtil.getMessage("system.error.login.input.verification"),minLength);
            return AjaxResult.error(msg);
        }
        return sysProfileService.updatePwd(oldPassword, newPassword);
    }

    /**
     * 修改用户
     */
    /*@GetMapping("/edit")
    public String edit(ModelMap mmap)
    {
        SysUser user = ShiroUtils.getSysUser();
        mmap.put("user", userService.selectUserById(user.getUserId()));
        return prefix + "/edit";
    }*/

    /**
     * 修改头像
     */
    @GetMapping("/avatar")
    public String avatar(ModelMap mmap) {
        SysUser user = AuthorizationUtils.getSysUser();
        user=userService.selectUserById(user.getUserId());
        if(StringUtils.isNotEmpty(user.getAvatar())){
            String imgEncode="data:image/jpg;base64,"+imageToBase64(user.getAvatar());
            user.setAvatar(imgEncode);
        }

        mmap.put("user", user);
        return prefix + "/avatar";
    }

    /**
     * 修改用户
     */
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(SysUser user) {
        SysUser currentUser = AuthorizationUtils.getSysUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());

        AjaxResult result = sysProfileService.updateProfile(currentUser);

        if (StringUtils.equals(String.valueOf(result.get(Constants.CODE)), String.valueOf(HttpStatus.SUCCESS))) {
            AuthorizationUtils.setSysUser(userService.selectUserById(currentUser.getUserId()));
            return success();
        }
//TODO:I18N
        String errorMsg = StringUtils.format("更新用户信息失败:{}", result.get(GatewayConstants.MSG_TAG));
        log.error(errorMsg);
        return error(errorMsg);
    }

    /**
     * 保存头像
     */
    @PostMapping("/updateAvatar")
    @ResponseBody
    public AjaxResult updateAvatar(@RequestParam("avatarfile") MultipartFile file) {
        SysUser currentUser = AuthorizationUtils.getSysUser();
        try {
            if (!file.isEmpty()) {
                String avatar = null;
                try{
                    avatar= iApsFileService.uploadFile(file,"image");
                }catch (Exception e){
                    log.error("Image file upload error: ", e);
                }
                currentUser.setAvatar(avatar);
                sysProfileService.updateProfile(currentUser);
                AuthorizationUtils.setSysUser(userService.selectUserById(currentUser.getUserId()));
                return AjaxResult.success();
            }
            return error();
        } catch (Exception e) {
            log.error("修改头像失败！", e);
            return error(e.getMessage());
        }
    }

    public  String imageToBase64(String imgPath) {
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
