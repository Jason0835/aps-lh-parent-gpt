package com.zlt.aps.dj.controller;


import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.dj.api.domain.dto.DjSpecifyMachineDto;
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;
import com.zlt.aps.dj.service.DjSpecifyMachineService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(tags = {"垫胶定点机台接口"})
@RestController
@RequestMapping("/dj/specifyMachine")
public class DjSpecifyMachineController extends BaseController {

    @Resource
    private DjSpecifyMachineService NcSpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @PostMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(@RequestBody DjSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjSpecifyMachineDto> list = NcSpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public DjSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        DjSpecifyMachineDto dto = new DjSpecifyMachineDto();
        BeanUtils.copyProperties(NcSpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody DjSpecifyMachineDto dto) {
        DjSpecifyMachine entity = new DjSpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        NcSpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        NcSpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        NcSpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<DjSpecifyMachineDto> exportData(@RequestBody DjSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<DjSpecifyMachineDto> list = NcSpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.nc.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入垫胶定点机台信息")
    public AjaxResult importData(@RequestBody List<DjSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return NcSpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
