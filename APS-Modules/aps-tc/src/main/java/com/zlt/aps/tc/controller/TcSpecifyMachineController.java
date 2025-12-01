package com.zlt.aps.tc.controller;


import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.domain.dto.TcSpecifyMachineDto;
import com.zlt.aps.tc.entity.TcSpecifyMachine;
import com.zlt.aps.tc.service.TcSpecifyMachineService;
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
@RequestMapping("/tc/specifyMachine")
public class TcSpecifyMachineController extends BaseController {

    @Resource
    private TcSpecifyMachineService tcSpecifyMachineService;

    @ApiOperation("根据条件查询定点机台列表")
    @PostMapping("/listSpecifyMachine")
    public TableDataInfo listSpecifyMachine(@RequestBody TcSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcSpecifyMachineDto> list = tcSpecifyMachineService.listSpecifyMachine(dto);
        return getDataTable(list);
    }

    @ApiOperation("根据id查询定点机台信息")
    @GetMapping("/getSpecifyMachine/{id}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public TcSpecifyMachineDto getSpecifyMachine(@PathVariable("id") Long id) {
        TcSpecifyMachineDto dto = new TcSpecifyMachineDto();
        BeanUtils.copyProperties(tcSpecifyMachineService.getById(id), dto);
        return dto;
    }

    @Log(title = "ui.tc.specifyMachine.column.modalName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存定点机台信息（id为空则新增，id不为空则修改）")
    @PostMapping("/saveSpecifyMachine")
    public AjaxResult saveSpecifyMachine(@RequestBody TcSpecifyMachineDto dto) {
        TcSpecifyMachine entity = new TcSpecifyMachine();
        BeanUtils.copyProperties(dto, entity);
        tcSpecifyMachineService.saveSpecifyMachine(entity);
        return AjaxResult.success();
    }

    @Log(title = "ui.tc.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("批量删除定点机台信息(逻辑删)")
    @PostMapping("/deleteSpecifyMachine/{ids}")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id數組", paramType = "query")
    })
    public AjaxResult deleteSpecifyMachine(@PathVariable("ids") Long[] ids) {
        tcSpecifyMachineService.deleteSpecifyMachine(ids);
        return AjaxResult.success();
    }

    @Log(title = "ui.tc.specifyMachine.column.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部定点机台信息(逻辑删)")
    @PostMapping("/deleteAllSpecifyMachine")
    public AjaxResult deleteAllSpecifyMachine() {
        tcSpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Log(title = "ui.tc.specifyMachine.column.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData")
    public List<TcSpecifyMachineDto> exportData(@RequestBody TcSpecifyMachineDto dto) {
        startPage();
        dto.setOrderStr(orderStr());
        List<TcSpecifyMachineDto> list = tcSpecifyMachineService.listSpecifyMachine(dto);
        return list;
    }

    @Log(title = "ui.tc.specifyMachine.column.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<TcSpecifyMachineDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return tcSpecifyMachineService.importData(list, updateSupport, importLogId);
    }
}
