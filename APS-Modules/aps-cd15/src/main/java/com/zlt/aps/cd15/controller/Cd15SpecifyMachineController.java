package com.zlt.aps.cd15.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15SpecifyMachineDto;
import com.zlt.aps.cd15.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.service.Cd15SpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"15度裁断定点机台接口"})
@RestController
@RequestMapping("/specifyMachine")
public class Cd15SpecifyMachineController extends BaseController {

    @Resource
    private Cd15SpecifyMachineService cd15SpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @GetMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(Cd15SpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15SpecifyMachineDto> list = cd15SpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15SpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        Cd15SpecifyMachineDto dto = new Cd15SpecifyMachineDto();
        BeanUtils.copyProperties(cd15SpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd15.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody Cd15SpecifyMachineDto dto) {
        Cd15SpecifyMachine entity = new Cd15SpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        cd15SpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        cd15SpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        cd15SpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<Cd15SpecifyMachineDto> exportData(Cd15SpecifyMachineDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd15SpecifyMachineDto> list = cd15SpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.cd15.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断定点机台信息")
    public AjaxResult importData(@RequestBody List<Cd15SpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return cd15SpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
