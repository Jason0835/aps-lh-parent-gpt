package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.SysUserRole;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 角色信息对外暴露接口
 */
@FeignClient(contextId = "iSysRoleService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysRoleService {


    @GetMapping("/role/list")
    TableDataInfo list(@SpringQueryMap SysRole role);

    @GetMapping("/role/totalList")
    List<SysRole> totalList(@SpringQueryMap SysRole role);

    /*@PostMapping("/role/export")
    void export(HttpServletResponse response, SysRole role) throws IOException;*/

    /**
     * 根据角色编号获取详细信息
     * @param roleId
     * @return
     */
    @GetMapping(value = "/role/{roleId}")
    AjaxResult getInfo(@PathVariable("roleId") Long roleId);

    /**
     * 新增角色
     * @param role
     * @return
     */
    @PostMapping("/role")
    AjaxResult add(@Validated @RequestBody SysRole role);

    /**
     * 修改角色
     * @param role
     * @return
     */
    @PutMapping("/role")
    AjaxResult edit(@Validated @RequestBody SysRole role);

    /**
     * 修改保存数据权限
     * @param role
     * @return
     */
    @PutMapping("/role/dataScope")
    AjaxResult dataScope(@RequestBody SysRole role);

    /**
     * 状态修改
     * @param role
     * @return
     */
    @PutMapping("/role/changeStatus")
    AjaxResult changeStatus(@RequestBody SysRole role);

    /**
     * 删除角色
     * @param roleIds
     * @return
     */
    @DeleteMapping("/role/{roleIds}")
    AjaxResult remove(@PathVariable("roleIds") Long[] roleIds);

    /**
     * 获取角色选择框列表
     * @return
     */
    @GetMapping("/role/optionselect")
    AjaxResult optionselect();

    /**
     * 根据用户ID获取权限
     * @param userId
     * @return
     */
    @PostMapping("/role/selectRolePermissionByUserId")
    Set<String> selectRolePermissionByUserId(@RequestParam("userId") Long userId);

    /**
     * 通过角色ID获取角色信息
     * @param roleId
     * @return
     */
    @GetMapping(value = "/role/selectRoleById/{roleId}")
    SysRole selectRoleById(@PathVariable("roleId") Long roleId);

    @PostMapping("/role/checkRoleNameUnique")
    String checkRoleNameUnique(@RequestBody SysRole role);

    /**
     * 校验角色权限
     */
    @PostMapping("/role/checkRoleKeyUnique")
    String checkRoleKeyUnique(@RequestBody SysRole role);

    /**
     * 根据用户ID获取角色列表
     * @param userId
     * @return
     */
    @PostMapping("/role/selectRolesByUserId")
    List<SysRole> selectRolesByUserId(@RequestParam("userId") Long userId);

    /**
     * 取消授权
     * @param userRole
     * @return
     */
    @PostMapping("/role/authUser/cancel")
    AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole);

    /**
     * 批量取消授权
     */
    @PostMapping("/role/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(@RequestParam("roleId") Long roleId, @RequestParam("userIds") String userIds);

    /**
     * 批量选择用户授权
     */
    @PostMapping("/role/authUser/selectAll")
    public AjaxResult selectAuthUserAll(@RequestParam("roleId") Long roleId, @RequestParam("userIds") String userIds);
}
