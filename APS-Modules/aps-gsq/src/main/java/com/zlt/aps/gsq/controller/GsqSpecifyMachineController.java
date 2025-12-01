package com.zlt.aps.gsq.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.dto.GsqSpecifyMachineDto;
import com.zlt.aps.gsq.entity.GsqSpecifyMachine;
import com.zlt.aps.gsq.service.GsqSpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"钢丝圈定点机台接口"})
@RestController
@RequestMapping("/gsq/specifyMachine")
public class GsqSpecifyMachineController extends BaseController {

    @Resource
    private GsqSpecifyMachineService GsqSpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @PostMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(@RequestBody GsqSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<GsqSpecifyMachineDto> list = GsqSpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public GsqSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        GsqSpecifyMachineDto dto = new GsqSpecifyMachineDto();
        BeanUtils.copyProperties(GsqSpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.gsq.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody GsqSpecifyMachineDto dto) {
        GsqSpecifyMachine entity = new GsqSpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        GsqSpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.gsq.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        GsqSpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.gsq.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        GsqSpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.gsq.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<GsqSpecifyMachineDto> exportData(@RequestBody GsqSpecifyMachineDto dto) {
        dto.setOrderStr(orderStr());
        List<GsqSpecifyMachineDto> list = GsqSpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.gsq.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入钢丝圈定点机台信息")
    public AjaxResult importData(@RequestBody List<GsqSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return GsqSpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
