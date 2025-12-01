package com.zlt.aps.gdyy.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gdyy.api.domain.dto.GdyySpecifyMachineDto;
import com.zlt.aps.gdyy.entity.GdyySpecifyMachine;
import com.zlt.aps.gdyy.service.GdyySpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"钢带压延定点机台接口"})
@RestController
@RequestMapping("/gdyy/specifyMachine")
public class GdyySpecifyMachineController extends BaseController {

    @Resource
    private GdyySpecifyMachineService GdyySpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @PostMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(@RequestBody GdyySpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GdyySpecifyMachineDto> list = GdyySpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GdyySpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        GdyySpecifyMachineDto dto = new GdyySpecifyMachineDto();
        BeanUtils.copyProperties(GdyySpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.gdyy.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody GdyySpecifyMachineDto dto) {
        GdyySpecifyMachine entity = new GdyySpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        GdyySpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.gdyy.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        GdyySpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.gdyy.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        GdyySpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.gdyy.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<GdyySpecifyMachineDto> exportData(@RequestBody GdyySpecifyMachineDto dto) {
        dto.setOrderStr(orderStr());
        List<GdyySpecifyMachineDto> list = GdyySpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.gdyy.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<GdyySpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return GdyySpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
