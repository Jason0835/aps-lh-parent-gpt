package com.zlt.aps.cd90.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.PreAuthorize;
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.service.Cd90MachineInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 90°裁断机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "90°裁断机台信息维护接口")
@RestController
@RequestMapping("/cd90/machine")
public class Cd90MachineInfoController extends BaseController {
    @Autowired
    private Cd90MachineInfoService machineInfoService;

    /**
     * 查询90°裁断机台信息列表
     */
    @ApiOperation("根据条件查询90°裁断机台信息")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody Cd90MachineInfo machineInfo) {
        startPage();
        machineInfo.setOrderStr(orderStr());
        List<Cd90MachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return getDataTable(list);
    }

    /**
     * 获取90°裁断机台信息详细信息
     */
    @GetMapping(value = "/{id}")
    @ApiOperation("根据id查询90°裁断机台信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public Cd90MachineInfo getInfo(@PathVariable("id") Long id) {
        return machineInfoService.selectMachineInfoById(id);
    }

    /**
     * 新增90°裁断机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:add")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增90°裁断机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody Cd90MachineInfo machineInfo) {
        return toAjax(machineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改90°裁断机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:edit")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改90°裁断机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody Cd90MachineInfo machineInfo) {
        return toAjax(machineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除90°裁断机台信息
     */
    //@PreAuthorize(hasPermi = "tc:machine:remove")
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除90°裁断机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(machineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody Cd90MachineInfo machineInfo) {
        return machineInfoService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 查询列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @PostMapping("/exportList")
    public List<Cd90MachineInfo> exportList(@RequestBody Cd90MachineInfo machineInfo) {
        machineInfo.setOrderStr(orderStr());
        List<Cd90MachineInfo> list = machineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    @PostMapping("/list2")
    List<Cd90MachineInfo> list2(@RequestBody Cd90MachineInfo machineInfo) {
        List<Cd90MachineInfo> list = machineInfoService.selectMachineInfoList2(machineInfo);
        return list;
    }

    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<Cd90MachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return machineInfoService.importData(list, updateSupport, importLogId);
    }
}
