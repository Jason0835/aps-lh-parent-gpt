package com.ruoyi.web.controller.system;

import com.ruoyi.api.gateway.system.service.*;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common.text.Convert;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    private String prefix = "system/user";

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    @Resource
    private ISysConfigService configService;

    @RequiresPermissions("system:user:view")
    @GetMapping()
    public String user()
    {
        return prefix + "/user";
    }

    @RequiresPermissions("system:user:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysUser user)
    {
        return userService.list(user);
    }


    @ApiOperation("导出用户管理")
    @RequiresPermissions("system:user:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysUser entity) throws IOException {
        List<SysUser> list = userService.totalList(entity);
        ExcelUtil<SysUser> util = new ExcelUtil(SysUser.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.user.info.export.sheetName"), I18nUtil.getMessage("ui.user.info.export.sheetName"));
    }



    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.user");
        ExcelUtil<SysUser> util = new ExcelUtil<>(SysUser.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
//
//    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
//    @RequiresPermissions("system:user:import")
//    @PostMapping("/importData")
//    @ResponseBody
//    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
//    {
//        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
//        List<SysUser> userList = util.importExcel(file.getInputStream());
//        String operName = ShiroUtils.getSysUser().getLoginName();
//        String message = userService.importUser(userList, updateSupport, operName);
//        return AjaxResult.success(message);
//    }
//
//    @RequiresPermissions("system:user:view")
//    @GetMapping("/importTemplate")
//    @ResponseBody
//    public AjaxResult importTemplate()
//    {
//        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
//        return util.importTemplateExcel("用户数据");
//    }

    /**
     * 新增用户
     */
    @GetMapping("/add")
    public String add(ModelMap mmap)
    {
        mmap.put("roles", userService.selectRoleAll());
        mmap.put("posts", userService.selectPostAll());
        String minLength = configService.selectConfigByKey("sys.password.min.length");  //从系统参数中获取密码最小长度设置
        minLength = StringUtils.isBlank(minLength) ? "8" : minLength;
        mmap.put("passwordMinLength", minLength);
        return prefix + "/add";
    }

    /**
     * 新增保存用户
     */
    @RequiresPermissions("system:user:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(SysUser user)
    {
        return userService.add(user);
    }

    /**
     * 修改用户
     */
    @GetMapping("/edit/{userId}")
    public String edit(@PathVariable("userId") Long userId, ModelMap mmap)
    {
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        mmap.put("user", userService.selectUserById(userId));
        mmap.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        mmap.put("posts", postService.selectPostsByUserId(userId));
        return prefix + "/edit";
    }

    /**
     * 修改保存用户
     */
    @RequiresPermissions("system:user:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SysUser user)
    {
        return userService.edit(user);
    }

    @RequiresPermissions("system:user:resetPwd")
    @GetMapping("/resetPwd/{userId}")
    public String resetPwd(@PathVariable("userId") Long userId, ModelMap mmap)
    {
        mmap.put("user", userService.selectUserById(userId));
        return prefix + "/resetPwd";
    }

    @RequiresPermissions("system:user:resetPwd")
    @PostMapping("/resetPwd")
    @ResponseBody
    public AjaxResult resetPwdSave(SysUser user)
    {
        return userService.resetPwd(user);
    }

    /**
     * 进入授权角色页
     */
    @GetMapping("/authRole/{userId}")
    public String authRole(@PathVariable("userId") Long userId, ModelMap mmap)
    {
        SysUser user = userService.selectUserById(userId);
        // 获取用户所属的角色列表
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        mmap.put("user", user);
        mmap.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return prefix + "/authRole";
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("system:user:add")
    @PostMapping("/authRole/insertAuthRole")
    @ResponseBody
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        return userService.insertAuthRole(userId, roleIds);
    }

    @RequiresPermissions("system:user:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
        return userService.remove(arr);
    }

    /**
     * 校验用户名
     */
    @PostMapping("/checkLoginNameUnique")
    @ResponseBody
    public String checkLoginNameUnique(SysUser user)
    {
        return userService.checkUserNameUnique(user);
    }
    /**
     * 校验手机号码
     */
    @PostMapping("/checkPhoneUnique")
    @ResponseBody
    public String checkPhoneUnique(SysUser user)
    {
        return userService.checkPhoneUnique(user);
    }

    /**
     * 校验email邮箱
     */
    @PostMapping("/checkEmailUnique")
    @ResponseBody
    public String checkEmailUnique(SysUser user)
    {
        return userService.checkEmailUnique(user);
    }

    /**
     * 用户状态修改
     */
    @RequiresPermissions("system:user:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(SysUser user)
    {
        return userService.changeStatus(user);
    }
}