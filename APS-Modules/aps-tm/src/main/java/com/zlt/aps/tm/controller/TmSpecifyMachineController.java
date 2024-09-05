package com.zlt.aps.tm.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tm.api.domain.dto.TmSpecifyMachineDto;
import com.zlt.aps.tm.entity.TmSpecifyMachine;
import com.zlt.aps.tm.service.TmSpecifyMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = {"胎面定点机台接口"})
@RestController
@RequestMapping("/specifyMachine")
public class TmSpecifyMachineController extends BaseController {

    @Resource
    private TmSpecifyMachineService tmSpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @GetMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(TmSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TmSpecifyMachineDto> list = tmSpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TmSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        TmSpecifyMachineDto dto = new TmSpecifyMachineDto();
        BeanUtils.copyProperties(tmSpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tm.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody TmSpecifyMachineDto dto) {
        TmSpecifyMachine entity = new TmSpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        tmSpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        tmSpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        tmSpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.tm.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @GetMapping("/exportData")
    public List<TmSpecifyMachineDto> exportData(TmSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TmSpecifyMachineDto> list = tmSpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.tm.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入胎面定点机台信息")
    public AjaxResult importData(@RequestBody List<TmSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tmSpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
