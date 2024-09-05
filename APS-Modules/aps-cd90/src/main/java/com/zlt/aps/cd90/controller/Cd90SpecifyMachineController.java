package com.zlt.aps.cd90.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.dto.Cd90SpecifyMachineDto;
import com.zlt.aps.cd90.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.service.Cd90SpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"90度裁断定点机台接口"})
@RestController
@RequestMapping("/specifyMachine")
public class Cd90SpecifyMachineController extends BaseController {

    @Resource
    private Cd90SpecifyMachineService Cd90SpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @GetMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(Cd90SpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd90SpecifyMachineDto> list = Cd90SpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90SpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        Cd90SpecifyMachineDto dto = new Cd90SpecifyMachineDto();
        BeanUtils.copyProperties(Cd90SpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd90.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody Cd90SpecifyMachineDto dto) {
        Cd90SpecifyMachine entity = new Cd90SpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        Cd90SpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        Cd90SpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        Cd90SpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<Cd90SpecifyMachineDto> exportData(Cd90SpecifyMachineDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd90SpecifyMachineDto> list = Cd90SpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.cd90.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90SpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return Cd90SpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
