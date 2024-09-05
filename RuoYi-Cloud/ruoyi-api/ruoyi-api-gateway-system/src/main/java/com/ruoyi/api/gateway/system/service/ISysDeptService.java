package com.ruoyi.api.gateway.system.service;

import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.api.gateway.system.domain.SysRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理对外暴露接口
 */
@FeignClient(contextId = "iSysDeptService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysDeptService {

    /**
     * 获取部门列表
     * @param dept
     * @return
     */
    @GetMapping("/dept/list")
    AjaxResult list(@SpringQueryMap SysDept dept);

    /**
     * 查询部门列表（排除节点）
     * @param deptId
     * @return
     */
    @GetMapping("/dept/list/exclude/{deptId}")
    AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId);

    /**
     * 根据部门编号获取详细信息
     * @param deptId
     * @return
     */
    @GetMapping(value = "/dept/{deptId}")
    AjaxResult getInfo(@PathVariable("deptId") Long deptId);

    /**
     * 获取部门下拉树列表
     * @param dept
     * @return
     */
    @GetMapping("/dept/treeselect")
    AjaxResult treeselect(@SpringQueryMap @RequestBody SysDept dept);

    /**
     * 加载对应角色部门列表树
     * @param roleId
     * @return
     */
    @GetMapping(value = "/dept/roleDeptTreeselect/{roleId}")
    AjaxResult roleDeptTreeselect(@PathVariable("roleId") Long roleId);

    /**
     * 新增部门
     * @param dept
     * @return
     */
    @PostMapping("/dept")
    AjaxResult add(@Validated @RequestBody SysDept dept);

    /**
     * 修改部门
     * @param dept
     * @return
     */
    @PutMapping("/dept")
    AjaxResult edit(@Validated @RequestBody SysDept dept);

    /**
     * 删除部门
     * @param deptId
     * @return
     */
    @DeleteMapping("/dept/{deptId}")
    AjaxResult remove(@PathVariable("deptId") Long deptId);

    /**
     * 获取部门信息列表
     * @param dept
     * @return
     */
    @GetMapping("/dept/deptList")
    List<SysDept> deptList(@SpringQueryMap SysDept dept);

    /**
     * 根据部门ID获取部门信息
     * @param deptId
     * @return
     */
    @PostMapping("/dept/selectDeptById")
    SysDept selectDeptById(@RequestParam("deptId") Long deptId);

    /**
     * 获取部门树形结构
     * @return
     */
    @GetMapping("/dept/treeData")
    List<Ztree> treeData();

    /**
     * 加载部门列表树（排除下级）
     * @param excludeId
     * @return
     */
    @GetMapping("/dept/treeData/{excludeId}")
    List<Ztree> treeDataExcludeChild(@PathVariable(value = "excludeId", required = false) Long excludeId);

    /**
     * 加载角色部门（数据权限）列表树
     * @param role
     * @return
     */
    @PostMapping("/dept/roleDeptTreeData")
     List<Ztree> deptTreeData(@RequestBody SysRole role);

    /**
     * 校验部门名称
     */
    @PostMapping("/dept/checkDeptNameUnique")
    public String checkDeptNameUnique(@RequestBody SysDept dept);

}
