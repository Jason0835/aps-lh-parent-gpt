package com.ruoyi.system.controller;

import java.util.Iterator;
import java.util.List;

import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.api.gateway.system.domain.SysDept;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 部门信息
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/dept")
public class SysDeptController extends BaseController
{
    @Autowired
    private ISysDeptService deptService;

    /**
     * 获取部门列表
     */
    @PreAuthorize(hasPermi = "system:dept:list")
    @GetMapping("/list")
    public AjaxResult list(SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return AjaxResult.success(depts);
    }

    /**
     * 查询部门列表（排除节点）
     */
    @PreAuthorize(hasPermi = "system:dept:list")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId)
    {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        Iterator<SysDept> it = depts.iterator();
        while (it.hasNext())
        {
            SysDept d = (SysDept) it.next();
            if (d.getDeptId().intValue() == deptId
                    || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""))
            {
                it.remove();
            }
        }
        return AjaxResult.success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @PreAuthorize(hasPermi = "system:dept:query")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId)
    {
        return AjaxResult.success(deptService.selectDeptById(deptId));
    }

    /**
     * 获取部门下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return AjaxResult.success(deptService.buildDeptTreeSelect(depts));
    }

    /**
     * 加载对应角色部门列表树
     */
    @GetMapping(value = "/roleDeptTreeselect/{roleId}")
    public AjaxResult roleDeptTreeselect(@PathVariable("roleId") Long roleId)
    {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.buildDeptTreeSelect(depts));
        return ajax;
    }

    /**
     * 新增部门
     */
    @PreAuthorize(hasPermi = "system:dept:add")
    @Log(title = "system.title.deptmanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDept dept)
    {
        if (UserConstants.NOT_UNIQUE.equals(deptService.checkDeptNameUnique(dept)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.dept.exist.noadd"), dept.getDeptName());
            return AjaxResult.error(errMsg);
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        // 语言包为空则默认给语言包
        if (StringUtils.isBlank(dept.getLangJson())) {
            dept.setLangJson("[{\"zh_CN\":\""+ dept.getDeptName() + "\",\"en_US\":\""+ dept.getDeptName() + "\"}]");
        }
        return toAjax(deptService.insertDept(dept));
    }

    /**
     * 修改部门
     */
    @PreAuthorize(hasPermi = "system:dept:edit")
    @Log(title = "system.title.deptmanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDept dept)
    {
        if (UserConstants.NOT_UNIQUE.equals(deptService.checkDeptNameUnique(dept)))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.dept.exist.noupdatename") , dept.getDeptName());
            return AjaxResult.error(errMsg);
        }
        else if (dept.getParentId().equals(dept.getDeptId()))
        {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.dept.change.parentisown") , dept.getDeptName());
            return AjaxResult.error(errMsg);
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus())
                && deptService.selectNormalChildrenDeptById(dept.getDeptId()) > 0)
        {
            return AjaxResult.error(I18nUtil.getMessage("system.error.dept.childdept.inused"));
        }
        // 语言包为空则默认给语言包
        if (StringUtils.isBlank(dept.getLangJson())) {
            dept.setLangJson("[{\"zh_CN\":\""+ dept.getDeptName() + "\",\"en_US\":\""+ dept.getDeptName() + "\"}]");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    /**
     * 删除部门
     */
    @PreAuthorize(hasPermi = "system:dept:remove")
    @Log(title = "system.title.deptmanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return AjaxResult.error(I18nUtil.getMessage("system.error.dept.childdept.nonone"));
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return AjaxResult.error(I18nUtil.getMessage("system.error.dept.haveuser"));
        }
        return toAjax(deptService.deleteDeptById(deptId));
    }

    /**
     * 获取部门列表
     * @param dept
     * @return
     */
    @GetMapping("/deptList")
    public List<SysDept> deptList(SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return depts;
    }

    /**
     * 根据部门ID获取部门信息
     * @param deptId
     * @return
     */
    @PostMapping("/selectDeptById")
    public SysDept selectDeptById(Long deptId){
        return deptService.selectDeptById(deptId);
    }

    /**
     * 加载部门列表树
     */
    @GetMapping("/treeData")
    public List<Ztree> treeData()
    {
        List<Ztree> ztrees = deptService.selectDeptTree(new SysDept());
        return ztrees;
    }

    /**
     * 加载部门列表树（排除下级）
     * @param excludeId
     * @return
     */
    @GetMapping("/treeData/{excludeId}")
    public List<Ztree> treeDataExcludeChild(@PathVariable(value = "excludeId", required = false) Long excludeId)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(excludeId);
        List<Ztree> ztrees = deptService.selectDeptTreeExcludeChild(dept);
        return ztrees;
    }

    /**
     * 加载角色部门（数据权限）列表树
     * @param role
     * @return
     */
    @PostMapping("/roleDeptTreeData")
    public List<Ztree> deptTreeData(@RequestBody SysRole role)
    {
        List<Ztree> ztrees = deptService.roleDeptTreeData(role);
        return ztrees;
    }

    /**
     * 校验部门名称
     */
    @PostMapping("/checkDeptNameUnique")
    public String checkDeptNameUnique(@RequestBody SysDept dept){
        return deptService.checkDeptNameUnique(dept);
    }

    @PostMapping("/userRoleDeptLevel1")
    public R<List<SysDept>> getUserRoleDeptLevel1(@RequestBody List<SysRole> roles){
        return R.ok(deptService.selectRoleDeptList(roles));
    }
}
