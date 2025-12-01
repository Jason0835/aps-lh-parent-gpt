package com.zlt.aps.cd15.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import com.zlt.aps.cd15.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.service.Cd15MachineRollMappingService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 钢带大卷与机台的映射表 前端控制器
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@RestController
@RequestMapping("/cd15/MachineRollMapping")
public class Cd15MachineRollMappingController extends BaseController {
    @Resource
    public Cd15MachineRollMappingService machineRollMappingService;

    @ApiOperation("根据条件查询钢带大卷与机台的映射表")
    @PostMapping("/listMachineRollMapping")
    public TableDataInfo listXwyyMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<Cd15MachineRollMappingDto> list = machineRollMappingService.listMachineRollMapping(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询钢带大卷与机台的映射表")
    @GetMapping("/getMachineRollMapping/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd15MachineRollMappingDto getMachineRollMapping(@PathVariable("id") Long id) {
        Cd15MachineRollMappingDto dto = new Cd15MachineRollMappingDto();
        BeanUtils.copyProperties(machineRollMappingService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.cd15.MachineRollMapping.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存钢带大卷与机台的映射表（id为空则新增，id不为空则修改）")
    @PostMapping("/saveMachineRollMapping")
    public AjaxResult saveXwyyMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto) {
        Cd15MachineRollMapping entity = new Cd15MachineRollMapping();
        BeanUtils.copyProperties(dto, entity);
        machineRollMappingService.saveMachineRollMapping(entity);
        return AjaxResult.success();
    }

    @ApiOperation("根据code判断主键是否已经存在")
    @PostMapping("/checkMachineRollMapping")
    public String checkMachineRollMapping(@RequestBody Cd15MachineRollMappingDto dto) {
        return machineRollMappingService.checkMachineRollMapping(dto);
    }

    @Log(title = "ui.cd15.MachineRollMapping.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除钢带大卷与机台的映射表(逻辑删)")
    @PostMapping("/deleteMachineRollMapping/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteMachineRollMapping(@PathVariable("ids") Long[] ids) {
        machineRollMappingService.deleteMachineRollMapping(ids);
        return AjaxResult.success();
    }


    @Log(title = "ui.cd15.MachineRollMapping.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        machineRollMappingService.deleteAll();
        return AjaxResult.success();
    }

    @Log(title = "ui.cd15.MachineRollMapping.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<Cd15MachineRollMappingDto> exportData(@RequestBody Cd15MachineRollMappingDto dto) {
        dto.setOrderStr(orderStr());
        List<Cd15MachineRollMappingDto> list = machineRollMappingService.listMachineRollMapping(dto);
        return list;
    }

    @Log(title = "ui.cd15.MachineRollMapping.column.modalName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入15度裁断钢压大卷和机台映射信息")
    public AjaxResult importData(@RequestBody List<Cd15MachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineRollMappingService.importData(list, updateSupport, importLogId);
    }
}
