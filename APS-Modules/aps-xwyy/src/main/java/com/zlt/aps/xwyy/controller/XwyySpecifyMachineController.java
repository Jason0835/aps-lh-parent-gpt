package com.zlt.aps.xwyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.xwyy.api.domain.dto.XwyySpecifyMachineDto;
import com.zlt.aps.xwyy.entity.XwyySpecifyMachine;
import com.zlt.aps.xwyy.service.XwyySpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"纤维压延定点机台接口"})
@RestController
@RequestMapping("/specifyMachine")
public class XwyySpecifyMachineController extends BaseController {

    @Resource
    private XwyySpecifyMachineService XwyySpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @GetMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(XwyySpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<XwyySpecifyMachineDto> list = XwyySpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public XwyySpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        XwyySpecifyMachineDto dto = new XwyySpecifyMachineDto();
        BeanUtils.copyProperties(XwyySpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.xwyy.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody XwyySpecifyMachineDto dto) {
        XwyySpecifyMachine entity = new XwyySpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        XwyySpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.xwyy.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        XwyySpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.xwyy.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        XwyySpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.xwyy.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<XwyySpecifyMachineDto> exportData(XwyySpecifyMachineDto dto) {
        dto.setOrderStr(orderStr());
        List<XwyySpecifyMachineDto> list = XwyySpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.xwyy.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<XwyySpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return XwyySpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
