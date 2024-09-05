package com.ruoyi.web.controller.system;

import com.ruoyi.api.gateway.system.domain.SysUserRole;
import com.ruoyi.api.gateway.system.service.ISysRoleService;
import com.ruoyi.api.gateway.system.service.ISysUserService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.core.controller.BaseController;
import com.ruoyi.common.text.Convert;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 角色信息
 * 
 * @author ruoyi
 */
@Controller
@RequestMapping("/system/role")
public class SysRoleController extends BaseController
{
    private String prefix = "system/role";

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysUserService userService;


    @RequiresPermissions("system:role:view")
    @GetMapping()
    public String role()
    {
        return prefix + "/role";
    }

    @RequiresPermissions("system:role:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SysRole role)
    {
        return roleService.list(role);
    }

    @ApiOperation("导出角色管理")
    @RequiresPermissions("system:role:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, SysRole entity) throws IOException {
        List<SysRole> list = roleService.totalList(entity);
        ExcelUtil<SysRole> util = new ExcelUtil(SysRole.class);
        util.exportExcel(response, list, I18nUtil.getMessage("ui.role.info.export.sheetName"), I18nUtil.getMessage("ui.role.info.export.sheetName"));
    }

   /* @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:role:export")
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        return util.exportExcel(list, "角色数据");
    }*/

    /**
     * 新增角色
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }

    /**
     * 新增保存角色
     */
    @RequiresPermissions("system:role:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(SysRole role)
    {
        return roleService.add(role);

    }

    /**
     * 修改角色
     */
    @GetMapping("/edit/{roleId}")
    public String edit(@PathVariable("roleId") Long roleId, ModelMap mmap)
    {
        mmap.put("role", roleService.selectRoleById(roleId));
        return prefix + "/edit";
    }

    /**
     * 修改保存角色
     */
    @RequiresPermissions("system:role:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SysRole role)
    {
        return roleService.edit(role);
    }

    /**
     * 角色分配数据权限
     */
    @GetMapping("/authDataScope/{roleId}")
    public String authDataScope(@PathVariable("roleId") Long roleId, ModelMap mmap)
    {
        mmap.put("role", roleService.selectRoleById(roleId));
        return prefix + "/dataScope";
    }

    /**
     * 保存角色分配数据权限
     */
    @RequiresPermissions("system:role:edit")
    @PostMapping("/authDataScope")
    @ResponseBody
    public AjaxResult authDataScopeSave(SysRole role)
    {
        return roleService.dataScope(role);
    }

    @RequiresPermissions("system:role:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        Long[] arr= Convert.toLongArray(ids);
       return roleService.remove(arr);
    }

    /**
     * 校验角色名称
     */
    @PostMapping("/checkRoleNameUnique")
    @ResponseBody
    public String checkRoleNameUnique(SysRole role)
    {
        return roleService.checkRoleNameUnique(role);
    }
    /**
     * 校验角色权限
     */
    @PostMapping("/checkRoleKeyUnique")
    @ResponseBody
    public String checkRoleKeyUnique(SysRole role)
    {
        return roleService.checkRoleKeyUnique(role);
    }

    /**
     * 选择菜单树
     */
    @GetMapping("/selectMenuTree")
    public String selectMenuTree()
    {
        return prefix + "/tree";
    }

    /**
     * 角色状态修改
     */
    @RequiresPermissions("system:role:edit")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(SysRole role)
    {
        return roleService.changeStatus(role);
    }

    /**
     * 分配用户
     */
    @RequiresPermissions("system:role:edit")
    @GetMapping("/authUser/{roleId}")
    public String authUser(@PathVariable("roleId") Long roleId, ModelMap mmap)
    {
        mmap.put("role", roleService.selectRoleById(roleId));
        return prefix + "/authUser";
    }

    /**
     * 查询已分配用户角色列表
     */
    @RequiresPermissions("system:role:list")
    @PostMapping("/authUser/allocatedList")
    @ResponseBody
    public TableDataInfo allocatedList(SysUser user)
    {
        return userService.allocatedList(user);
    }

    /**
     * 取消授权
     */
    @PostMapping("/authUser/cancel")
    @ResponseBody
    public AjaxResult cancelAuthUser(SysUserRole userRole)
    {
        return roleService.cancelAuthUser(userRole);
    }
    /**
     * 批量取消授权
     */
    @PostMapping("/authUser/cancelAll")
    @ResponseBody
    public AjaxResult cancelAuthUserAll(Long roleId, String userIds)
    {
        return roleService.cancelAuthUserAll(roleId, userIds);
    }

    /**
     * 选择用户
     */
    @GetMapping("/authUser/selectUser/{roleId}")
    public String selectUser(@PathVariable("roleId") Long roleId, ModelMap mmap)
    {
        mmap.put("role", roleService.selectRoleById(roleId));
        return prefix + "/selectUser";
    }

    /**
     * 查询未分配用户角色列表
     */
    @RequiresPermissions("system:role:list")
    @PostMapping("/authUser/unallocatedList")
    @ResponseBody
    public TableDataInfo unallocatedList(SysUser user)
    {
        return userService.unallocatedList(user);
    }

    /**
     * 批量选择用户授权
     */
    @PostMapping("/authUser/selectAll")
    @ResponseBody
    public AjaxResult selectAuthUserAll(Long roleId, String userIds)
    {
        return roleService.selectAuthUserAll(roleId, userIds);
    }
}