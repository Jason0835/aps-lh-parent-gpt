package com.zlt.aps.cd90.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import com.zlt.aps.cd90.entity.Cd90MachineRollMapping;
import com.zlt.aps.cd90.service.Cd90MachineRollMappingService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-16
 */
@RestController
@RequestMapping("/cd90/MachineRollMapping")
public class Cd90MachineRollMappingController extends BaseController {
    @Resource
    public Cd90MachineRollMappingService machineRollMappingService;

    @ApiOperation("根据条件查询帘布大卷与机台的映射表列表")
    @PostMapping("/listMachineRollMapping")
    public TableDataInfo listMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd90MachineRollMappingDto> list = machineRollMappingService.listMachineRollMapping(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询帘布大卷与机台的映射表列表")
    @GetMapping("/getMachineRollMapping/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90MachineRollMappingDto getMachineRollMapping(@PathVariable("id") Long id) {
        Cd90MachineRollMappingDto dto = new Cd90MachineRollMappingDto();
        BeanUtils.copyProperties(machineRollMappingService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd90.MachineRollMapping.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存帘布大卷颜色提示信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveMachineRollMapping")
    public AjaxResult saveXwyyMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto) {
        Cd90MachineRollMapping entity = new Cd90MachineRollMapping();
        BeanUtils.copyProperties(dto, entity);
        machineRollMappingService.saveMachineRollMapping(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkMachineRollMapping")
    public String checkMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto) {
        return machineRollMappingService.checkMachineRollMapping(dto);
    }

    @Log(title = "ui.cd90.MachineRollMapping.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除帘布大卷颜色提示信息(逻辑删)")
    @PostMapping("/deleteMachineRollMapping/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMachineRollMapping(@PathVariable("ids") Long[] ids) {
        machineRollMappingService.deleteMachineRollMapping(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.MachineRollMapping.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        machineRollMappingService.deleteAll();
        return AjaxResult.success();
    }

    @Log(title = "ui.cd90.MachineRollMapping.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<Cd90MachineRollMappingDto> exportData(@RequestBody Cd90MachineRollMappingDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd90MachineRollMappingDto> list = machineRollMappingService.listMachineRollMapping(dto);
        return list;
    }

    @Log(title = "ui.cd90.MachineRollMapping.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90MachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineRollMappingService.importData(list, updateSupport, importLogId);
    }
}
