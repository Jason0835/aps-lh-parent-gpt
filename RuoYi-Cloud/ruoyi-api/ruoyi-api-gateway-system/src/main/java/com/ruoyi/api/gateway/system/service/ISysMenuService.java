package com.ruoyi.api.gateway.system.service;


import com.ruoyi.api.gateway.system.domain.SysMenu;
import com.ruoyi.api.gateway.system.domain.Ztree;
import com.ruoyi.api.gateway.system.domain.vo.RouterVo;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.SysRole;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 菜单信息对外暴露接口
 */
@FeignClient(contextId = "iSysMenuService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.system:system}")
public interface ISysMenuService {

    /**
     * 获取菜单列表
     * @param menu
     * @return
     */
    @GetMapping("/menu/list")
    AjaxResult list(@SpringQueryMap SysMenu menu);

    /**
     * 根据菜单编号获取详细信息
     * @param menuId
     * @return
     */
    @GetMapping(value = "/menu/{menuId}")
    AjaxResult getInfo(@PathVariable("menuId") Long menuId);

    /**
     * 获取菜单下拉树列表
     * @param menu
     * @return
     */
    @GetMapping("/menu/treeselect")
    AjaxResult treeselect(@SpringQueryMap SysMenu menu);

    /**
     * 加载对应角色菜单列表树
     * @param roleId
     * @return
     */
    @GetMapping(value = "/menu/roleMenuTreeselect/{roleId}")
    AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId);

    /**
     * 新增菜单
     * @param menu
     * @return
     */
    @PostMapping("/menu")
    AjaxResult add(@Validated @RequestBody SysMenu menu);

    /**
     * 修改菜单
     * @param menu
     * @return
     */
    @PutMapping("/menu")
    AjaxResult edit(@Validated @RequestBody SysMenu menu);

    /**
     * 删除菜单
     * @param menuId
     * @return
     */
    @DeleteMapping("/menu/{menuId}")
    AjaxResult remove(@PathVariable("menuId") Long menuId);

    /**
     * 获取路由信息
     * @return
     */
    @GetMapping("/menu/getRouters")
    AjaxResult getRouters();

    @PostMapping("/menu/getRouters")
    List<RouterVo> getRoutersList();

    /**
     * 获取菜单列表
     * @param menu
     * @return
     */
    @PostMapping("/menu/menuList")
    List<SysMenu> menuList(@RequestBody SysMenu menu);

    /**
     * 通过菜单ID获取菜单信息
     * @param menuId
     * @return
     */
    @PostMapping("/menu/selectMenuById")
    SysMenu selectMenuById(@RequestParam("menuId") Long menuId);

    /**
     * 通过用户ID获取所有菜单权限
     * @param userId
     * @return
     */
    @PostMapping("/menu/selectMenuPermsByUserId")
    Set<String> selectMenuPermsByUserId(@RequestParam("userId") Long userId);

    /**
     * 加载角色菜单列表树
     */
    @PostMapping("/menu/roleMenuTreeData")
    List<Ztree> roleMenuTreeData(@RequestBody SysRole role);

    @PostMapping("/menu/menuTreeData")
    List<Ztree> menuTreeData();

    /**
     * 校验菜单名称
     */
    @PostMapping("/menu/checkMenuNameUnique")
    String checkMenuNameUnique(@RequestBody SysMenu menu);
}
