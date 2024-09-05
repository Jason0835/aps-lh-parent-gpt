package com.ruoyi.system.controller;

import com.ruoyi.api.gateway.system.domain.SysMenu;
import com.ruoyi.api.gateway.system.domain.SysRole;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.api.gateway.system.domain.vo.RouterVo;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysMenuService;
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

import java.util.List;
import java.util.Set;

/**
 * 菜单信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/menu")
public class SysMenuController extends BaseController {
    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取菜单列表
     */
    @PreAuthorize(hasPermi = "system:menu:list")
    @GetMapping("/list")
    public AjaxResult list(SysMenu menu) {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(menu, userId);
        return AjaxResult.success(menus);
    }

    /**
     * 根据菜单编号获取详细信息
     */
    @PreAuthorize(hasPermi = "system:menu:query")
    @GetMapping(value = "/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId) {
        return AjaxResult.success(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysMenu menu) {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(menu, userId);
        return AjaxResult.success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     */
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId) {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuList(userId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    /**
     * 新增菜单
     */
    @PreAuthorize(hasPermi = "system:menu:add")
    @Log(title = "system.title.menumanage", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenu menu) {
        if (UserConstants.NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu))) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.exist.noadd"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }//vue.js
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame())
                && !StringUtils.startsWithAny(menu.getPath(), Constants.HTTP, Constants.HTTPS)) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.urlhttp.noadd"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }//bootui
        else if (UserConstants.TARGET.equals(menu.getTarget())
                && !StringUtils.startsWithAny(menu.getBtUrl(), Constants.HTTP, Constants.HTTPS)) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.urlhttp.noadd"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }
        menu.setCreateBy(SecurityUtils.getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     */
    @PreAuthorize(hasPermi = "system:menu:edit")
    @Log(title = "system.title.menumanage", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMenu menu) {
        if (UserConstants.NOT_UNIQUE.equals(menuService.checkMenuNameUnique(menu))) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.exist.noupdate"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }
        //vue.js
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame())
                && !StringUtils.startsWithAny(menu.getPath(), Constants.HTTP, Constants.HTTPS)) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.urlhttp.noupdate"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }
        //bootui
        else if (UserConstants.TARGET.equals(menu.getTarget())
                && !StringUtils.startsWithAny(menu.getBtUrl(), Constants.HTTP, Constants.HTTPS)) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.urlhttp.noupdate"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        } else if (menu.getMenuId().equals(menu.getParentId())) {
            String errMsg = StringUtils.format(I18nUtil.getMessage("system.error.menu.change.parentisown"), menu.getMenuName());
            return AjaxResult.error(errMsg);
        }
        menu.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 删除菜单
     */
    @PreAuthorize(hasPermi = "system:menu:remove")
    @Log(title = "system.title.menumanage", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId) {
        if (menuService.hasChildByMenuId(menuId)) {
            return AjaxResult.error(I18nUtil.getMessage("system.error.menu.childmenu.nonone"));
        }
        if (menuService.checkMenuExistRole(menuId)) {
            return AjaxResult.error(I18nUtil.getMessage("system.error.menu.childmenu.inused"));
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 获取路由信息
     *
     * @return 路由信息
     */
    @PostMapping("getRouters")
    public List<RouterVo> getRoutersList() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return menuService.buildMenus(menus);
    }

    /**
     * 获取菜单列表集合
     *
     * @param menu
     * @return
     */
    @PostMapping("/menuList")
    public List<SysMenu> menuList(@RequestBody SysMenu menu) {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menuList = menuService.selectMenuList(menu, userId);
        return menuList;
    }

    /**
     * 通过ID获取菜单
     *
     * @param menuId
     * @return
     */
    @PostMapping("/selectMenuById")
    public SysMenu selectMenuById(Long menuId) {
        return menuService.selectMenuById(menuId);
    }

    /**
     * 通过用户ID获取菜单权限
     *
     * @param userId
     * @return
     */
    @PostMapping("/selectMenuPermsByUserId")
    public Set<String> selectMenuPermsByUserId(Long userId) {
        return menuService.selectMenuPermsByUserId(userId);
    }

    /**
     * 加载角色菜单列表树
     */
    @PostMapping("/roleMenuTreeData")
    public List<Ztree> roleMenuTreeData(@RequestBody SysRole role) {
        Long userId = SecurityUtils.getUserId();
        List<Ztree> ztrees = menuService.roleMenuTreeData(role, userId);
        return ztrees;
    }

    /**
     * 加载所有菜单列表树
     *
     * @return
     */
    @PostMapping("/menuTreeData")
    public List<Ztree> menuTreeData() {
        Long userId = SecurityUtils.getUserId();
        List<Ztree> ztrees = menuService.menuTreeData(userId);
        return ztrees;
    }

    /**
     * 校验菜单名称
     */
    @PostMapping("/checkMenuNameUnique")
    public String checkMenuNameUnique(@RequestBody SysMenu menu) {
        return menuService.checkMenuNameUnique(menu);
    }
}