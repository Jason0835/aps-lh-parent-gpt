package com.zlt.mix.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.zlt.mix.common.core.constant.BusinessConstant;
import com.zlt.mix.setting.api.domain.entity.AccessoriesMachine;
import com.zlt.mix.setting.service.AccessoriesMachineService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 硫磺辅料与机台对应Controller
 *
 * @author Liam
 * @date 2022-04-18
 */
@RestController
@RequestMapping("/accessoriesMachine")
public class AccessoriesMachineController extends BaseController {
    @Resource
    private AccessoriesMachineService accessoriesMachineService;

    /**
     * 查询硫磺辅料与机台对应列表
     */
    @ApiOperation("查询硫磺辅料与机台对应列表")
    @PostMapping("/list")
    public TableDataInfo listAccessoriesMachine(@RequestBody AccessoriesMachine accessoriesMachine) {
        startPage(false);
        accessoriesMachine.setOrderStr(orderStr());
        List<AccessoriesMachine> list = accessoriesMachineService.selectAccessoriesMachineList(accessoriesMachine);
        return getDataTable(list);
    }

    @ApiOperation("获取硫磺辅料与机台对应详细信息")
    @GetMapping(value = "/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public AccessoriesMachine getAccessoriesMachineInfo(@PathVariable("id") Long id) {
        AccessoriesMachine accessoriesMachine = accessoriesMachineService.getById(id);
        List<AccessoriesMachine> accessoriesMachines = accessoriesMachineService.selectExactAccessoriesMachineList(accessoriesMachine);
        if (CollectionUtils.isEmpty(accessoriesMachines)) {
            return new AccessoriesMachine();
        }
        return accessoriesMachines.get(0);
    }

    @Log(title = "setting.accessoriesMachine.modelName", newBusinessType = BusinessConstant.INSERT_OR_UPDATE)
    @ApiOperation("保存硫磺辅料与机台对应信息（id为空则新增，id不为空则修改）")
    @PostMapping("/save")
    public AjaxResult saveAccessoriesMachine(@RequestBody AccessoriesMachine accessoriesMachine) {
        Long id = accessoriesMachine.getId();
        accessoriesMachineService.saveAccessoriesMachine(accessoriesMachine);
        //返回ID和机台名称进行显示
        if (id != null) {
            AccessoriesMachine params = new AccessoriesMachine();
            params.setMixArea(accessoriesMachine.getMixArea());
            params.setMaterialName(accessoriesMachine.getMaterialName());
            List<AccessoriesMachine> accessoriesMachineList = exportData(params);
            if (accessoriesMachineList != null && !accessoriesMachineList.isEmpty()) {
                return AjaxResult.success(I18nUtil.getMessage("common.msg.ajax.operation.success"), accessoriesMachineList.get(0));
            }
        }
        return AjaxResult.success();
    }

    @Log(title = "setting.accessoriesMachine.modelName", newBusinessType = BusinessConstant.DELETE)
    @ApiOperation("删除硫磺辅料与机台对应")
    @PostMapping("/delete/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteAccessoriesMachine(@PathVariable Long[] ids) {
        return toAjax(accessoriesMachineService.deleteAccessoriesMachineByIds(ids));
    }

    @Log(title = "setting.accessoriesMachine.modelName", newBusinessType = BusinessConstant.EXPORT)
    @ApiOperation("导出硫磺辅料与机台对应列表")
    @PostMapping("/exportData")
    public List<AccessoriesMachine> exportData(@RequestBody AccessoriesMachine accessoriesMachine) {
        startPage(false);
        accessoriesMachine.setOrderStr(orderStr());  //拿到前端传的排序字段+排序方式
        return accessoriesMachineService.selectAccessoriesMachineList(accessoriesMachine);
    }

    @ApiOperation("校验硫磺辅料与机台对应唯一性（返回'0'表示唯一，返回'1'表示不唯一）")
    @PostMapping("/checkAccessoriesMachineUnique")
    public String checkAccessoriesMachineUnique(@RequestBody AccessoriesMachine accessoriesMachine) {
        return accessoriesMachineService.checkAccessoriesMachineUnique(accessoriesMachine);
    }

    @Log(title = "setting.accessoriesMachine.modelName", newBusinessType = BusinessConstant.IMPORT)
    @ApiOperation("导入硫磺辅料与机台对应数据")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "list", dataType = "list", value = "集合", paramType = "query"),
            @ApiImplicitParam(name = "updateSupport", dataType = "boolean", value = "已存在记录是否更新", paramType = "query"),
            @ApiImplicitParam(name = "importLogId", dataType = "int", value = "导入日志id", paramType = "query"),
    })
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<AccessoriesMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("import.nodata"));
        }
        return accessoriesMachineService.importData(list, updateSupport, importLogId);
    }

    /**
     * 根据密炼区和胶料名称查询机台信息
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/getAccessoriesMachineList")
    public ArrayList<AccessoriesMachine> getAccessoriesMachineList(@RequestBody AccessoriesMachine accessoriesMachine) {
        return accessoriesMachineService.getAccessoriesMachineList(accessoriesMachine);
    }

    /**
     * 根据密炼区和胶料名称查询配方机台列表
     * @param formulaMachine 密炼区、胶料名称
     * @return 查询出来的集合
     */
    @ApiOperation("根据密炼区和胶料名称查询机台信息")
    @PostMapping("/listRecipeMachine")
    public ArrayList<AccessoriesMachine> listRecipeMachine(@RequestBody AccessoriesMachine accessoriesMachine) {
    	return accessoriesMachineService.listRecipeMachine(accessoriesMachine);
    }
}
