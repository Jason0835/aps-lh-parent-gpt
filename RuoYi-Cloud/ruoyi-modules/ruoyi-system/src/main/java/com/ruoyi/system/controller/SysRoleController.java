package com.ruoyi.system.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.api.gateway.system.domain.SysUserRole;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.system.service.ISysRoleService;

/**
 * 角色信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/role")
public class SysRoleController extends BaseController
{
    @Autowired
    private ISysRoleService roleService;

    @PreAuthorize(hasPermi = "system:role:list")
    @GetMapping("/list")
    public TableDataInfo list(SysRole role)
    {
        startPage();
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }
    @PreAuthorize(hasPermi = "system:role:list")
    @GetMapping("/totalList")
    public List<SysRole> totalList(SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        return list;
    }

    @Log(title = "system.title.rolemanage", businessType = BusinessType.EXPORT)
    @PreAuthorize(hasPermi = "system:role:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysRole role) throws IOException
    {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        util.exportExcel(response, list, I18nUtil.getMessage("system.title.roledata"));
    }

    /**
     * 根据角色编号获取详细信息
     */
    @PreAuthorize(hasPermi = "system:role:query")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId)
    {
        return AjaxResult.success(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     */
    @PreAuthorize(hasPermi = "system:role:add")
    @Log(title = "system.title.rolemanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysRole role)
    {
        if (UserConstants.NOT_UNIQUE.equals(roleService.checkRoleNameUnique(role)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.role.exist.noadd"), role.getRoleName());
            return AjaxResult.error(errMsg);
        }
        else if (UserConstants.NOT_UNIQUE.equals(roleService.checkRoleKeyUnique(role)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.role.exist.code.noadd") , role.getRoleName());
            return AjaxResult.error(errMsg);
        }
        role.setCreateBy(SecurityUtils.getUsername());
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 修改保存角色
     */
    @PreAuthorize(hasPermi = "system:role:edit")
    @Log(title = "system.title.rolemanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        if (UserConstants.NOT_UNIQUE.equals(roleService.checkRoleNameUnique(role)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.role.exist.noupdate"), role.getRoleName());
            return AjaxResult.error(errMsg);
        }
        else if (UserConstants.NOT_UNIQUE.equals(roleService.checkRoleKeyUnique(role)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.role.exist.code.noupdate"), role.getRoleName());
            return AjaxResult.error(errMsg);
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRole(role));
    }

    /**
     * 修改保存数据权限
     */
    @PreAuthorize(hasPermi = "system:role:edit")
    @Log(title = "system.title.rolemanage", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    public AjaxResult dataScope(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 状态修改
     */
    @PreAuthorize(hasPermi = "system:role:edit")
    @Log(title = "system.title.rolemanage", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 删除角色
     */
    @PreAuthorize(hasPermi = "system:role:remove")
    @Log(title = "system.title.rolemanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds)
    {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色选择框列表
     */
    @PreAuthorize(hasPermi = "system:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return AjaxResult.success(roleService.selectRoleAll());
    }

    /**
     * 根据用户ID加载权限
     * @param userId
     * @return
     */
    @PostMapping("/selectRolePermissionByUserId")
    public Set<String> selectRolePermissionByUserId(Long userId){
        return this.roleService.selectRolePermissionByUserId(userId);
    }

    /**
     * 通过角色ID获取角色信息
     * @param roleId
     * @return
     */
    @GetMapping(value = "selectRoleById/{roleId}")
    public SysRole selectRoleById(@PathVariable Long roleId)
    {
        return roleService.selectRoleById(roleId);
    }

    /**
     * 校验角色名称
     * @param role
     * @return
     */
    @PostMapping("/checkRoleNameUnique")
    public String checkRoleNameUnique(@RequestBody SysRole role){
        return roleService.checkRoleNameUnique(role);
    }

    /**
     * 校验角色权限
     */
    @PostMapping("/checkRoleKeyUnique")
    public String checkRoleKeyUnique(@RequestBody SysRole role){
        return roleService.checkRoleKeyUnique(role);
    }

    /**
     * 根据用户ID获取角色列表
     * @param userId
     * @return
     */
    @PostMapping("/selectRolesByUserId")
    public List<SysRole> selectRolesByUserId(Long userId){
        return roleService.selectRolesByUserId(userId);
    }

    /**
     * 取消授权
     * @param userRole
     * @return
     */
    @PostMapping("/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole){
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 批量取消授权
     */
    @PostMapping("/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(Long roleId, String userIds)
    {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    /**
     * 批量选择用户授权
     */
    @PostMapping("/authUser/selectAll")
    public AjaxResult selectAuthUserAll(Long roleId, String userIds){
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }
}