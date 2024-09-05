package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.SysPost;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.BaseException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.security.config.ApplicationSecurityConfig;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.api.RemoteAuthService;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.SysUser;
import com.ruoyi.api.gateway.system.model.LoginUser;
import com.ruoyi.system.service.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/user")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysPermissionService permissionService;

    @Autowired
    private ISysDeptService iSysDeptService;

    @Autowired
    private ISysRoleService iSysRoleService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    RemoteAuthService remoteAuthService;
    @Resource
    private ISysConfigService configService;

    @Autowired
    ApplicationSecurityConfig applicationSecurityConfig;
    /**
     * 获取用户列表
     */
    @PreAuthorize(hasPermi = "system:user:list")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }
    /**
     * 获取用户列表
     */
    @PreAuthorize(hasPermi = "system:user:list")
    @GetMapping("/totalList")
    public List<SysUser> totalList(SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        return list;
    }

    @Log(title = "system.title.usermanage", businessType = BusinessType.EXPORT)
    @PreAuthorize(hasPermi = "system:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user) throws IOException
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.userdata"));
    }

    @Log(title = "system.title.usermanage", businessType = BusinessType.IMPORT)
    @PreAuthorize(hasPermi = "system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return AjaxResult.success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, I18nUtil.getMessage("system.title.userdata"));
    }

    /**
     * 获取当前用户信息
     * 这个接口只用在非单点登录状态下使用。
     */
    @GetMapping("/info/{username}")
    public R<LoginUser> info(@PathVariable("username") String username)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser))
        {
            return R.fail(I18nUtil.getMessage("system.error.user.nofound"));
        }
        //20201214 linbn 把权限角色去除掉，统一前端的取值方法
        LoginUser sysUserVo = new LoginUser();
        sysUserVo.setSysUser(sysUser);
        return R.ok(sysUserVo);
    }

    @PostMapping("/userInfo")
    public R<LoginUser> userInfo(@RequestParam("username") String username)
    {
        return info(username);
    }

    /***
     * 单点登录状态下，登录的权限，每个系统自己抓取。
     * 这里不提供用户信息。
     * 20201214 linbn
     * @param userId
     * @return
     */
    @PostMapping("info/auth/{userId}")
    public AjaxResult getUserAuth(@PathVariable("userId") Long userId){

        // 角色集合
        Set<String> roles = permissionService.getRolePermission(userId);
        // 权限集合
        Set<String> permissions = permissionService.getMenuBtPermission(userId);

        //20201210 linbn
        //加入用户一级部门列表【工厂】
        List<SysDept> roleDepts = iSysDeptService.selectRoleDeptList(iSysRoleService.selectRolesByUserId(userId));

        AjaxResult ajax = AjaxResult.success();
        ajax.put(UserConstants.KEY_AUTH_ROLES, roles);
        ajax.put(UserConstants.KEY_AUTH_PERMISSIONS, permissions);
        ajax.put(UserConstants.KEY_AUTH_FACTORYS, roleDepts);
        ajax.put("token", tokenService.getLoginUser().getToken());
        ajax.put(CacheConstants.TOKEN_SYS_CODE, applicationSecurityConfig.CurrentCode());

        R<LoginUser> result = remoteAuthService.appendUserAuths(ajax );
        if(result.getCode() == HttpStatus.SUCCESS){
            LoginUser user = result.getData();
            return AjaxResult.success(user);
        }
        return AjaxResult.error(I18nUtil.getMessage("system.error.user.getUserAuth.fail"));
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        Long userId = SecurityUtils.getUserId();
        AjaxResult ajax = getUserAuth(userId);
        ajax.put("user", userService.selectUserById(userId));
        return ajax;
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize(hasPermi = "system:user:query")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put(UserConstants.KEY_AUTH_ROLES, SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        if (StringUtils.isNotNull(userId))
        {
            ajax.put(AjaxResult.DATA_TAG, userService.selectUserById(userId));
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", roleService.selectRoleListByUserId(userId));
        }
        return ajax;
    }

    /**
     * 新增用户
     */
    @PreAuthorize(hasPermi = "system:user:add")
    @Log(title = "system.title.usermanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        String minLength = configService.selectConfigByKey("sys.password.min.length");  //从系统参数中获取密码最小长度设置
        minLength = StringUtils.isBlank(minLength) ? "8" : minLength;
        String pwPattern = "^(?![A-Za-z0-9]+$)(?![A-Za-z\\W]+$)(?![0-9\\W]+$)[a-zA-Z0-9\\W]{" + minLength + ",}$";
        if(!user.getPassword().matches(pwPattern)) {
            ////密码要求由数字、字母、特殊字符组成，并且密码长度不得少于minLength位
            String msg=StringUtils.format(I18nUtil.getMessage("system.error.login.input.verification"),minLength);
            return AjaxResult.error(msg);
        } else if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(user.getUserName())))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.user.exist.noadd"), user.getUserName());
            return AjaxResult.error(errMsg);
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.user.telephone.exist.noadd"), user.getUserName());
            return AjaxResult.error(errMsg);
        }
        else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.user.email.exist.noadd") , user.getUserName());
            return AjaxResult.error(errMsg);
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize(hasPermi = "system:user:edit")
    @Log(title = "system.title.usermanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.user.telephone.exist.noupdate") , user.getUserName());
            return AjaxResult.error(errMsg);
        }
        else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.user.email.exist.noupdate"), user.getUserName());
            return AjaxResult.error(errMsg);
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize(hasPermi = "system:user:remove")
    @Log(title = "system.title.usermanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @PreAuthorize(hasPermi = "system:user:edit")
    @Log(title = "system.title.usermanage", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @PreAuthorize(hasPermi = "system:user:edit")
    @Log(title = "system.title.usermanage", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }


    /**
     * 根据用户ID获取用户信息
     * @param userId
     * @return
     */
    @PostMapping("/selectUserById")
    public SysUser selectUserById(Long userId)
    {
        return userService.selectUserById(userId);
    }

    /**
     * 获取全部角色
     * @return
     */
    @PostMapping("/selectRoleAll")
    public List<SysRole> selectRoleAll(){
        return  roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList());
    }

    /**
     * 获取全部岗位
     * @return
     */
    @PostMapping("/selectPostAll")
    public List<SysPost> selectPostAll(){
        return  postService.selectPostAll();
    }

    /**
     * 检查登录名称
     * @param user
     * @return
     */
    @PostMapping("/checkUserNameUnique")
    public String checkUserNameUnique(@RequestBody SysUser user)
    {
        return userService.checkUserNameUnique(user.getUserName());
    }

    /**
     * 检查手机号
     * @param user
     * @return
     */
    @PostMapping("/checkPhoneUnique")
    public String checkPhoneUnique(@RequestBody SysUser user)
    {
        return userService.checkPhoneUnique(user);
    }

    /**
     * 检查邮箱
     * @param user
     * @return
     */
    @PostMapping("/checkEmailUnique")
    public String checkEmailUnique(@RequestBody SysUser user)
    {
        return userService.checkEmailUnique(user);
    }

    /**
     * 用户授权角色
     * @param userId
     * @param roleIds
     * @return
     */
    @PostMapping("/insertAuthRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds){
        if(userId==null){
            return AjaxResult.error();
        }
        userService.insertAuthRole(userId,roleIds);
        return AjaxResult.success();
    }

    /**
     * 查询已分配用户角色列表
     * @param user
     * @return
     */
    @PostMapping("/selectAllocatedList")
    public List<SysUser> selectAllocatedList(@RequestBody SysUser user){
        return userService.selectAllocatedList(user);
    }

    /**
     * 查询已分配用户角色列表
     */
    @PostMapping("/authUser/allocatedList")
    public TableDataInfo allocatedList(@RequestBody SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectAllocatedList(user);
        return getDataTable(list);
    }

    /**
     * 查询未分配用户角色列表
     */
    @PostMapping("/authUser/unallocatedList")
    public TableDataInfo unallocatedList(@RequestBody SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUnallocatedList(user);
        return getDataTable(list);
    }


}
