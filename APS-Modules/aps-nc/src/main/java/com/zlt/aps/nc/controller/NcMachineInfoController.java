package com.zlt.aps.nc.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;
import com.zlt.aps.nc.service.NcMachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "内衬机台信息维护接口")
@RestController
@RequestMapping("/nc/machine")
public class NcMachineInfoController extends BaseController {
    @Autowired
    private NcMachineInfoService machineInfoService;

    /**
     * 查询内衬机台信息列表
     */
    @ApiOperation("根据条件查询内衬机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody NcMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<NcMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取内衬机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询内衬机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public NcMachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增内衬机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:add")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增内衬机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody NcMachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改内衬机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:edit")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改内衬机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody NcMachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除内衬机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:remove")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除内衬机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody NcMachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<NcMachineInfo> exportList(@RequestBody NcMachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<NcMachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    /**
     * 根据内衬和口型板获取对应机台信息
     */
    @PostMapping("/list2")
    public List<NcMachineInfo> list2(@RequestBody NcMachineInfo machineInfo) {
        List<NcMachineInfo> list = machineInfoService.selectMachineInfoList2(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入内衬机台信息")
    public AjaxResult importData(@RequestBody List<NcMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (StringUtils.isNull(list) || list.size() == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
