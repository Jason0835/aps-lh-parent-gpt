package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.SysPost;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.SysUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户信息对外暴露接口
 */
@FeignClient(contextId = "iSysUserService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysUserService {

    /**
     * 获取用户列表
     * @param user
     * @return
     */
    @GetMapping("/user/list")
    TableDataInfo list(@SpringQueryMap SysUser user);
    /**
     * 获取用户列表
     * @param user
     * @return
     */
    @GetMapping("/user/totalList")
    List<SysUser> totalList(@SpringQueryMap SysUser user);

//    /**
//     * 获取当前用户信息
//     * @param username
//     * @return
//     */
//    @GetMapping("/user/info/{username}")
//    R<LoginUser> info(@PathVariable("username") String username);

//    /**
//     * 获取用户信息
//     * @return
//     */
//    @GetMapping("/user/getInfo")
//    AjaxResult getInfo();

    /**
     * 通过用户ID 取权限，面向每个独立的系统
     * @param userId
     * @return
     */
    @PostMapping("/user/info/auth/{userId}")
    AjaxResult getUserAuth(@PathVariable("userId") Long userId);

    /**
     * 根据用户编号获取详细信息
     * @param userId
     * @return
     */
    @GetMapping(value = {"/user/{userId}" })
    AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId);

    /**
     * 新增用户
     * @param user
     * @return
     */
    @PostMapping("/user")
    AjaxResult add(@Validated @RequestBody SysUser user);

    /**
     * 修改用户
     * @param user
     * @return
     */
    @PutMapping("/user")
    AjaxResult edit(@Validated @RequestBody SysUser user);

    /**
     * 删除用户
     * @param userIds
     * @return
     */
    @DeleteMapping("/user/{userIds}")
    AjaxResult remove(@PathVariable("userIds") Long[] userIds);

    /**
     * 重置密码
     * @param user
     * @return
     */
    @PutMapping("/user/resetPwd")
    AjaxResult resetPwd(@RequestBody SysUser user);

    /**
     * 状态修改
     * @param user
     * @return
     */
    @PutMapping("/user/changeStatus")
    AjaxResult changeStatus(@RequestBody SysUser user);

    /**
     * 根据用户ID获取用户信息
     * @param userId
     * @return
     */
    @PostMapping("/user/selectUserById")
    SysUser selectUserById(@RequestParam("userId") Long userId);

    /**
     * 获取全部角色
     * @return
     */
    @PostMapping("/user/selectRoleAll")
    List<SysRole> selectRoleAll();

    /**
     * 获取全部岗位
     * @return
     */
    @PostMapping("/user/selectPostAll")
    List<SysPost> selectPostAll();

    @PostMapping("/user/checkUserNameUnique")
    String checkUserNameUnique(@RequestBody SysUser user);

    @PostMapping("/user/checkPhoneUnique")
    String checkPhoneUnique(@RequestBody SysUser user);

    @PostMapping("/user/checkEmailUnique")
    String checkEmailUnique(@RequestBody SysUser user);

    /**
     * 新增用户权限关系
     * @param userId
     * @param roleIds
     * @return
     */
    @PostMapping("/user/insertAuthRole")
    AjaxResult insertAuthRole(@RequestParam("userId") Long userId, @RequestParam("roleIds") Long[] roleIds);

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @PostMapping("/user/selectAllocatedList")
    List<SysUser> selectAllocatedList(@RequestBody SysUser user);

    /**
     * 查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @PostMapping("/user/authUser/allocatedList")
    TableDataInfo allocatedList(@RequestBody SysUser user);

    /**
     * 查询未分配用户角色列表
     */
    @PostMapping("/user/authUser/unallocatedList")
    TableDataInfo unallocatedList(@RequestBody SysUser user);
}
