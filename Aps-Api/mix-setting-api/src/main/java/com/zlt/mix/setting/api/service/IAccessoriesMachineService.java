package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Service接口
 *
 * @author Liam
 * @date 2022-04-18
 */
@FeignClient(contextId = "IAccessoriesMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IAccessoriesMachineService {

    /**
     * 查询硫磺辅料与机台对应列表
     */
    @PostMapping("/accessoriesMachine/list")
    TableDataInfo listAccessoriesMachine(@RequestBody AccessoriesMachine accessoriesMachine);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/accessoriesMachine/{id}")
    AccessoriesMachine getAccessoriesMachineInfo(@PathVariable("id") Long id);

    /**
     * 保存硫磺辅料与机台对应信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/accessoriesMachine/save")
    AjaxResult saveAccessoriesMachine(@RequestBody AccessoriesMachine accessoriesMachine);

    /**
     * 批量删除硫磺辅料与机台对应
     */
    @PostMapping("/accessoriesMachine/delete/{ids}")
    AjaxResult deleteAccessoriesMachine(@PathVariable("ids") Long[] ids);

    /**
     * 校验硫磺辅料与机台对应唯一性
     */
    @ApiOperation("校验硫磺辅料与机台对应唯一性")
    @PostMapping("/accessoriesMachine/checkAccessoriesMachineUnique")
    String checkAccessoriesMachineUnique(@RequestBody AccessoriesMachine accessoriesMachine);

    /**
     * 导出硫磺辅料与机台对应列表
     */
    @PostMapping("/accessoriesMachine/exportData")
    List<AccessoriesMachine> exportData(@RequestBody AccessoriesMachine accessoriesMachine);

    /**
     * 导入硫磺辅料与机台对应数据
     */
    @ApiOperation("导入硫磺辅料与机台对应")
    @PostMapping("/accessoriesMachine/importData")
    public AjaxResult importData(@RequestBody List<AccessoriesMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据密炼区和胶料名称查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/accessoriesMachine/getAccessoriesMachineList")
    public ArrayList<AccessoriesMachine> getAccessoriesMachineList(@RequestBody AccessoriesMachine accessoriesMachine);

    /**
     * 根据密炼区和胶料名称查询机台信息
     *
     * @param accessoriesMachine 硫磺辅料与机台对应对象
     * @return 硫磺辅料与机台对应对象列表
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/accessoriesMachine/listRecipeMachine")
    public ArrayList<AccessoriesMachine> listRecipeMachine(@RequestBody AccessoriesMachine accessoriesMachine);
}
